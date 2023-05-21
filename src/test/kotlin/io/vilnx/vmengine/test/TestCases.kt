package io.vilnx.vmengine.test

import io.vlinx.vmengine.*
import io.vlinx.vmengine.imagine.ImagineBreaker
import io.vlinx.vmengine.util.ClassUtils
import org.apache.bcel.classfile.Method
import org.burningwave.core.assembler.StaticComponentContainer
import org.junit.jupiter.api.Test
import java.io.File

class TestCases {

    fun init() {
        StaticComponentContainer.Configuration.Default.setFileName("burningwave.properties")
        StaticComponentContainer.Modules.exportAllToAll()
        ImagineBreaker.openAllBaseModulesReflectively()
    }

    @Test
    fun executeStaticMethod() {

        init()

        val classPath = "/Users/vlinx/Test/zelix"
        val className = "a"
        val methodName = "main"
        val methodSignature = "([Ljava/lang/String;)V"
        val args = listOf<Any?>()

        val url = File(classPath).toURI().toURL()
        val urls = arrayOf(url)
        val loader = VlxClassLoader(urls)

        val clazz = loader.loadClass(className)
        val method = ClassUtils.getParsedMethod(clazz, methodName, methodSignature, loader)

        val thread = VMThread(VMEngine.instance, loader)
        thread.execute(clazz, method as Method, args, 0)
    }

    @Test
    fun executeVirtualMethod() {
        init()

        val classPath = "/Users/vlinx/Projects/java-app-samples/basic"
        val className = "Hello"
        val methodName = "hello"
        val methodSignature = "()V"
        val args = listOf<Any?>()

        val url = File(classPath).toURI().toURL()
        val urls = arrayOf(url)
        val loader = VlxClassLoader(urls)

        val clazz = loader.loadClass(className)

        val method = ClassUtils.getMethod(clazz, methodName, methodSignature, loader)
        val instance = clazz.getDeclaredConstructor().newInstance()


        val thread = VMThread(VMEngine.instance, loader)
        thread.execute(instance, method!!, args, true, 0)
    }


}