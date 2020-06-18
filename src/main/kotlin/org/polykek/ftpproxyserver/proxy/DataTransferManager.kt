package org.polykek.ftpproxyserver.proxy

import org.apache.logging.log4j.kotlin.logger
import java.io.InputStream
import java.io.OutputStream
import java.lang.Exception
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket

class DataTransferManager {

    companion object {
        const val TIMEOUT = 1000
        private val logger = logger()
    }

    private val dataTransfers = mutableListOf<DataTransfer>()
    private var clientSocketAddress: InetSocketAddress? = null
    private var serverSocket: ServerSocket? = null

    fun abortAll() = synchronized(this) {
        logger.debug("Aborting transfers")
        dataTransfers.forEach { it.stopProperly() }
        dataTransfers.forEach { it.join() }
        dataTransfers.clear()
    }

    fun changeClientSocketAddress(newClientSocketAddress: InetSocketAddress) = synchronized(this) {
        clientSocketAddress = newClientSocketAddress
        logger.debug("Client socket address changed to ${newClientSocketAddress.hostName}:${clientSocketAddress?.port}")
    }

    fun tryOpenServerSocket(): Int = synchronized(this) {
        serverSocket = try {
            ServerSocket(0)
        } catch (exception: Exception) {
            logger.error("Could not open server socket", exception)
            null
        }
        return@synchronized if (serverSocket == null) {
            -1
        } else {
            logger.debug("Server socket open on ${serverSocket?.localPort} port")
            serverSocket!!.localPort
        }
    }

    fun clearConnectionInfo() = synchronized(this) {
        serverSocket?.close()
        serverSocket = null
        clientSocketAddress = null
        logger.debug("Connection info cleared")
    }

    fun takeLastActiveDataTransfer(): DataTransfer? = synchronized(this) {
        val transfers = dataTransfers.reversed()
        val activeTransfers = transfers.takeWhile { it.isAlive }
        return@synchronized activeTransfers.firstOrNull()
    }

    fun openTransferFromCache(cacheInputStream: InputStream): DataTransfer? = synchronized(this) {
        logger.debug("Opening transfer from cache")
        if (clientSocketAddress == null) {
            logger.error("Client socket address not specified")
            return@synchronized null
        }
        val socketForClient = try {
            Socket(clientSocketAddress!!.hostName, clientSocketAddress!!.port)
        } catch (exception: Exception) {
            logger.error("Opening socket for client failed", exception)
            clearConnectionInfo()
            return@synchronized null
        }
        return@synchronized try {
            val transfer = DataTransfer.FromCache(socketForClient, cacheInputStream)
            dataTransfers.add(transfer)
            transfer
        } catch (exception: Exception) {
            logger.error("Opening data transfer failed", exception)
            null
        } finally {
            clearConnectionInfo()
        }
    }

    fun openCachingTransferFromServer(cacheOutputStream: OutputStream): DataTransfer? = synchronized(this) {
        logger.debug("Opening caching transfer from server")
        if (clientSocketAddress == null) {
            logger.error("Client socket address not specified")
            return@synchronized null
        }
        val socketForClient = try {
            Socket(clientSocketAddress!!.hostName, clientSocketAddress!!.port)
        } catch (exception: Exception) {
            logger.error("Opening socket for client failed", exception)
            clearConnectionInfo()
            return@synchronized null
        }
        val socketForServer = acceptSocketWithTimeout()
        if (socketForServer == null) {
            logger.error("Opening socket for server failed")
            socketForClient.close()
            clearConnectionInfo()
            return@synchronized null
        }
        return@synchronized try {
            val transfer = DataTransfer.WithServer.Caching(socketForServer, socketForClient, cacheOutputStream)
            dataTransfers.add(transfer)
            transfer
        } catch (exception: Exception) {
            logger.error("Opening data transfer failed", exception)
            null
        } finally {
            clearConnectionInfo()
        }
    }

    fun openTransferFromServer(): DataTransfer? = synchronized(this) {
        logger.debug("Opening transfer from server")
        if (clientSocketAddress == null) {
            logger.error("Client socket address not specified")
            return@synchronized null
        }
        val socketForClient = try {
            Socket(clientSocketAddress!!.hostName, clientSocketAddress!!.port)
        } catch (exception: Exception) {
            logger.error("Opening socket for client failed", exception)
            clearConnectionInfo()
            return@synchronized null
        }
        val socketForServer = acceptSocketWithTimeout()
        if (socketForServer == null) {
            logger.error("Opening socket for server failed")
            socketForClient.close()
            clearConnectionInfo()
            return@synchronized null
        }
        return@synchronized try {
            val transfer = DataTransfer.WithServer(socketForServer, socketForClient)
            dataTransfers.add(transfer)
            transfer
        } catch (exception: Exception) {
            logger.error("Opening data transfer failed", exception)
            null
        } finally {
            clearConnectionInfo()
        }
    }

    private fun acceptSocketWithTimeout(): Socket? {
        return try {
            serverSocket?.soTimeout = TIMEOUT
            serverSocket?.accept()
        } catch (exception: Exception) {
            logger.error("Accept socket failed", exception)
            null
        } finally {
            serverSocket?.close()
            serverSocket = null
        }
    }
}