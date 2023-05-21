package io.vlinx.vmengine

import org.apache.bcel.classfile.JavaClass
import org.apache.bcel.classfile.Method
import java.util.*

class Frame(
    val thread: VMThread,
    val clazz: Class<*>,
    val method: Method,
    args: List<Any?>,
    val level: Int
) {

    val localVars = Array<Any?>(method.code.maxLocals) {}
    val operandStack = Stack<Any?>()

    init {
        for (i in 0 until args.size) {
            localVars[i] = args[i]
        }
        var tab = ""
        repeat(level) {
            tab += "\t"
        }
        Logging.verbose("${tab}LocalVars: ${localVars.contentToString()}")
    }


    override fun toString(): String {
        return this.method.toString()
    }
}