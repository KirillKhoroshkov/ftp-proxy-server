package org.polykek.ftpproxyserver.command

import org.polykek.ftpproxyserver.command.Token.*

class CommandParser(private val handler: CommandHandler) {

    fun parse(raw: String) {
        val withoutSpacesAtStart = raw.dropWhile { it == ' ' }
        val commandAndArguments = withoutSpacesAtStart.split(Regex(" +"), 2)
        val command = commandAndArguments[0].toUpperCase()
        if (command.isEmpty()) {
            throw IllegalArgumentException("Empty command")
        }
        val params = commandAndArguments.getOrNull(1)
        when (command) {
            /* USER-command example: USER username ftp.example.com:21 */
            USER.text -> {
                params ?: throw IllegalArgumentException("Command ${USER.text} must have arguments")
                val usernameAndAddress = params.split(Regex(" +", RegexOption.IGNORE_CASE), 2)
                val address = usernameAndAddress.getOrNull(1)
                address ?: throw IllegalArgumentException("No address specified in command ${USER.text}")
                val username = usernameAndAddress[0]
                val hostAndPort = address.split(Regex("( *: *)|( +)"), 2)
                val host = hostAndPort.getOrNull(0)
                host ?: throw IllegalArgumentException("Host must be specified in command ${USER.text}")
                val port = hostAndPort.getOrNull(1)?.toIntOrNull()
                port ?: throw IllegalArgumentException("Port must be specified in command ${USER.text}")
                handler.handle(Command.Handled.User(username, host, port))
            }
            PASSWORD.text -> {
                handler.handle(Command.Handled.Password(params))
            }
            PORT.text -> {
                params ?: throw IllegalArgumentException("Port params must be specified")
                try {
                    handler.handle(Command.Handled.Port(params))
                } catch (exception: IllegalArgumentException) {
                    throw IllegalArgumentException("Could not recognize address in command ${PORT.text}")
                }
            }
            PASSIVE.text -> {
                handler.handle(Command.Handled.Passive)
            }
            RETRIEVE.text -> {
                params ?: throw IllegalArgumentException("Filename must be specified in command ${RETRIEVE.text}")
                handler.handle(Command.Handled.Retrieve(params))
            }
            ABORT.text -> {
                handler.handle(Command.Handled.Abort)
            }
            REINITIALIZE.text -> {
                handler.handle(Command.Handled.Reinitialize)
            }
            QUIT.text -> {
                handler.handle(Command.Handled.Quit)
            }
            else -> {
                handler.handle(Command.Unhandled.Raw(command, params))
            }
        }
    }
}