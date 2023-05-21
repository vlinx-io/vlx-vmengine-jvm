package io.vlinx.vmengine

import org.apache.bcel.classfile.ClassParser
import org.apache.bcel.classfile.Method
import org.objectweb.asm.Opcodes
import java.io.ByteArrayInputStream
import java.net.URL
import java.net.URLClassLoader
import kotlin.collections.HashMap


/**
 * @author vlinx <vlinx@vlinx.io>
 * @create 2023-03-01
 * @version 1.0.0
 */

class VlxClassLoader(urls: Array<out URL>?) : URLClassLoader(urls) {


    val clinitMap = HashMap<String, Method>()


}