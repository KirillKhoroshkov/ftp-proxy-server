package org.polykek.ftpproxyserver.command

import org.polykek.ftpproxyserver.util.convertSocketAddressToStringEnumeration
import org.polykek.ftpproxyserver.util.convertStringEnumerationToSocketAddress
import java.net.InetSocketAddress

sealed class Command {

    protected abstract fun takeToken(): String

    protected abstract fun takeParams(): String?

    fun toCommandLine(): String {
        return takeToken() + if (takeParams() != null) " ${takeParams()}" else ""
    }

    sealed class Handled : Command() {
        abstract val token: Token
        override fun takeToken() = token.text

        data class User(val username: String, val host: String, val port: Int) : Handled() {
            override val token = Token.USER
            override fun takeParams() = "$username $host $port"
        }

        data class Password(val password: String?) : Handled() {
            override val token = Token.PASSWORD
            override fun takeParams() = password
        }

        data class Port(val socketAddress: InetSocketAddress) : Handled() {
            override val token = Token.PORT
            override fun takeParams() = convertSocketAddressToStringEnumeration(socketAddress)
            constructor(stringEnumeration: String): this(convertStringEnumerationToSocketAddress(stringEnumeration))
        }

        object Passive : Handled() {
            override val token = Token.PASSIVE
            override fun takeParams(): String? = null
        }

        data class Retrieve(val filename: String) : Handled() {
            override val token = Token.RETRIEVE
            override fun takeParams() = filename
        }

        object Abort : Handled() {
            override val token = Token.ABORT
            override fun takeParams(): String? = null
        }

        object Reinitialize : Handled() {
            override val token = Token.REINITIALIZE
            override fun takeParams(): String? = null
        }

        object Quit : Handled() {
            override val token = Token.QUIT
            override fun takeParams(): String? = null
        }
    }

    sealed class Unhandled : Command() {

        data class Raw(val tokenText: String, val params: String?) : Unhandled() {
            override fun takeToken() = tokenText
            override fun takeParams() = params
        }

        sealed class Internal : Unhandled() {
            abstract val token: Token
            override fun takeToken() = token.text

            object CurrentPath : Internal() {
                override val token = Token.CURRENT_PATH
                override fun takeParams(): String? = null
            }

            data class User(val username: String) : Internal() {
                override val token = Token.USER
                override fun takeParams() = username
            }
        }
    }
}