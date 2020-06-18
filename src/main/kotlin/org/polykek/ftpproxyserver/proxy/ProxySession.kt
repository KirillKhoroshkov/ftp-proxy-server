package org.polykek.ftpproxyserver.proxy

import org.apache.logging.log4j.kotlin.logger
import org.polykek.ftpproxyserver.cache.Cache
import org.polykek.ftpproxyserver.command.Command
import org.polykek.ftpproxyserver.command.CommandHandler
import org.polykek.ftpproxyserver.response.MessageLine
import org.polykek.ftpproxyserver.response.Response
import org.polykek.ftpproxyserver.response.ResponseHandler
import java.lang.Exception
import java.lang.IllegalArgumentException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.atomic.AtomicBoolean

class ProxySession(clientControlSocket: Socket,
                   private val cache: Cache,
                   private val address: InetAddress) : CommandHandler, ResponseHandler {

    companion object {
        private val logger = logger()
    }

    private val userState = UserState()
    private val pathState = PathState()

    private var isClosed = AtomicBoolean(false)

    private val clientControlConnection = ClientControlConnection(clientControlSocket, this)
    private var serverControlConnection: ServerControlConnection? = null

    private val dataTransferManager = DataTransferManager()

    init {
        if (address.address.size != 4) {
            throw IllegalArgumentException("Illegal address value (address: $address)")
        }
        clientControlConnection.sendResponse(Response.Handled.ServiceReady(listOf(MessageLine("Proxy service ready"))))
        clientControlConnection.start()
    }

    private fun stopServerControlConnection() {
        serverControlConnection?.stopProperly()
        serverControlConnection = null
    }

    fun clear() {
        userState.logOut()
        pathState.clear()
        stopServerControlConnection()
        dataTransferManager.clearConnectionInfo()
        dataTransferManager.abortAll()
    }

    fun close() {
        if (!isClosed.getAndSet(true)) {
            clear()
            clientControlConnection.stopProperly()
        }
    }

    fun sendInvalidCommandMessage(message: String?) {
        clientControlConnection.sendResponse(Response.Unhandled.Internal.InvalidArguments(listOf(MessageLine(message))))
    }

    override fun handle(user: Command.Handled.User) {
        logger.debug("Handle $user")
        val isUsernameChanged = userState.tryChangeUsername(user.username)
        if (isUsernameChanged) {
             try {
                 val serverControlSocket = Socket(user.host, user.port)
                 serverControlConnection = ServerControlConnection(serverControlSocket, this)
                 serverControlConnection!!.start()
             } catch (exception: Exception) {
                 clientControlConnection.sendResponse(Response.Unhandled.Internal.RequestActionDidNotTake(
                         listOf(MessageLine("Could not connect to server"))))
                 userState.logOut()
            }
        } else {
            clientControlConnection.sendResponse(Response.Handled.NotLoggedIn(
                    listOf(MessageLine("Can't change to another user"))))
        }
    }

    override fun handle(password: Command.Handled.Password) {
        logger.debug("Handle $password")
        if (userState.tryExpectLogIn()) {
            serverControlConnection!!.sendCommand(password)
        } else {
            clientControlConnection.sendResponse(Response.Unhandled.Internal.BadSequenceOfCommands(
                    listOf(MessageLine("Login with USER first"))))
        }
    }

    override fun handle(port: Command.Handled.Port) {
        logger.debug("Handle $port")
        val clientHostAddress = clientControlConnection.socketAddress.address.hostAddress
        if (!userState.isLoggedIn()) {
            clientControlConnection
                    .sendResponse(Response.Handled.NotLoggedIn(listOf(MessageLine("Please login with USER and PASS"))))
        } else if (clientHostAddress != port.socketAddress.address.hostAddress) {
            clientControlConnection.sendResponse(Response.Unhandled.Internal.RequestActionDidNotTake(listOf(MessageLine(
                    "I won't open a connection to " +
                    "${port.socketAddress.address} " +
                    "(only to ${clientHostAddress})"))))
        } else {
            dataTransferManager.changeClientSocketAddress(port.socketAddress)
            val newPort = dataTransferManager.tryOpenServerSocket()
            if (newPort > 0) {
                val newSocketAddress = InetSocketAddress(address, newPort)
                serverControlConnection!!.sendCommand(Command.Handled.Port(newSocketAddress))
            } else {
                clientControlConnection.sendResponse(Response.Unhandled.Internal.CommandSuccessful(
                                listOf(MessageLine("PORT command successful"))))
            }
        }
    }

    override fun handle(passive: Command.Handled.Passive) {
        logger.debug("Handle $passive")
        clientControlConnection.sendResponse(Response.Unhandled.Internal.CommandNotImplemented(
                listOf(MessageLine("Passive mode is unsupported"))))
    }

    override fun handle(retrieve: Command.Handled.Retrieve) {
        logger.debug("Handle $retrieve")
        if (!userState.isLoggedIn()) {
            clientControlConnection
                    .sendResponse(Response.Handled.NotLoggedIn(listOf(MessageLine("Please login with USER and PASS"))))
        } else {
            pathState.changeFilename(retrieve.filename)
            serverControlConnection!!.sendCommand(Command.Unhandled.Internal.CurrentPath)
        }
    }

    override fun handle(abort: Command.Handled.Abort) {
        logger.debug("Handle $abort")
        if (userState.isLoggedIn()) {
            val lastDataTransfer = dataTransferManager.takeLastActiveDataTransfer()
            if (lastDataTransfer == null) {
                clientControlConnection.sendResponse(Response.Unhandled.Internal.ClosingDataConnection(
                        listOf(MessageLine("No active data transfers"))))
            } else {
                if (lastDataTransfer !is DataTransfer.FromCache) {
                    serverControlConnection!!.sendCommand(abort)
                } else {
                    clientControlConnection.sendResponse(Response.Unhandled.Internal.DataTransferAborted(
                            listOf(MessageLine("Data transfer aborted"))))
                    lastDataTransfer.stopProperly()
                    lastDataTransfer.join()
                }
            }
        } else {
            clientControlConnection
                    .sendResponse(Response.Handled.NotLoggedIn(listOf(MessageLine("Please login with USER and PASS"))))
        }
    }

    override fun handle(reinitialize: Command.Handled.Reinitialize) {
        logger.debug("Handle $reinitialize")
        clientControlConnection.sendResponse(Response.Unhandled.Internal.CommandNotImplemented(
                        listOf(MessageLine("REIN command is not implemented"))))
    }

    override fun handle(quit: Command.Handled.Quit) {
        logger.debug("Handle $quit")
        if (serverControlConnection == null) {
            clientControlConnection
                    .sendResponse(Response.Handled.ClosingControlConnection(listOf(MessageLine("Goodbye"))))
            close()
        } else {
            serverControlConnection!!.sendCommand(quit)
        }
    }

    override fun handle(unhandled: Command.Unhandled) {
        logger.debug("Handle $unhandled")
        if (serverControlConnection == null) {
            clientControlConnection.sendResponse(Response.Unhandled.Internal.ActionAborted(
                    listOf(MessageLine("Specify server by USER command"))))
        } else {
            serverControlConnection!!.sendCommand(unhandled)
        }
    }

    override fun handle(serviceReady: Response.Handled.ServiceReady) {
        logger.debug("Handle $serviceReady")
        val user = Command.Unhandled.Internal.User(userState.takeUsername())
        serverControlConnection!!.sendCommand(user)
    }

    override fun handle(pathStatus: Response.Handled.PathStatus) {
        logger.debug("Handle $pathStatus")
        if (pathState.tryChangeDirectoryPath(pathStatus.takePath())) {
            val path = pathState.tryTakeFullPath()
            logger.debug("Path to file: $path")
            if (path != null) {
                val serverSocketAddress = serverControlConnection!!.socketAddress
                val alreadyCachedFile = try {
                    cache.readCachedFile(serverSocketAddress.hostName, serverSocketAddress.port, path)
                } catch (exception: Exception) {
                    logger.debug("Cannot open cached file", exception)
                    null
                }
                if (alreadyCachedFile != null) {
                    pathState.clear()
                    clientControlConnection
                            .sendResponse(Response.Handled.OpeningDataConnection(listOf(MessageLine("Opening data connection"))))
                    val dataTransfer = dataTransferManager.openTransferFromCache(alreadyCachedFile)
                    if (dataTransfer != null) {
                        dataTransfer.onClose = {
                            clientControlConnection.sendResponse(Response.Unhandled.Internal.ClosingDataConnection(
                                    listOf(MessageLine("Data connection close"))))
                        }
                        dataTransfer.start()
                    } else {
                        alreadyCachedFile.close()
                        clientControlConnection.sendResponse(Response.Handled.CannotOpenDataConnection(
                                listOf(MessageLine("Cannot open data connection"))))
                    }
                } else {
                    serverControlConnection!!.sendCommand(Command.Handled.Retrieve(pathState.tryTakeFilename()!!))
                }
            } else {
                serverControlConnection!!.sendCommand(Command.Handled.Retrieve(pathState.tryTakeFilename()!!))
            }
        } else {
            clientControlConnection.sendResponse(pathStatus)
        }
    }

    override fun handle(openingDataConnection: Response.Handled.OpeningDataConnection) {
        logger.debug("Handle $openingDataConnection")
        val size = try {
            openingDataConnection.takeSize()
        } catch (exception: Exception) {
            -1L
        }
        logger.debug("File size: $size")
        clientControlConnection.sendResponse(openingDataConnection)
        val path = pathState.tryRemoveFullPath()
        logger.debug("Path to file: $path")
        val dataTransfer = if (size > 0 && path != null) {
            val serverSocketAddress = serverControlConnection!!.socketAddress
            val newCachedFile = try {
                cache.writeCachedFile(serverSocketAddress.hostName, serverSocketAddress.port, path, size)
            } catch (exception: Exception) {
                logger.debug("Cannot write cache file", exception)
                null
            }
            if (newCachedFile != null) {
                val transfer = dataTransferManager.openCachingTransferFromServer(newCachedFile)
                transfer ?: newCachedFile.close()
                transfer
            } else {
                dataTransferManager.openTransferFromServer()
            }
        } else {
            dataTransferManager.openTransferFromServer()
        }
        dataTransfer?.start()
    }

    override fun handle(loginSuccessful: Response.Handled.LoginSuccessful) {
        logger.debug("Handle $loginSuccessful")
        userState.logIn()
        clientControlConnection.sendResponse(loginSuccessful)
    }

    override fun handle(usernameOk: Response.Handled.UsernameOk) {
        logger.debug("Handle $usernameOk")
        userState.okUsername()
        clientControlConnection.sendResponse(usernameOk)
    }

    override fun handle(cannotOpenDataConnection: Response.Handled.CannotOpenDataConnection) {
        logger.debug("Handle $cannotOpenDataConnection")
        dataTransferManager.clearConnectionInfo()
        clientControlConnection.sendResponse(cannotOpenDataConnection)
    }

    override fun handle(notLoggedIn: Response.Handled.NotLoggedIn) {
        logger.debug("Handle $notLoggedIn")
        clear()
        clientControlConnection.sendResponse(notLoggedIn)
    }

    override fun handle(closingControlConnection: Response.Handled.ClosingControlConnection) {
        logger.debug("Handle $closingControlConnection")
        clientControlConnection.sendResponse(closingControlConnection)
        close()
    }

    override fun handle(unhandled: Response.Unhandled) {
        logger.debug("Handle $unhandled")
        clientControlConnection.sendResponse(unhandled)
    }
}