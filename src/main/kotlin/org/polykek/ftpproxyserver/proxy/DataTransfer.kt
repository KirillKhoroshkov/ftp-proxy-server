package org.polykek.ftpproxyserver.proxy

import org.apache.logging.log4j.kotlin.logger
import org.polykek.ftpproxyserver.cuncurrent.AbstractProperlyStoppableThread
import java.io.InputStream
import java.io.OutputStream
import java.net.Socket

sealed class DataTransfer : AbstractProperlyStoppableThread() {

    companion object {
        const val BUFFER_SIZE = 512
    }

    var onClose: (() -> Unit)? = null
    protected abstract fun closeAllStreams()

    class FromCache(private val socketForClient: Socket,
                    private val cacheStream: InputStream) : DataTransfer() {

        companion object {
            private val logger = logger()
        }

        private val clientOutputStream = socketForClient.getOutputStream()

        override fun closeAllStreams() {
            clientOutputStream.close()
            socketForClient.close()
            cacheStream.close()
        }

        override fun stopProperly() {
            logger.debug("Properly stopping")
            if (!stoppedProperly()) {
                closeAllStreams()
            }
        }

        override fun run() {
            logger.debug("Running")
            try {
                val buffer = ByteArray(BUFFER_SIZE)
                var size = cacheStream.read(buffer, 0, BUFFER_SIZE)
                while (size != -1) {
                    clientOutputStream.write(buffer, 0, size)
                    clientOutputStream.flush()
                    size = cacheStream.read(buffer, 0, BUFFER_SIZE)
                }
            } catch (exception: Exception) {
                logger.debug("Reason to stop:", exception)
            } finally {
                logger.info("Stopping")
                if (!stoppedProperly()) {
                    closeAllStreams()
                }
                onClose?.let { it() }
                logger.info("Stopped")
            }
        }
    }

    open class WithServer(private val socketForServer: Socket,
                          private val socketForClient: Socket) : DataTransfer() {

        companion object {
            private val logger = logger()
        }

        private val clientInputStream = socketForClient.getInputStream()!!
        protected val clientOutputStream = socketForClient.getOutputStream()!!
        protected val serverInputStream = socketForServer.getInputStream()!!
        private val serverOutputStream = socketForServer.getOutputStream()!!

        override fun closeAllStreams() {
            clientInputStream.close()
            clientOutputStream.close()
            socketForClient.close()
            serverInputStream.close()
            serverOutputStream.close()
            socketForServer.close()
        }

        override fun stopProperly() {
            logger.debug("Properly stopping")
            if (!stoppedProperly()) {
                closeAllStreams()
            }
        }

        override fun run() {
            logger.debug("Running")
            try {
                while (clientInputStream.available() <= 0 && serverInputStream.available() <= 0) {
                    sleep(1)
                }
                val inputStream: InputStream
                val outputStream: OutputStream
                if (serverInputStream.available() > 0) {
                    inputStream = serverInputStream
                    outputStream = clientOutputStream
                } else {
                    inputStream = clientInputStream
                    outputStream = serverOutputStream
                }
                val buffer = ByteArray(BUFFER_SIZE)
                var size = inputStream.read(buffer, 0, BUFFER_SIZE)
                while (size != -1) {
                    outputStream.write(buffer, 0, size)
                    outputStream.flush()
                    size = inputStream.read(buffer, 0, BUFFER_SIZE)
                }
            } catch (exception: Exception) {
                logger.debug("Reason to stop:", exception)
            } finally {
                logger.info("Stopping")
                if (!stoppedProperly()) {
                    closeAllStreams()
                }
                onClose?.let { it() }
                logger.info("Stopped")
            }
        }

        class Caching(socketForServer: Socket,
                      socketForClient: Socket,
                      private val cacheStream: OutputStream) : WithServer(socketForServer, socketForClient) {

            companion object {
                private val logger = logger()
            }

            override fun closeAllStreams() {
                super.closeAllStreams()
                cacheStream.close()
            }

            override fun stopProperly() {
                logger.debug("Properly stopping")
                if (!stoppedProperly()) {
                    closeAllStreams()
                }
            }

            override fun run() {
                logger.debug("Running")
                try {
                    val buffer = ByteArray(BUFFER_SIZE)
                    var size = serverInputStream.read(buffer, 0, BUFFER_SIZE)
                    while (size != -1) {
                        clientOutputStream.write(buffer, 0, size)
                        clientOutputStream.flush()
                        cacheStream.write(buffer, 0, size)
                        cacheStream.flush()
                        size = serverInputStream.read(buffer, 0, BUFFER_SIZE)
                    }
                } catch (exception: Exception) {
                    logger.debug("Reason to stop:", exception)
                } finally {
                    logger.info("Stopping")
                    if (!stoppedProperly()) {
                        closeAllStreams()
                    }
                    onClose?.let { it() }
                    logger.info("Stopped")
                }
            }
        }
    }
}