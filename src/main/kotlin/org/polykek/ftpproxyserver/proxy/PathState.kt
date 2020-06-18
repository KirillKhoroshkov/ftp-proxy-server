package org.polykek.ftpproxyserver.proxy

import java.nio.file.Path

class PathState {

    private var filename: String? = null
    private var directoryPath: Path? = null

    fun tryTakeFilename(): String? = synchronized(this) {
        return@synchronized filename
    }

    fun tryTakeFullPath(): Path? = synchronized(this) {
        if (filename == null) {
            return@synchronized null
        }
        if (directoryPath == null) {
            return@synchronized null
        }
        val fullPath = directoryPath!!.resolve(filename!!)
        return@synchronized fullPath
    }

    fun tryRemoveFullPath(): Path? = synchronized(this) {
        if (filename == null) {
            return@synchronized null
        }
        if (directoryPath == null) {
            return@synchronized null
        }
        val fullPath = directoryPath!!.resolve(filename!!)
        filename = null
        directoryPath = null
        return@synchronized fullPath
    }

    fun changeFilename(newFilename: String) = synchronized(this) {
        filename = newFilename
        directoryPath = null
    }

    fun tryChangeDirectoryPath(newDirectoryPath: Path): Boolean = synchronized(this) {
        return@synchronized if (filename != null && directoryPath == null) {
            directoryPath = newDirectoryPath
            true
        } else {
            false
        }
    }

    fun clear() = synchronized(this) {
        filename = null
        directoryPath = null
    }
}