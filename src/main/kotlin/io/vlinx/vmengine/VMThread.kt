package io.vlinx.vmengine

import io.vlinx.logging.Logger
import io.vlinx.vmengine.Logging.verbose
import io.vlinx.vmengine.util.ClassUtils
import org.apache.bcel.classfile.JavaClass
import org.apache.bcel.generic.Type
import java.lang.reflect.Constructor
import java.lang.reflect.Executable
import java.lang.reflect.Method
import java.util.*

class VMThread(val engine: VMEngine, val loader: VlxClassLoader) {
    val frames = Stack<Frame>()

    class RetValue(val value: Any?) {
        override fun toString(): String {
            return "Return(value=$value)"
        }
    }


    fun execute(receiver: Any?, method: Executable, args: List<Any?>, handle: Boolean, level: Int): RetValue? {
        try {

            var tab = ""

            repeat(level) {
                tab += "\t"
            }

            var method = method
            var handle = handle

            // 这里解决一个向下找的问题
            if (receiver != null && method is Method) {
                method = ClassUtils.getMethod(receiver.javaClass, method.name, Type.getSignature(method), loader)
                    ?: throw Exception("Can't get the method to execute for method: $method, receiver: $receiver ")
            }

            verbose("${tab}Execute method: $method")
            if (receiver != null) {
                verbose("${tab}Receiver: $receiver")
            }
            verbose("${tab}Args: $args")

            method.isAccessible = true

            val parsedClazz = ClassUtils.getParsedClass(method.declaringClass, loader)
            val parsedMethod = ClassUtils.getParsedMethod(parsedClazz, method, loader)

            if (parsedClazz == null) {
                handle = false
                if (!method.declaringClass.name.startsWith("Lambda\$\$Lambda")) {
                    Logger.ERROR("Can't parse class ${method.declaringClass}")
                }
            } else if (parsedMethod == null) {
                handle = false
                Logger.ERROR("Can't parse method $method")
            }

            if (parsedMethod?.isNative == true) {
                handle = false
            }

            if (method is Constructor<*>) {
                // 此处的constructor就是要通过托管方式执行的
                return execute(method.declaringClass, parsedMethod!!, args, level)
            }

            if (method is Method) {

                if (handle) {
                    return execute(method.declaringClass, parsedMethod!!, args, level)
                }

                var value: Any? = if (parsedMethod?.isStatic == true) {
                    method.invoke(null, *args.toTypedArray())
                } else {
                    val ref = args[0]
                    method.invoke(ref, *args.subList(1, args.size).toTypedArray())
                }

                if (method.returnType.name == "void") {
                    return null
                }

                return RetValue(value)
            }

            return null

        } catch (t: Throwable) {
            if (t is VlxVmException) {
                throw t
            }

            throw VlxVmException(t)
        }
    }


    // 托管运行代码
    fun execute(
        clazz: Class<*>,
        parsedMethod: org.apache.bcel.classfile.Method,
        args: List<Any?>,
        level: Int
    ): RetValue? {

        try {

            var tab = ""
            repeat(level) {
                tab += "\t"
            }

            val frame = Frame(this, clazz, parsedMethod, args, level)
            this.frames.push(frame)
            val executor = VMExecutor(frame, this)
            executor.execute()
            this.frames.pop()
            if (executor.hasRetValue) {
                return RetValue(executor.retValue)
            }
            return null

        } catch (t: Throwable) {
            if (t is VlxVmException) {
                throw t
            }

            throw VlxVmException(t)
        }
    }


}