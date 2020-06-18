package org.polykek.ftpproxyserver.config

data class Config(val proxyPort: Int,
                  val proxyAddress: String,
                  val cachePath: String,
                  val cacheSize: Long)