package org.polykek.ftpproxyserver.proxy

import org.apache.logging.log4j.kotlin.logger
import org.polykek.ftpproxyserver.cache.Cache
import org.polykek.ftpproxyserver.cuncurrent.AbstractProperlyStoppableThread
import java.io.IOException
import java.lang.Exception
import java.lang.IllegalArgumentException
import java.net.InetSocketAddress
import java.net.ServerSocket

class ProxyDispatcher(private val proxySocketAddress: InetSocketAddress,
                      private val cache: Cache) : AbstractProperlyStoppableThread() {

    companion object {
        private val logger = logger()
    }

    private val serverSocket: ServerSocket
    private val proxySessions = mutableSetOf<ProxySession>()

    init {
        if (proxySocketAddress.address.address.size != 4) {
            logger.fatal("Illegal address value (address: $proxySocketAddress)")
            throw IllegalArgumentException("Illegal address value (address: $proxySocketAddress)")
        }
        serverSocket = try {
            ServerSocket(proxySocketAddress.port)
        } catch (exception: IOException) {
            logger.fatal("Opening server socket failed", exception)
            throw exception
        }
        logger.info("Server socket successfully created on port ${proxySocketAddress.port}")
    }

    override fun stopProperly() {
        logger.debug("Properly stopping")
        if (!stoppedProperly()) {
            serverSocket.close()
        }
    }

    override fun run() {
        logger.info("Running")
        try {
            while (true) {
                val clientSocket = serverSocket.accept()
                logger.info("Client socket accepted")
                val proxySession = ProxySession(clientSocket, cache, proxySocketAddress.address)
                logger.debug("ProxySession created")
                proxySessions.add(proxySession)
            }
        } catch (exception: Exception) {
            logger.debug("Reason to stop:", exception)
        } finally {
            logger.info("Stopping")
            stoppedProperly()
            for (session in proxySessions) {
                session.close()
            }
            logger.info("Stopped")
        }
    }
}