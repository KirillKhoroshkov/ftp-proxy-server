package org.polykek.ftpproxyserver.util

import java.lang.IllegalArgumentException
import java.net.InetSocketAddress

fun convertSocketAddressToStringEnumeration(socketAddress: InetSocketAddress): String {
    val firstPortNumber = socketAddress.port / 256
    val secondPortNumber = socketAddress.port % 256
    var result = ""
    socketAddress.address.address.forEach {
        val number = if (it >= 0) it else 256 + it
        result += "$number,"
    }
    result += "$firstPortNumber,"
    result += secondPortNumber
    return result
}

fun convertStringEnumerationToSocketAddress(stringEnumeration: String): InetSocketAddress {
    val numbers = stringEnumeration.filter { it != ' ' }.split(",")
    if (numbers.size != 6) {
        throw IllegalArgumentException("String enumeration must contain 6 numbers")
    }
    val address = "${numbers[0]}.${numbers[1]}.${numbers[2]}.${numbers[3]}"
    val port = numbers[4].toInt() * 256 + numbers[5].toInt()
    return InetSocketAddress(address, port)
}