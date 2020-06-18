package org.polykek.ftpproxyserver.config

import java.security.InvalidParameterException
import java.util.*

private const val CACHE_SIZE_PROPERTY = "cache.size"
private const val CACHE_PATH_PROPERTY = "cache.path"
private const val PROXY_PORT_PROPERTY = "proxy.port"
private const val PROXY_ADDRESS_PROPERTY = "proxy.address"
private const val LOG_CONSOLE_PROPERTY = "log.console"

class ConfigLoader {

    fun load(configFile: String): Config {
        var proxyPort: Int? = null
        var cachePath: String? = null
        var cacheSize: Long? = null
        var proxyAddress: String? = null
        ClassLoader.getSystemResourceAsStream(configFile).use {
            val properties = Properties()
            properties.load(it)
            proxyPort = (properties[PROXY_PORT_PROPERTY] as String?)?.toIntOrNull()
            cachePath = properties[CACHE_PATH_PROPERTY] as String?
            cacheSize = (properties[CACHE_SIZE_PROPERTY] as String?)?.toLongOrNull()
            proxyAddress = properties[PROXY_ADDRESS_PROPERTY] as String?
        }
        if (proxyPort == null || proxyPort!! <= 0) {
            throw InvalidParameterException("Invalid port ($PROXY_PORT_PROPERTY = $proxyPort)")
        }
        if (cacheSize == null || cacheSize!! < 0) {
            throw InvalidParameterException("Invalid cache size ($CACHE_SIZE_PROPERTY = $cacheSize)")
        }
        if (cachePath == null) {
            throw InvalidParameterException("Cache path is not present ($CACHE_PATH_PROPERTY = $cachePath)")
        }
        if (proxyAddress == null) {
            throw InvalidParameterException("Proxy address is not present ($PROXY_ADDRESS_PROPERTY = $proxyAddress)")
        }
        return Config(proxyPort!!, proxyAddress!!, cachePath!!, cacheSize!!)
    }
}