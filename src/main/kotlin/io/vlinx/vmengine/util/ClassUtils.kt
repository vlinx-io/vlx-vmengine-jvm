package io.vlinx.vmengine.util

import io.vlinx.vmengine.PrimitiveTypes
import io.vlinx.vmengine.VlxVmException
import org.apache.bcel.Const
import org.apache.bcel.classfile.BootstrapMethod
import org.apache.bcel.classfile.BootstrapMethods
import org.apache.bcel.classfile.ClassParser
import org.apache.bcel.classfile.JavaClass
import org.apache.bcel.generic.Type
import org.burningwave.core.classes.Fields
import org.burningwave.core.classes.Methods
import sun.misc.Unsafe
import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType
import java.lang.reflect.Constructor
import java.lang.reflect.Executable
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.util.*


/**
 * @author vlinx <vlinx@vlinx.io>
 * @create 2023-03-03
 * @version 1.0.0
 */

object ClassUtils {

    val fields = Fields.create()
    val methods = Methods.create()

    fun getClass(className: String, loader: ClassLoader): Class<*> {


        var _className = className

        var dimensions = 0
        while (_className.endsWith("[]")) {
            dimensions++
            _className = _className.removeSuffix("[]")
        }

        val clazz = when (_className) {
            "void" -> PrimitiveTypes.VOID
            "byte" -> PrimitiveTypes.BYTE
            "boolean" -> PrimitiveTypes.BOOLEAN
            "char" -> PrimitiveTypes.CHAR
            "short" -> PrimitiveTypes.SHORT
            "int" -> PrimitiveTypes.INT
            "float" -> PrimitiveTypes.FLOAT
            "long" -> PrimitiveTypes.LONG
            "double" -> PrimitiveTypes.DOUBLE
            else -> {
                loader.loadClass(_className)
            }
        }

        val arr = IntArray(dimensions) { 0 }

        return if (dimensions == 0) {
            clazz
        } else {
            java.lang.reflect.Array.newInstance(clazz, *arr).javaClass
        }

    }

    fun getParsedClass(clazz: Class<*>, loader: ClassLoader): JavaClass? {
        val path = clazz.name.replace(".", "/") + ".class"
        val stream = loader.getResourceAsStream(path) ?: return null
        var parser: ClassParser

        stream.use {
            parser = ClassParser(stream, path)
            return parser.parse()
        }

        return null

    }

    fun getParsedMethod(clazz: JavaClass?, method: Executable, loader: ClassLoader): org.apache.bcel.classfile.Method? {

        if (clazz == null) {
            return null
        }

        if (method is Constructor<*>) {
            return clazz.methods.filter {
                it.name == "<init>" && method.parameterTypes.contentEquals(
                    getArgumentTypes(
                        it.signature, loader
                    ).toTypedArray()
                )
            }.firstOrNull()
        }

        return clazz.methods.filter {
            it.name == method.name && it.signature == Type.getSignature(
                method as Method
            )
        }.firstOrNull()
    }

    fun getParsedMethod(method: Method, loader: ClassLoader): org.apache.bcel.classfile.Method? {
        val clazz = getParsedClass(method.declaringClass, loader) ?: return null

        return clazz.methods.filter {
            it.name == method.name && it.signature == Type.getSignature(
                method
            )
        }.firstOrNull()
    }

    fun createLookup(clazz: Class<*>, loader: ClassLoader): MethodHandles.Lookup {
        val lookupClazz = loader.loadClass("java.lang.invoke.MethodHandles\$Lookup")
        val constructor = lookupClazz.getDeclaredConstructor(Class::class.java)
        constructor.isAccessible = true
        return constructor.newInstance(clazz) as MethodHandles.Lookup
    }

    fun getBootstrapMethod(clazz: Class<*>, index: Int): BootstrapMethod? {

        val path = clazz.name.replace(".", "/") + ".class"
        val stream = clazz.classLoader.getResourceAsStream(path)

        var parser: ClassParser
        var clazz: JavaClass

        stream.use {
            parser = ClassParser(stream, path)
            clazz = parser.parse()
        }

        var bsMethods =
            clazz.attributes.filter { it.tag == Const.ATTR_BOOTSTRAP_METHODS }.firstOrNull() ?: return null

        bsMethods = bsMethods as BootstrapMethods

        var bsMethod = bsMethods.bootstrapMethods[index] ?: return null

        return bsMethod
    }

    fun createMethodType(signature: String, loader: ClassLoader): MethodType {
        return MethodType.fromMethodDescriptorString(signature, loader)
    }

    fun createMethodType(method: Executable, loader: ClassLoader): MethodType {
        return MethodType.fromMethodDescriptorString(Type.getSignature(method as Method), loader)
    }

    fun createMethodHandle(kind: Int, method: Executable, loader: ClassLoader): MethodHandle {
        val lookup = createLookup(method.declaringClass, loader)
        val clazz = method.declaringClass
        val name = method.name
        val type = createMethodType(method, loader)

        return when (kind.toByte()) {
            Const.REF_invokeStatic -> {
                lookup.findStatic(clazz, name, type)
            }

            Const.REF_invokeVirtual, Const.REF_invokeInterface -> {
                lookup.findVirtual(clazz, name, type)
            }

            else -> {
                throw VlxVmException("Invalid kind $kind")
            }

        }

    }

    fun getArrayType(descriptor: String, loader: ClassLoader): Class<*> {
        var type = Type.getType(descriptor).toString()
        return if (type.endsWith("[]")) {
            type = type.substring(0, type.indexOf("["))
            getClass(type, loader)
        } else {
            getClass(type, loader)
        }
    }

    fun getArgumentTypes(signature: String, loader: ClassLoader): List<Class<*>> {
        val types = Type.getArgumentTypes(signature)
        val clazzes = mutableListOf<Class<*>>()
        repeat(types.size) {
            clazzes.add(getClass(types[it].toString(), loader))
        }
        return clazzes
    }

    fun getAllFields(clazz: Class<*>?): Array<Field> {
        var clazz = clazz
        val list: MutableList<Field> = mutableListOf()
        while (clazz != null) {
            list.addAll(ArrayList(Arrays.asList(*clazz.declaredFields)))
            clazz = clazz.superclass
        }
        return list.toTypedArray()
    }

    // 所有父类中的方法与接口中的方法，父类的方法优先
    fun getAllMethods(clazz: Class<*>?): Array<Method> {
        var infs = clazz!!.interfaces
        var clazz = clazz

        // 先按照顺序从父类中找
        val list: MutableList<Method> = mutableListOf()
        while (clazz != null) {
            list.addAll(listOf(*clazz.declaredMethods))
            clazz = clazz.superclass
        }

        // 然后再从接口方法中找
        for (inf in infs) {
            list.addAll(listOf(*inf.declaredMethods))
        }

        return list.toTypedArray()
    }

    fun getField(clazz: Class<*>, name: String): Field? {
        val fields = getAllFields(clazz)
        for (field in fields) {
            if (field.name == name) return field
        }
        return null
    }

    fun getMethod(clazz: Class<*>, name: String, signature: String, loader: ClassLoader): Executable? {

        // 每一个类都有构造函数，只有本类的自己声明的构造函数，才能构造自身，父类的构造函数，构造的就是父类，不是自身了
        if (name == "<init>") {
            val types = getArgumentTypes(signature, loader)
            return clazz.getDeclaredConstructor(*types.toTypedArray())
        }

        val methods = getAllMethods(clazz)

        for (method in methods) {
            if (method.name == name && Type.getSignature(method) == signature) {
                return method
            }
        }

        return null
    }

    fun createUnsafeInstance(clazz: Class<*>, loader: ClassLoader): Any {
        val unsafeClazz = loader.loadClass("sun.misc.Unsafe")
        val unsafeField = unsafeClazz.getDeclaredField("theUnsafe")
        unsafeField.isAccessible = true
        val unsafe: Unsafe = unsafeField.get(null) as Unsafe
        return unsafe.allocateInstance(clazz)
    }

    fun getParsedMethod(
        clazz: Class<*>,
        name: String,
        signature: String,
        loader: ClassLoader
    ): org.apache.bcel.classfile.Method? {

        val parsedClazz = getParsedClass(clazz, loader) ?: return null
        for (method in parsedClazz.methods) {
            if (method.name == name && method.signature == signature) {
                return method
            }
        }

        return null
    }

    fun isArray(obj: Any): Boolean {
        when (obj) {
            is ByteArray, is BooleanArray, is CharArray, is ShortArray, is IntArray, is FloatArray,
            is LongArray, is DoubleArray,
            is Array<*> -> {
                return true
            }
        }
        return false
    }

    fun arrToString(obj: Any): String {
        when (obj) {
            is ByteArray -> return Arrays.toString(obj)
            is BooleanArray -> return Arrays.toString(obj)
            is CharArray -> return Arrays.toString(obj)
            is ShortArray -> return Arrays.toString(obj)
            is IntArray -> return Arrays.toString(obj)
            is FloatArray -> return Arrays.toString(obj)
            is LongArray -> return Arrays.toString(obj)
            is DoubleArray -> return Arrays.toString(obj)
            is Array<*> -> {
                return Arrays.toString(obj)
            }

            else -> {
                throw VlxVmException("$obj is not an array")
            }

        }
    }

    fun setFieldValue(obj: Any?, field: Field, value: Any?) {
        if (obj == null) {
            fields.setStaticDirect(field, value)
        } else {
            fields.setDirect(obj, field, value)
        }
    }


}