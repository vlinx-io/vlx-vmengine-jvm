package io.vlinx.vmengine

import io.vlinx.logging.Logger

object Logging {

    fun verbose(msg: Any?) {
        try {
            if (!Options.verbose) {
                return
            }
            Logger.DEBUG(msg)
        } catch (t: Throwable) {
            //ignore
        }
    }

    fun error(msg: Any?) {
        Logger.ERROR(msg)
    }

}