package org.polykek.ftpproxyserver.response

enum class Code(val text: String) {
    OPENING_DATA_CONNECTION("150"), // After RETR, LIST, NLST, APPE, REST, STOR, STOU commands (contains size)
    COMMAND_SUCCESSFUL("200"), // For internal use (after PORT, TYPE, ... commands)
    SERVICE_READY("220"), // After open control connection
    CLOSING_CONTROL_CONNECTION("221"), // Send to client QUIT when not connected to server. After timeout or closing
    CLOSING_DATA_CONNECTION("226"), // For internal use (after transfer completion or termination)
    LOGIN_SUCCESSFUL("230"), // After right password
    PATH_STATUS("257"), // Current directory after PWD or created after MKD
    USERNAME_OK("331"), // Waiting password after USER
    CANNOT_OPEN_DATA_CONNECTION("425"), // After PORT or PASV
    DATA_TRANSFER_ABORTED("426"), // For internal use (after ABOR if transmission occurs)
    ACTION_ABORTED("451"), // For internal use (after RETR, ... commands)
    REQUEST_ACTION_DID_NOT_TAKE("500"), // For internal use (if PORT address is not equals client address)
    INVALID_ARGUMENTS("501"), // For internal use (if USER-command do not contains address, ...)
    COMMAND_NOT_IMPLEMENTED("502"), // For internal use (response on PASV-command)
    BAD_SEQUENCE_OF_COMMANDS("503"), // For internal use (PASS before is username ok)
    NOT_LOGGED_IN("530") // If user want to send command to server before USER-command or log in failed
}