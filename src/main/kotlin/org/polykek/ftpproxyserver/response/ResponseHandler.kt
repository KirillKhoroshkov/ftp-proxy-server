package org.polykek.ftpproxyserver.response

interface ResponseHandler {

    fun handle(serviceReady: Response.Handled.ServiceReady)

    fun handle(pathStatus: Response.Handled.PathStatus)

    fun handle(openingDataConnection: Response.Handled.OpeningDataConnection)

    fun handle(loginSuccessful: Response.Handled.LoginSuccessful)

    fun handle(usernameOk: Response.Handled.UsernameOk)

    fun handle(cannotOpenDataConnection: Response.Handled.CannotOpenDataConnection)

    fun handle(notLoggedIn: Response.Handled.NotLoggedIn)

    fun handle(closingControlConnection: Response.Handled.ClosingControlConnection)

    fun handle(unhandled: Response.Unhandled)
}