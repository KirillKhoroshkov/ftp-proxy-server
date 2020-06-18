package org.polykek.ftpproxyserver.cuncurrent

import java.util.concurrent.atomic.AtomicBoolean

abstract class AbstractProperlyStoppableThread : Thread() {

    private var isStoppedProperly = AtomicBoolean(false)

    abstract fun stopProperly()

    protected fun stoppedProperly(): Boolean {
        return isStoppedProperly.getAndSet(true)
    }
}