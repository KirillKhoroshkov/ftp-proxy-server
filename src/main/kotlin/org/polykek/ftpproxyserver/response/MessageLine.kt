package org.polykek.ftpproxyserver.response

data class MessageLine(val text: String?, val isWithCode: Boolean) {
    constructor(text: String?): this(text, true)
}