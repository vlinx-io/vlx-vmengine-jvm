package io.vlinx.vmengine

import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes.ACC_STATIC


/**
 * @author vlinx <vlinx@vlinx.io>
 * @create 2023-03-01
 * @version 1.0.0
 */

class VlxClassVisitor(api: Int, cv: ClassVisitor?) : ClassVisitor(api, cv) {


    override fun visitMethod(
        access: Int,
        name: String?,
        descriptor: String?,
        signature: String?,
        exceptions: Array<out String>?
    ): MethodVisitor? {

        if (access == ACC_STATIC && name == "<clinit>" && signature == "()V") {
            
            return null
        }

        // 实际是调用了cv.visitMethod实现串联
        return super.visitMethod(access, name, descriptor, signature, exceptions)
    }


}