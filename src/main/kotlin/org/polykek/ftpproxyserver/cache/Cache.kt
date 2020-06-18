package org.polykek.ftpproxyserver.cache

import java.io.FileNotFoundException
import java.io.InputStream
import java.io.OutputStream
import java.lang.IllegalArgumentException
import java.nio.file.*
import javax.naming.SizeLimitExceededException

class Cache(path: String, private val cacheMaxSize: Long) {
    private val cacheDirectory: Path

    init {
        if (cacheMaxSize < 0) {
            throw IllegalArgumentException("Max size cannot be negative (size: $cacheMaxSize)")
        }
        cacheDirectory = Path.of(path)
        if (!Files.exists(cacheDirectory)) {
            throw FileNotFoundException("Path: $path")
        }
    }

    fun readCachedFile(host: String, port: Int, path: Path): InputStream = synchronized(this) {
        val filePath = makeFullPath(host, port, path)
        if (!Files.exists(filePath)) {
            throw FileNotFoundException(filePath.toString())
        }
        return Files.newInputStream(filePath, StandardOpenOption.READ)
    }

    fun writeCachedFile(host: String, port: Int, path: Path, size: Long): OutputStream = synchronized(this) {
        val filePath = makeFullPath(host, port, path)
        if (Files.exists(filePath)) {
            throw FileAlreadyExistsException(filePath.toString())
        }
        val actualCacheSize = Files.size(cacheDirectory)
        if ((actualCacheSize + size) > cacheMaxSize) {
            throw SizeLimitExceededException("Max: $cacheMaxSize, actual: $actualCacheSize, file size: $size")
        }
        createFullPath(filePath)
        return Files.newOutputStream(filePath)
    }

    private fun makeFullPath(host: String, port: Int, path: Path): Path {
        val directoryForServer = cacheDirectory.resolve("$host:$port")
        val fullPath = Paths.get(directoryForServer.toString(), path.toString())
        return fullPath
    }

    private fun createFullPath(path: Path) {
        val parentDirectory = path.parent
        if (Files.notExists(parentDirectory)) {
            Files.createDirectories(parentDirectory)
        }
        Files.createFile(path)
    }
}