package org.polykek.ftpproxyserver.proxy

import org.apache.logging.log4j.kotlin.logger
import org.polykek.ftpproxyserver.command.CommandParser
import org.polykek.ftpproxyserver.cuncurrent.AbstractProperlyStoppableThread
import org.polykek.ftpproxyserver.response.Response
import org.polykek.ftpproxyserver.util.printCRLF
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintStream
import java.net.InetSocketAddress
import java.net.Socket

class ClientControlConnection(private val clientControlSocket: Socket,
                              private val session: ProxySession) : AbstractProperlyStoppableThread() {

    private val clientControlInput = BufferedReader(InputStreamReader(clientControlSocket.inputStream))
    private val clientControlOutput = PrintStream(clientControlSocket.outputStream)

    private val commandParser = CommandParser(session)

    val socketAddress = InetSocketAddress(clientControlSocket.inetAddress.hostAddress, clientControlSocket.port)

    companion object {
        private val logger = logger()
    }

    override fun stopProperly() {
        logger.debug("Properly stopping")
        if (!stoppedProperly()) {
            clientControlOutput.close()
            clientControlInput.close()
            clientControlSocket.close()
        }
    }

    fun sendResponse(response: Response) = synchronized(this) {
        val responseLines = response.toResponseLines()
        responseLines.forEach {
            logger.info("Sending: $it")
            clientControlOutput.printCRLF(it)
        }
        clientControlOutput.flush()
    }

    override fun run() {
        logger.info("Running")
        try {
            while (true) {
                val rawCommand = clientControlInput.readLine()
                logger.info("Command from client: $rawCommand")
                try {
                    commandParser.parse(rawCommand)
                } catch (exception: IllegalArgumentException) {
                    logger.error("Invalid argument in command", exception)
                    session.sendInvalidCommandMessage(exception.message)
                    continue
                }
            }
        } catch (exception: Exception) {
            logger.debug("Reason to stop:", exception)
        } finally {
            logger.info("Stopping")
            if (!stoppedProperly()) {
                clientControlOutput.close()
                clientControlInput.close()
                clientControlSocket.close()
                session.close()
            }
            logger.info("Stopped")
        }
    }
}