package org.polykek.ftpproxyserver.command

enum class Token(val text: String) {
    USER("USER"),
    PASSWORD("PASS"),
    PORT("PORT"),
    PASSIVE("PASV"),
    RETRIEVE("RETR"),
    QUIT("QUIT"),
    REINITIALIZE("REIN"),
    ABORT("ABOR"), // Abort last transfer
    CURRENT_PATH("PWD") // For internal use (take path for cache file)
}