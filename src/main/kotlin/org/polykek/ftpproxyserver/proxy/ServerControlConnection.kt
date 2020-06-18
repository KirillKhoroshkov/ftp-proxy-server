package org.polykek.ftpproxyserver.proxy

import org.apache.logging.log4j.kotlin.logger
import org.polykek.ftpproxyserver.command.Command
import org.polykek.ftpproxyserver.cuncurrent.AbstractProperlyStoppableThread
import org.polykek.ftpproxyserver.response.ResponseParser
import org.polykek.ftpproxyserver.util.printCRLF
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintStream
import java.net.InetSocketAddress
import java.net.Socket

class ServerControlConnection(private val serverControlSocket: Socket,
                              private val session: ProxySession) : AbstractProperlyStoppableThread() {

    private val serverControlInput = BufferedReader(InputStreamReader(serverControlSocket.inputStream))
    private val serverControlOutput = PrintStream(serverControlSocket.outputStream)

    private val responseParser = ResponseParser(session)

    val socketAddress = InetSocketAddress(serverControlSocket.inetAddress.hostAddress, serverControlSocket.port)

    companion object {
        private val logger = logger()
    }

    override fun stopProperly() {
        logger.debug("Properly stopping")
        if (!stoppedProperly()) {
            serverControlOutput.close()
            serverControlInput.close()
            serverControlSocket.close()
        }
    }

    fun sendCommand(command: Command) = synchronized(this) {
        val commandLine = command.toCommandLine()
        logger.info("Sending: $commandLine")
        serverControlOutput.printCRLF(commandLine)
        serverControlOutput.flush()
    }

    override fun run() {
        logger.info("Running")
        try {
            while (true) {
                var rawResponseLine: String
                val rawResponseLines = mutableListOf<String>()
                do {
                    rawResponseLine = serverControlInput.readLine()
                    logger.info("Response from server: $rawResponseLine")
                    rawResponseLines.add(rawResponseLine)
                } while (!responseParser.isLastLine(rawResponseLine))
                responseParser.parse(rawResponseLines)
            }
        } catch (exception: IllegalArgumentException) {
            logger.error("Invalid response", exception)
            logger.debug("Reason to stop:", exception)
        }  catch (exception: Exception) {
            logger.debug("Reason to stop:", exception)
        } finally {
            logger.info("Stopping")
            if (!stoppedProperly()) {
                serverControlOutput.close()
                serverControlInput.close()
                serverControlSocket.close()
                session.clear()
            }
            logger.info("Stopped")
        }
    }
}