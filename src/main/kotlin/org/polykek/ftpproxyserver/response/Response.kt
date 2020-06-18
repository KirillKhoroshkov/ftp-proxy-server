package org.polykek.ftpproxyserver.response

import java.nio.file.Path

sealed class Response {
    abstract val messageLines: List<MessageLine>
    protected abstract fun takeCode(): String

    fun toResponseLines(): List<String> {
        if (messageLines.isEmpty()) {
            return listOf(takeCode())
        }
        val lastMessage = messageLines.last()
        val withoutLast = messageLines.dropLast(1)
        val resultList = mutableListOf<String>()
        for (message in withoutLast) {
            var resultLine = if (message.isWithCode) "${takeCode()}- " else " "
            if (message.text != null) {
                resultLine += message.text
            }
            resultList.add(resultLine)
        }
        if (lastMessage.text == null) {
            resultList.add(takeCode())
        } else {
            resultList.add("${takeCode()} ${lastMessage.text}")
        }
        return resultList
    }

    sealed class Handled : Response() {
        abstract val code: Code
        override fun takeCode() = code.text

        data class ServiceReady(override val messageLines: List<MessageLine>) : Handled() {
            override val code = Code.SERVICE_READY
        }

        data class PathStatus(override val messageLines: List<MessageLine>) : Handled() {
            override val code = Code.PATH_STATUS

            fun takePath(): Path {
                val firstLine = messageLines[0].text
                firstLine ?: throw IllegalArgumentException()
                val rawPath = firstLine.substring(firstLine.indexOf('\"') + 1, firstLine.lastIndexOf('\"'))
                val path = rawPath.replace("\"\"", "\"")
                if (path.isEmpty()) {
                    throw IllegalArgumentException()
                }
                return Path.of(path)
            }
        }

        data class OpeningDataConnection(override val messageLines: List<MessageLine>) : Handled() {
            override val code = Code.OPENING_DATA_CONNECTION

            fun takeSize(): Long {
                val firstLine = messageLines[0].text
                firstLine ?: throw IllegalArgumentException()
                val rawSize = firstLine
                        .substring(firstLine.lastIndexOf('(') + 1, firstLine.lastIndexOf(')'))
                        .takeWhile { it != ' ' }
                return rawSize.toLong()
            }
        }

        data class LoginSuccessful(override val messageLines: List<MessageLine>) : Handled() {
            override val code = Code.LOGIN_SUCCESSFUL
        }

        data class UsernameOk(override val messageLines: List<MessageLine>) : Handled() {
            override val code = Code.USERNAME_OK
        }

        data class CannotOpenDataConnection(override val messageLines: List<MessageLine>) : Handled() {
            override val code = Code.CANNOT_OPEN_DATA_CONNECTION
        }

        data class NotLoggedIn(override val messageLines: List<MessageLine>) : Handled() {
            override val code = Code.NOT_LOGGED_IN
        }

        data class ClosingControlConnection(override val messageLines: List<MessageLine>) : Handled() {
            override val code = Code.CLOSING_CONTROL_CONNECTION
        }
    }

    sealed class Unhandled : Response() {

        data class Raw(val codeText: String, override val messageLines: List<MessageLine>) : Unhandled() {
            override fun takeCode() = codeText
        }

        sealed class Internal : Unhandled() {
            abstract val code: Code
            override fun takeCode() = code.text

            data class InvalidArguments(override val messageLines: List<MessageLine>) : Internal() {
                override val code = Code.INVALID_ARGUMENTS
            }

            data class ClosingDataConnection(override val messageLines: List<MessageLine>) : Internal() {
                override val code = Code.CLOSING_DATA_CONNECTION
            }

            data class DataTransferAborted(override val messageLines: List<MessageLine>) : Internal() {
                override val code = Code.DATA_TRANSFER_ABORTED
            }

            data class CommandNotImplemented(override val messageLines: List<MessageLine>) : Internal() {
                override val code = Code.COMMAND_NOT_IMPLEMENTED
            }

            data class RequestActionDidNotTake(override val messageLines: List<MessageLine>) : Internal() {
                override val code = Code.REQUEST_ACTION_DID_NOT_TAKE
            }

            data class BadSequenceOfCommands(override val messageLines: List<MessageLine>) : Internal() {
                override val code = Code.BAD_SEQUENCE_OF_COMMANDS
            }

            data class CommandSuccessful(override val messageLines: List<MessageLine>) : Internal() {
                override val code = Code.COMMAND_SUCCESSFUL
            }

            data class ActionAborted(override val messageLines: List<MessageLine>) : Internal() {
                override val code = Code.ACTION_ABORTED
            }
        }
    }
}