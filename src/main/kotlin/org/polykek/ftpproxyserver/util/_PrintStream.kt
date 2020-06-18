package org.polykek.ftpproxyserver.util

import java.io.PrintStream

fun PrintStream.printCRLF(string: String) {
    this.print(string + "\r\n")
}