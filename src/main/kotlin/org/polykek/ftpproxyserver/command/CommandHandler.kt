package org.polykek.ftpproxyserver.command

interface CommandHandler {

    fun handle(user: Command.Handled.User)

    fun handle(password: Command.Handled.Password)

    fun handle(port: Command.Handled.Port)

    fun handle(passive: Command.Handled.Passive)

    fun handle(retrieve: Command.Handled.Retrieve)

    fun handle(abort: Command.Handled.Abort)

    fun handle(reinitialize: Command.Handled.Reinitialize)

    fun handle(quit: Command.Handled.Quit)

    fun handle(unhandled: Command.Unhandled)
}