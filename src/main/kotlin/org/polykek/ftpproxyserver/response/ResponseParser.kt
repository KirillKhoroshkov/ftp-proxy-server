package org.polykek.ftpproxyserver.response

import org.polykek.ftpproxyserver.response.Code.*

class ResponseParser(private val handler: ResponseHandler) {

    fun isLastLine(raw: String): Boolean {
        val parsedLine = parseLine(raw)
        return parsedLine.isLast
    }

    fun parse(rawLines: List<String>) {
        val parsedLines = rawLines.map { parseLine(it) }
        if (parsedLines.isEmpty()) {
            throw IllegalArgumentException("Empty list")
        }
        val lastParsedLine = parsedLines.last()
        val code = lastParsedLine.code
        if (code == null) {
            throw IllegalArgumentException("Last line must contains code")
        }
        if (!lastParsedLine.isLast) {
            throw IllegalArgumentException("Last line can not contain '-' after code")
        }
        val withoutLast = parsedLines.dropLast(1)
        withoutLast.forEach {
            if (it.code != null && it.code != code) {
                throw IllegalArgumentException("Different codes")
            }
            if (it.isLast) {
                throw IllegalArgumentException("More than one last line")
            }
        }
        val messageLines = mutableListOf<MessageLine>()
        withoutLast.forEach {
            val isWithCode = (it.code != null)
            messageLines.add(MessageLine(it.message, isWithCode))
        }
        messageLines.add(MessageLine(lastParsedLine.message, true))
        when (code) {
            SERVICE_READY.text -> {
                handler.handle(Response.Handled.ServiceReady(messageLines))
            }
            PATH_STATUS.text -> {
                handler.handle(Response.Handled.PathStatus(messageLines))
            }
            OPENING_DATA_CONNECTION.text -> {
                handler.handle(Response.Handled.OpeningDataConnection(messageLines))
            }
            LOGIN_SUCCESSFUL.text -> {
                handler.handle(Response.Handled.LoginSuccessful(messageLines))
            }
            USERNAME_OK.text -> {
                handler.handle(Response.Handled.UsernameOk(messageLines))
            }
            CANNOT_OPEN_DATA_CONNECTION.text -> {
                handler.handle(Response.Handled.CannotOpenDataConnection(messageLines))
            }
            NOT_LOGGED_IN.text -> {
                handler.handle(Response.Handled.NotLoggedIn(messageLines))
            }
            CLOSING_CONTROL_CONNECTION.text -> {
                handler.handle(Response.Handled.ClosingControlConnection(messageLines))
            }
            else -> {
                handler.handle(Response.Unhandled.Raw(code, messageLines))
            }
        }
    }

    private fun parseLine(raw: String): ParsedLine {
        val code = raw.takeWhile { it.isDigit() }
        val hyphenAndMessage = raw.substring(code.length)
        val isLast: Boolean
        val message: String
        if (code.isEmpty()) {
            isLast = false
            message = hyphenAndMessage.dropWhile { it == ' ' }
        } else if (hyphenAndMessage.firstOrNull() == '-') {
            isLast = false
            message = hyphenAndMessage.drop(1).dropWhile { it == ' ' }
        } else {
            isLast = true
            message = hyphenAndMessage.dropWhile { it == ' ' }
        }
        return ParsedLine(code.ifEmpty { null }, isLast, message.ifEmpty { null })
    }

    private data class ParsedLine(val code: String?, val isLast: Boolean, val message: String?)
}