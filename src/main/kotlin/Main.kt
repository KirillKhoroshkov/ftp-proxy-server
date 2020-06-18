import org.apache.logging.log4j.kotlin.logger
import org.polykek.ftpproxyserver.cache.Cache
import org.polykek.ftpproxyserver.config.ConfigLoader
import org.polykek.ftpproxyserver.proxy.ProxyDispatcher
import java.io.FileNotFoundException
import java.lang.Exception
import java.lang.IllegalArgumentException
import java.net.InetSocketAddress
import java.security.InvalidParameterException
import kotlin.system.exitProcess

private const val QUIT_COMMAND = "quit"
private const val PROPERTIES_FILE_NAME = "application.properties"

private val logger = logger("main")

fun main() {
    logger.info("Application started")
    val configLoader = ConfigLoader()
    val config = try {
        configLoader.load(PROPERTIES_FILE_NAME)
    } catch (exception: InvalidParameterException) {
        logger.fatal("Invalid params in $PROPERTIES_FILE_NAME", exception)
        exitProcess(-1)
    } catch (exception: Exception) {
        logger.fatal("Cannot read $PROPERTIES_FILE_NAME", exception)
        exitProcess(-1)
    }
    logger.debug("Config is loaded: $config")
    val cache = try {
        Cache(config.cachePath, config.cacheSize)
    } catch (exception: FileNotFoundException) {
        logger.fatal("Could not found cache directory", exception)
        exitProcess(-3)
    } catch (exception: IllegalArgumentException) {
        logger.fatal("Cache size cannot be negative", exception)
        exitProcess(-3)
    }
    logger.debug("Cache is configured")
    val proxySocketAddress = InetSocketAddress(config.proxyAddress, config.proxyPort)
    val proxyDispatcher = ProxyDispatcher(proxySocketAddress, cache)
    logger.debug("ProxyDispatcher created")
    proxyDispatcher.start()
    while (true) {
        val command = readLine()
        logger.info("Entered command: $command")
        if (command?.toLowerCase() == QUIT_COMMAND) {
            logger.info("Stopping")
            proxyDispatcher.stopProperly()
            logger.debug("ProxyDispatcher stopped")
            proxyDispatcher.join()
            logger.debug("ProxyDispatcher joined")
            break
        }
    }
    logger.info("Stopped")
}