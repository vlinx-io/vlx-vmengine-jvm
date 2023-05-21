package io.vlinx.vmengine

import java.lang.Exception

class VlxVmException : Exception {

    constructor(message: String?) : super(message)
    constructor(t: Throwable) : super(t)

}