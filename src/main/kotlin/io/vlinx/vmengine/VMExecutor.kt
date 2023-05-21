package io.vlinx.vmengine

import io.vlinx.image.analyzer.util.LEByteUtils
import io.vlinx.image.analyzer.util.NumberUtils
import io.vlinx.logging.Logger
import io.vlinx.vmengine.Opcodes.*
import io.vlinx.vmengine.VMExecutor.Operand.*
import io.vlinx.vmengine.util.ByteUtils
import io.vlinx.vmengine.util.ClassUtils
import io.vlinx.vmengine.util.ClassUtils.createLookup
import org.apache.bcel.classfile.*
import org.burningwave.core.classes.Fields
import sun.misc.Unsafe
import java.lang.StringBuilder
import java.lang.invoke.CallSite
import java.lang.invoke.MethodHandle
import java.lang.reflect.Constructor
import java.lang.reflect.Executable
import java.lang.reflect.Field
import java.nio.ByteOrder
import java.util.*
import java.util.concurrent.locks.ReentrantLock
import kotlin.collections.HashMap
import kotlin.system.exitProcess

class VMExecutor(val frame: Frame, val thread: VMThread) {

    var hasRetValue: Boolean = false
    var retValue: Any? = null
    private var currentOpcode: Opcodes? = null
    private var tab = ""

    enum class Operand {
        ADD,
        SUB,
        MULTI,
        DIV,
        REM,
        AND,
        OR,
        XOR,
    }

    enum class Condition {
        EQ,
        GT,
        LT,
        GTE,
        LTE,
        NEQ
    }

    private var sequence: RandomByteSequence = RandomByteSequence(frame.method.code.code)

    init {
        repeat(frame.level) {
            tab += "\t"
        }
    }

    private var pc = 0

    fun execute() {
        while (true) {
            try {
                pc = sequence.index()
                val opcode = sequence.readUnsignedByte().toShort()
                if (execute(opcode)) {
                    break
                }
            } catch (vme: VlxVmException) {
                Logger.FATAL(vme)
                exitProcess(1)
            } catch (t: Throwable) {
                handleException(t)
            }

        }
    }

    private fun execute(op: Short): Boolean {

        currentOpcode = getOpcode(op)
        verbose("L$pc: $currentOpcode")
        when (currentOpcode) {
            NOP -> {}
            ACONST_NULL -> push(null)
            ICONST_M1 -> push(-1)
            ICONST_0 -> push(0)
            ICONST_1 -> push(1)
            ICONST_2 -> push(2)
            ICONST_3 -> push(3)
            ICONST_4 -> push(4)
            ICONST_5 -> push(5)
            LCONST_0 -> pushl(0L)
            LCONST_1 -> pushl(1L)
            FCONST_0 -> push(0.0f)
            FCONST_1 -> push(1.0f)
            FCONST_2 -> push(2.0f)
            DCONST_0 -> push(0.0)
            DCONST_1 -> push(1.0)
            BIPUSH -> bipush()
            SIPUSH -> sipush()
            LDC -> ldc()
            LDC_W -> ldc_w()
            LDC2_W -> ldc_w()
            ILOAD -> load()
            LLOAD -> lload()
            FLOAD -> load()
            DLOAD -> lload()
            ALOAD -> load()
            ILOAD_0 -> load(0)
            ILOAD_1 -> load(1)
            ILOAD_2 -> load(2)
            ILOAD_3 -> load(3)
            LLOAD_0 -> lload(0)
            LLOAD_1 -> lload(1)
            LLOAD_2 -> lload(2)
            LLOAD_3 -> lload(3)
            FLOAD_0 -> load(0)
            FLOAD_1 -> load(1)
            FLOAD_2 -> load(2)
            FLOAD_3 -> load(3)
            DLOAD_0 -> lload(0)
            DLOAD_1 -> lload(1)
            DLOAD_2 -> lload(2)
            DLOAD_3 -> lload(3)
            ALOAD_0 -> load(0)
            ALOAD_1 -> load(1)
            ALOAD_2 -> load(2)
            ALOAD_3 -> load(3)
            IALOAD -> iaload()
            LALOAD -> laload()
            FALOAD -> faload()
            DALOAD -> daload()
            AALOAD -> aaload()
            BALOAD -> baload()
            CALOAD -> caload()
            SALOAD -> saload()
            ISTORE -> store()
            LSTORE -> lstore()
            FSTORE -> store()
            DSTORE -> lstore()
            ASTORE -> store()
            ISTORE_0 -> store(0)
            ISTORE_1 -> store(1)
            ISTORE_2 -> store(2)
            ISTORE_3 -> store(3)
            LSTORE_0 -> lstore(0)
            LSTORE_1 -> lstore(1)
            LSTORE_2 -> lstore(2)
            LSTORE_3 -> lstore(3)
            FSTORE_0 -> store(0)
            FSTORE_1 -> store(1)
            FSTORE_2 -> store(2)
            FSTORE_3 -> store(3)
            DSTORE_0 -> lstore(0)
            DSTORE_1 -> lstore(1)
            DSTORE_2 -> lstore(2)
            DSTORE_3 -> lstore(3)
            ASTORE_0 -> store(0)
            ASTORE_1 -> store(1)
            ASTORE_2 -> store(2)
            ASTORE_3 -> store(3)
            IASTORE -> iastore()
            LASTORE -> lastore()
            FASTORE -> fastore()
            DASTORE -> dastore()
            AASTORE -> aastore()
            BASTORE -> bastore()
            CASTORE -> castore()
            SASTORE -> sastore()
            POP -> pop()
            POP_2 -> pop_2()
            DUP -> dup()
            DUP_X1 -> dup_x1()
            DUP_X2 -> dup_x2()
            DUP2 -> dup2()
            DUP2_X1 -> dup2_x1()
            DUP2_X2 -> dup2_x2()
            SWAP -> swap()
            IADD -> icalc(ADD)
            LADD -> lcalc(ADD)
            FADD -> fcalc(ADD)
            DADD -> dcalc(ADD)
            ISUB -> icalc(SUB)
            LSUB -> lcalc(SUB)
            FSUB -> fcalc(SUB)
            DSUB -> dcalc(SUB)
            IMUL -> icalc(MULTI)
            LMUL -> lcalc(MULTI)
            FMUL -> fcalc(MULTI)
            DMUL -> dcalc(MULTI)
            IDIV -> icalc(DIV)
            LDIV -> lcalc(DIV)
            FDIV -> fcalc(DIV)
            DDIV -> dcalc(DIV)
            IREM -> icalc(REM)
            LREM -> lcalc(REM)
            FREM -> fcalc(REM)
            DREM -> dcalc(REM)
            INEG -> ineg()
            LNEG -> lneg()
            FNEG -> fneg()
            DNEG -> dneg()
            ISHL -> ishl()
            LSHL -> lshl()
            ISHR -> ishr()
            LSHR -> lshr()
            IUSHR -> iushr()
            LUSHR -> lushr()
            IAND -> icalc(AND)
            LAND -> lcalc(AND)
            IOR -> icalc(OR)
            LOR -> lcalc(OR)
            IXOR -> icalc(XOR)
            LXOR -> lcalc(XOR)
            IINC -> iinc()
            I2L -> i2l()
            I2F -> i2f()
            I2D -> i2d()
            L2I -> l2i()
            L2F -> l2f()
            L2D -> l2d()
            F2I -> f2i()
            F2L -> f2l()
            F2D -> f2d()
            D2I -> d2i()
            D2L -> d2l()
            D2F -> d2f()
            I2B -> i2b()
            I2C -> i2c()
            I2S -> i2s()
            LCMP -> lcmp()
            FCMPL -> fcmp(false)
            FCMPG -> fcmp(true)
            DCMPL -> dcmp(false)
            DCMPG -> dcmp(true)
            IFEQ -> ifcmp(Condition.EQ)
            IFNE -> ifcmp(Condition.NEQ)
            IFLT -> ifcmp(Condition.LT)
            IFGE -> ifcmp(Condition.GTE)
            IFGT -> ifcmp(Condition.GT)
            IFLE -> ifcmp(Condition.LTE)
            IF_ICMPEQ -> if_icmp(Condition.EQ)
            IF_ICMPNE -> if_icmp(Condition.NEQ)
            IF_ICMPLT -> if_icmp(Condition.LT)
            IF_ICMPGE -> if_icmp(Condition.GTE)
            IF_ICMPGT -> if_icmp(Condition.GT)
            IF_ICMPLE -> if_icmp(Condition.LTE)
            IF_ACMPEQ -> if_acmp(Condition.EQ)
            IF_ACMPNE -> if_acmp(Condition.NEQ)
            GOTO -> goto()
            TABLESWITCH -> tableswitch()
            LOOKUPSWITCH -> lookupswitch()
            IRETURN, FRETURN, ARETURN -> {
                this.hasRetValue = true
                this.retValue = pop()
                verbose("Return ${this.retValue}")
                return true
            }

            LRETURN -> {
                this.hasRetValue = true
                this.retValue = popl()
                return true
            }

            DRETURN -> {
                this.hasRetValue = true
                this.retValue = popd()
                return true
            }

            RETURN -> {
                return true
            }

            GETSTATIC -> getfield(true)
            PUTSTATIC -> putfield(true)
            GETFIELD -> getfield(false)
            PUTFIELD -> putfield(false)
            INVOKEVIRTUAL -> invokevirtual()
            INVOKESPECIAL -> invokespecial()
            INVOKESTATIC -> invokestatic()
            INVOKEINTERFACE -> invokeinterface()
            INVOKEDYNAMIC -> invokeDynamic()
            NEW -> new()
            NEWARRAY -> newarray()
            ANEWARRAY -> anewarray()
            ARRAYLENGTH -> arraylength()
            ATHROW -> athrow()
            CHECKCAST -> checkcast()
            INSTANCEOF -> instanceof()
            MONITORENTER -> monitorenter()
            MONITOREXIT -> monitorexit()
            WIDE -> TODO()
            MULTIANEWARRAY -> multinewarray()
            IFNULL -> ifnull(Condition.EQ)
            IFNONNULL -> ifnull(Condition.NEQ)
            GOTO_W -> goto_w()

            else -> {
                throw VlxVmException("Unsupported opcode $op")
            }
        }

        return false
    }

    private fun monitorenter() {
        val ref = pop()
        verbose(ref)

        synchronized(ref!!) {
            if (thread.engine.locks[ref] == null) {
                val lock = ReentrantLock()
                thread.engine.locks[ref] = lock
            }
        }

        thread.engine.locks[ref]!!.lock()
    }


    private fun monitorexit() {
        val ref = pop()
        verbose(ref)
        synchronized(ref!!) {
            thread.engine.locks[ref]!!.unlock()
        }
    }

    private fun bipush() {
        try {
            push(sequence.readByte().toInt())
        } catch (t: Throwable) {
            if (t is VlxVmException) {
                throw t
            }
            throw VlxVmException(t)
        }
    }

    private fun sipush() {
        try {
            push(sequence.readShort().toInt())
        } catch (t: Throwable) {
            if (t is VlxVmException) {
                throw t
            }
            throw VlxVmException(t)
        }

    }

    private fun ldc() {
        try {
            ldc(sequence.readUnsignedByte())
        } catch (t: Throwable) {
            if (t is VlxVmException) {
                throw t
            }
            throw VlxVmException(t)
        }
    }

    private fun ldc_w() {
        try {
            ldc(sequence.readUnsignedShort())
        } catch (t: Throwable) {
            if (t is VlxVmException) {
                throw t
            }
            throw VlxVmException(t)
        }
    }


    private fun ldc(index: Int) {
        try {
            verbose("#$index")
            val value = getConstantValue(index)
            verbose(value)
            if (value is Long || value is Double) {
                pushl(value)
            } else {
                push(value)
            }
        } catch (t: Throwable) {
            if (t is VlxVmException) {
                throw t
            }
            throw VlxVmException(t)
        }
    }

    private fun load() {
        try {
            val index = sequence.readUnsignedByte()
            load(index)
        } catch (t: Throwable) {
            if (t is VlxVmException) {
                throw t
            }
            throw VlxVmException(t)
        }
    }

    private fun lload() {
        try {
            val index = sequence.readUnsignedByte()
            lload(index)
        } catch (t: Throwable) {
            if (t is VlxVmException) {
                throw t
            }
            throw VlxVmException(t)
        }
    }

    private fun load(index: Int) {
        try {
            verbose("#$index")
            push(frame.localVars[index])
        } catch (t: Throwable) {
            if (t is VlxVmException) {
                throw t
            }
            throw VlxVmException(t)
        }
    }

    private fun lload(index: Int) {
        try {
            verbose("#$index")
            val low = frame.localVars[index] as ByteArray
            val high = frame.localVars[index + 1] as ByteArray
            push(low)
            push(high)
        } catch (t: Throwable) {
            if (t is VlxVmException) {
                throw t
            }
            throw VlxVmException(t)
        }
    }

    private fun getConstantValue(index: Int): Any? {

        try {

            val cp = frame.method.constantPool

            when (val c = cp.getConstant<Constant>(index)) {

                is ConstantString -> {
                    return getConstantValue(c.stringIndex)
                }

                is ConstantUtf8 -> {
                    return c.bytes
                }

                is ConstantClass -> {
                    val name = (getConstantValue(c.nameIndex) as String).replace("/", ".")
                    // [[开头的一定是多维数组
                    return if (name.startsWith("[[")) {
                        ClassUtils.getArrayType(name, thread.loader)
                    } else {
                        thread.loader.loadClass(name)
                    }
                }

                is ConstantInteger -> {
                    return c.bytes
                }

                is ConstantFloat -> {
                    return c.bytes
                }

                is ConstantLong -> {
                    return c.bytes
                }

                is ConstantDouble -> {
                    return c.bytes
                }

                is ConstantFieldref -> {
                    val clazz = getConstantValue(c.classIndex) as Class<*>
                    val nameAndType = getConstantValue(c.nameAndTypeIndex) as NameAndType
                    return ClassUtils.getField(clazz, nameAndType.name)
                }

                is ConstantMethodref, is ConstantInterfaceMethodref -> {
                    val clazz = getConstantValue((c as ConstantCP).classIndex) as Class<*>
                    val nameAndType = getConstantValue(c.nameAndTypeIndex) as NameAndType
                    verbose("$clazz, $nameAndType")
                    // 这里getMethod解决了向上找的问题
                    return ClassUtils.getMethod(clazz, nameAndType.name, nameAndType.type, thread.loader)
                }

                is ConstantMethodType -> {
                    return ClassUtils.createMethodType(getConstantValue(c.descriptorIndex) as String, thread.loader)
                }

                is ConstantMethodHandle -> {
                    val kind = c.referenceKind
                    val method = getConstantValue(c.referenceIndex) as Executable
                    return ClassUtils.createMethodHandle(kind, method, thread.loader)
                }


                is ConstantInvokeDynamic -> {
                    val bsIndex = c.bootstrapMethodAttrIndex
                    val bsMethod = ClassUtils.getBootstrapMethod(frame.clazz, bsIndex)
                    val nameAndType = getConstantValue(c.nameAndTypeIndex) as NameAndType
                    return InvokeDynamicInfo(bsMethod!!, nameAndType)
                }

                is ConstantDynamic -> {
                    TODO()
                }

                is ConstantPackage -> {
                    TODO()
                }

                is ConstantModule -> {
                    TODO()
                }

                is ConstantNameAndType -> {
                    return NameAndType(c.getName(cp), c.getSignature(cp))
                }

                else -> {
                    throw VlxVmException("Unknown constant $c")
                }

            }

            return null
        } catch (t: Throwable) {
            if (t is VlxVmException) {
                throw t
            }
            throw VlxVmException(t)
        }
    }


    private fun push(value: Any?) {
        verbose("push", value)
        try {
            frame.operandStack.push(value)
        } catch (t: Throwable) {
            if (t is VlxVmException) {
                throw t
            }
            throw VlxVmException(t)
        }
    }

    private fun pushl(value: Any) {
        verbose("pushl", value)
        try {
            val bytes = ByteUtils.toByteArray(value, ByteOrder.LITTLE_ENDIAN)
            // low
            push(bytes.sliceArray(0..3))
            // high
            push(bytes.sliceArray(4..7))
        } catch (t: Throwable) {
            if (t is VlxVmException) {
                throw t
            }
            throw VlxVmException(t)
        }
    }

    private fun baload() {
        try {
            val index = NumberUtils.lvalueToInt(pop()!!)
            val arr = pop() as ByteArray
            verbose("$arr[$index]")
            push(arr[index])
        } catch (t: Throwable) {
            if (t is VlxVmException) {
                throw t
            }
            throw VlxVmException(t)
        }
    }

    private fun caload() {
        try {
            val index = NumberUtils.lvalueToInt(pop()!!)
            val arr = pop() as CharArray
            verbose("$arr[$index]")
            push(arr[index])
        } catch (t: Throwable) {
            if (t is VlxVmException) {
                throw t
            }
            throw VlxVmException(t)
        }
    }

    private fun saload() {
        try {
            val index = pop().toString().toInt()
            val arr = pop() as ShortArray
            verbose("$arr[$index]")
            push(arr[index])
        } catch (t: Throwable) {
            if (t is VlxVmException) {
                throw t
            }
            throw VlxVmException(t)
        }
    }

    private fun iaload() {
        try {
            val index = pop().toString().toInt()
            val arr = pop() as IntArray
            verbose("$arr[$index]")
            push(arr[index])
        } catch (t: Throwable) {
            if (t is VlxVmException) {
                throw t
            }
            throw VlxVmException(t)
        }
    }

    private fun faload() {
        try {
            val index = pop().toString().toInt()
            val arr = pop() as FloatArray
            verbose("$arr[$index]")
            push(arr[index])
        } catch (t: Throwable) {
            if (t is VlxVmException) {
                throw t
            }
            throw VlxVmException(t)
        }
    }

    private fun aaload() {
        try {
            val index = pop().toString().toInt()
            var arr = pop() as Array<Any?>
            verbose("$arr[$index]")
            push(arr[index])
        } catch (t: Throwable) {
            if (t is VlxVmException) {
                throw t
            }
            throw VlxVmException(t)
        }
    }

    private fun laload() {
        try {
            val index = pop().toString().toInt()
            val arr = pop() as LongArray
            val buffer = ByteUtils.toByteArray(arr[index]!!, ByteOrder.LITTLE_ENDIAN)
            val low = buffer.sliceArray(0..3)
            val high = buffer.sliceArray(4..7)
            verbose("$arr[$index]")
            push(low)
            push(high)
        } catch (t: Throwable) {
            if (t is VlxVmException) {
                throw t
            }
            throw VlxVmException(t);
        }
    }

    private fun daload() {
        try {
            val index = pop().toString().toInt()
            val arr = pop() as DoubleArray
            val buffer = ByteUtils.toByteArray(arr[index]!!, ByteOrder.LITTLE_ENDIAN)
            val low = buffer.sliceArray(0..3)
            val high = buffer.sliceArray(4..7)
            verbose("$arr[$index]")
            push(low)
            push(high)
        } catch (t: Throwable) {
            if (t is VlxVmException) {
                throw t
            }
            throw VlxVmException(t)
        }
    }

    private fun store() {
        try {
            store(sequence.readUnsignedByte())
        } catch (t: Throwable) {
            if (t is VlxVmException) {
                throw t
            }
            throw VlxVmException(t)
        }
    }

    private fun store(index: Int) {
        try {
            val value = pop()
            setLocalVar(index, value)
        } catch (t: Throwable) {
            if (t is VlxVmException) {
                throw t
            }
            throw VlxVmException(t)
        }
    }

    private fun lstore() {
        try {
            lstore(sequence.readUnsignedByte())
        } catch (t: Throwable) {
            if (t is VlxVmException) {
                throw t
            }
            throw VlxVmException(t)
        }
    }

    private fun lstore(index: Int) {
        verbose(index)
        try {
            val high = pop()
            val low = pop()
            setLocalVar(index, low)
            setLocalVar(index + 1, high)
        } catch (t: Throwable) {
            if (t is VlxVmException) {
                throw t
            }
            throw VlxVmException(t)
        }
    }


    private fun bastore() {
        try {
            val value = pop() as Byte
            val index = pop().toString().toInt()
            val arr = pop() as ByteArray
            verbose("$arr[$index] = $value")
            arr[index] = value
        } catch (t: Throwable) {
            if (t is VlxVmException) {
                throw t
            }
            throw VlxVmException(t)
        }

    }

    private fun castore() {
        try {
            val value = pop() as Char
            val index = pop().toString().toInt()
            val arr = pop() as CharArray
            verbose("$arr[$index] = $value")
            arr[index] = value
        } catch (t: Throwable) {
            if (t is VlxVmException) {
                throw t
            }
            throw VlxVmException(t)
        }
    }

    private fun sastore() {
        try {
            val value = pop() as Short
            val index = pop().toString().toInt()
            val arr = pop() as ShortArray
            verbose("$arr[$index] = $value")
            arr[index] = value
        } catch (t: Throwable) {
            throw VlxVmException(t)
        }
    }

    private fun iastore() {
        try {
            val value = pop().toString().toInt()
            val index = pop().toString().toInt()
            val arr = pop() as IntArray
            verbose("$arr[$index] = $value")
            arr[index] = value
        } catch (t: Throwable) {
            if (t is VlxVmException) {
                throw t
            }
            throw VlxVmException(t)
        }
    }

    private fun fastore() {
        try {
            val value = pop() as Float
            val index = pop().toString().toInt()
            val arr = pop() as Array<Float>
            verbose("$arr[$index] = $value")
            arr[index] = value
        } catch (t: Throwable) {
            if (t is VlxVmException) {
                throw t
            }
            throw VlxVmException(t)
        }
    }

    private fun aastore() {
        try {
            val value = pop()
            val index = pop().toString().toInt()
            var arr = pop() as Array<Any?>
            arr[index] = value
        } catch (t: Throwable) {
            if (t is VlxVmException) {
                throw t
            }
            throw VlxVmException(t)
        }
    }

    private fun lastore() {
        try {
            val high = pop() as ByteArray
            val low = pop() as ByteArray
            val index = pop().toString().toInt()
            val arr = pop() as LongArray
            val buffer = low.plus(high)
            val value = LEByteUtils.toLong(buffer)
            verbose("$arr[$index] = $value")
            arr[index] = value
        } catch (t: Throwable) {
            if (t is VlxVmException) {
                throw t
            }
            throw VlxVmException(t)
        }
    }

    private fun dastore() {
        try {
            val high = pop() as ByteArray
            val low = pop() as ByteArray
            val index = pop().toString().toInt()
            val arr = pop() as DoubleArray
            val buffer = low.plus(high)
            val value = LEByteUtils.toDouble(buffer)
            verbose("$arr[$index] = $value")
            arr[index] = value
        } catch (t: Throwable) {
            if (t is VlxVmException) {
                throw t
            }
            throw VlxVmException(t)
        }
    }


    private fun pop(): Any? {
        try {
            val value = frame.operandStack.pop()
            verbose("pop", value)
            return value
        } catch (t: Throwable) {
            if (t is VlxVmException) {
                throw t
            }
            throw VlxVmException(t)
        }
    }

    private fun pop_2() {
        pop()
        pop()
    }

    private fun popl(): Long {
        try {
            val high = pop() as ByteArray
            val low = pop() as ByteArray
            val buffer = low.plus(high)
            val l = LEByteUtils.toLong(buffer)
            verbose("pop", l)
            return l
        } catch (t: Throwable) {
            if (t is VlxVmException) {
                throw t
            }
            throw VlxVmException(t)
        }
    }

    private fun popd(): Double {
        try {
            val high = pop() as ByteArray
            val low = pop() as ByteArray
            val buffer = low.plus(high)
            val d = LEByteUtils.toDouble(buffer)
            verbose("pop", d)
            return d
        } catch (t: Throwable) {
            if (t is VlxVmException) {
                throw t
            }
            throw VlxVmException(t)
        }
    }

    private fun dup() {
        val value = pop()
        push(value)
        push(value)
    }

    private fun dup_x1() {
        val v1 = pop()
        val v2 = pop()
        push(v1)
        push(v2)
        push(v1)
    }

    private fun dup_x2() {
        val v1 = pop()
        val v2 = pop()
        val v3 = pop()

        push(v1)
        push(v3)
        push(v2)
        push(v1)
    }

    private fun dup2() {
        val high = pop()
        val low = pop()
        push(low)
        push(high)
        push(low)
        push(high)
    }

    private fun dup2_x1() {

        val v1 = pop()
        val v2 = pop()
        val v3 = pop()

        push(v2)
        push(v1)
        push(v3)
        push(v2)
        push(v1)
    }

    private fun dup2_x2() {
        val v1 = pop()
        val v2 = pop()
        val v3 = pop()
        val v4 = pop()

        push(v2)
        push(v1)
        push(v4)
        push(v3)
        push(v2)
        push(v1)
    }

    private fun swap() {
        val top = pop()
        val bottom = pop()

        push(top)
        push(bottom)
    }

    private fun icalc(operand: Operand) {
        try {
            val value2 = NumberUtils.lvalueToInt(pop()!!)
            val value1 = NumberUtils.lvalueToInt(pop()!!)

            verbose("$value1 $operand $value2")

            val result = when (operand) {
                ADD -> value1 + value2
                SUB -> value1 - value2
                MULTI -> value1 * value2
                DIV -> value1 / value2
                REM -> value1 % value2
                AND -> value1 and value2
                OR -> value1 or value2
                XOR -> value1 xor value2
            }

            push(result)
        } catch (t: Throwable) {
            if (t is VlxVmException) {
                throw t
            }
            throw VlxVmException(t)
        }
    }

    private fun fcalc(operand: Operand) {

        try {
            val value2 = pop() as Float
            val value1 = pop() as Float

            verbose("$value1 $operand $value2")

            val result = when (operand) {
                ADD -> value1 + value2
                SUB -> value1 - value2
                MULTI -> value1 * value2
                DIV -> value1 / value2
                REM -> value1 % value2
                else -> {
                    throw VlxVmException("Invalid operand $operand for fcalc")
                }
            }

            push(result)
        } catch (t: Throwable) {
            if (t is VlxVmException) {
                throw t
            }
            throw VlxVmException(t)
        }
    }

    private fun lcalc(operand: Operand) {
        try {
            val value2 = popl()
            val value1 = popl()

            verbose("$value1 $operand $value2")

            val result = when (operand) {
                ADD -> value1 + value2
                SUB -> value1 - value2
                MULTI -> value1 * value2
                DIV -> value1 / value2
                REM -> value1 % value2
                AND -> value1 and value2
                OR -> value1 or value2
                XOR -> value1 xor value2
            }

            pushl(result)
        } catch (t: Throwable) {
            if (t is VlxVmException) {
                throw t
            }
            throw VlxVmException(t)
        }
    }

    private fun dcalc(operand: Operand) {
        try {
            val value2 = popd()
            val value1 = popd()

            verbose("$value1 $operand $value2")

            val result = when (operand) {
                ADD -> value1 + value2
                SUB -> value1 - value2
                MULTI -> value1 * value2
                DIV -> value1 / value2
                REM -> value1 % value2
                else -> {
                    throw VlxVmException("Invalid operand $operand for fcalc")
                }
            }

            pushl(result)
        } catch (t: Throwable) {
            if (t is VlxVmException) {
                throw t
            }
            throw VlxVmException(t)
        }
    }

    private fun ineg() {
        val value = NumberUtils.lvalueToInt(pop()!!)
        push(-value)
    }

    private fun fneg() {
        val value = pop() as Float
        push(-value)
    }


    private fun lneg() {
        val value = popl()
        pushl(-value)
    }

    private fun dneg() {
        val value = popd()
        pushl(-value)
    }

    private fun ishl() {
        try {
            val v2 = NumberUtils.lvalueToInt(pop()!!)
            val v1 = NumberUtils.lvalueToInt(pop()!!)

            verbose("$v1 shl $v2")

            // TODO 验证下这里是否需要转为无符号数
            val s = v2 and 0x1f
            val result = v1 shl s
            push(result)
        } catch (t: VlxVmException) {
            if (t is VlxVmException) {
                throw t
            }
            throw VlxVmException(t)
        }
    }

    private fun ishr() {
        try {
            val v2 = NumberUtils.lvalueToInt(pop()!!)
            val v1 = NumberUtils.lvalueToInt(pop()!!)

            verbose("$v1 shr $v2")

            // TODO 验证下这里是否需要转为无符号数
            val s = v2 and 0x1f
            val result = v1 shr s
            push(result)
        } catch (t: Throwable) {
            if (t is VlxVmException) {
                throw t
            }
            throw VlxVmException(t)
        }
    }

    private fun lshl() {
        try {
            val v2 = NumberUtils.lvalueToInt(pop()!!)
            val v1 = popl()

            verbose("$v1 lshl $v2")

            // TODO 研究下这两个移位命令,还有有符号数与无符号数的问题
            val s = v2 and 0x3f
            val result = v1 shl s
            pushl(result)
        } catch (t: Throwable) {
            if (t is VlxVmException) {
                throw t
            }
            throw VlxVmException(t)
        }
    }

    private fun lshr() {
        try {
            val v2 = NumberUtils.lvalueToInt(pop()!!)
            val v1 = popl()

            verbose("$v1 lshr $v2")

            val s = v2 and 0x3f
            val result = v1 shr s
            pushl(result)
        } catch (t: Throwable) {
            if (t is VlxVmException) {
                throw t
            }
            throw VlxVmException(t)
        }
    }


    private fun iushr() {
        try {
            val v2 = NumberUtils.lvalueToInt(pop()!!)
            val v1 = NumberUtils.lvalueToInt(pop()!!)

            verbose("$v1 iushr $v2")
            val result = v1 ushr v2
            push(result)
        } catch (t: Throwable) {
            if (t is VlxVmException) {
                throw t
            }
            throw VlxVmException(t)
        }
    }


    private fun lushr() {
        try {
            val v2 = NumberUtils.lvalueToInt(pop()!!)
            val v1 = popl()

            verbose("$v1 lushr $v2")
            val result = v1 ushr v2
            pushl(result)
        } catch (t: Throwable) {
            if (t is VlxVmException) {
                throw t
            }
            throw VlxVmException(t)
        }
    }

    private fun iinc() {
        try {
            val index = sequence.readUnsignedByte()
            val const = sequence.readByte()

            val value = frame.localVars[index].toString().toInt() + const
            setLocalVar(index, value)
        } catch (t: Throwable) {
            if (t is VlxVmException) {
                throw t
            }
            throw VlxVmException(t)
        }
    }

    private fun i2l() {
        try {
            val i = NumberUtils.lvalueToInt(pop()!!)
            pushl(i.toLong())
        } catch (t: Throwable) {
            if (t is VlxVmException) {
                throw t
            }
            throw VlxVmException(t)
        }
    }

    private fun i2f() {
        try {
            val i = NumberUtils.lvalueToInt(pop()!!)
            push(i.toFloat())
        } catch (t: Throwable) {
            if (t is VlxVmException) {
                throw t
            }
            throw VlxVmException(t)
        }
    }

    private fun i2d() {
        try {
            val i = NumberUtils.lvalueToInt(pop()!!)
            pushl(i.toDouble())
        } catch (t: Throwable) {
            if (t is VlxVmException) {
                throw t
            }
            throw VlxVmException(t)
        }
    }

    private fun l2i() {
        try {
            val l = popl()
            push(l.toInt())
        } catch (t: Throwable) {
            if (t is VlxVmException) {
                throw t
            }
            throw VlxVmException(t)
        }
    }

    private fun l2f() {
        try {
            val l = popl()
            push(l.toFloat())
        } catch (t: Throwable) {
            if (t is VlxVmException) {
                throw t
            }
            throw VlxVmException(t)
        }
    }

    private fun l2d() {
        try {
            val l = popl()
            pushl(l.toDouble())
        } catch (t: Throwable) {
            if (t is VlxVmException) {
                throw t
            }
            throw VlxVmException(t)
        }
    }

    private fun f2i() {
        try {
            val f = pop() as Float
            push(f.toInt())
        } catch (t: Throwable) {
            if (t is VlxVmException) {
                throw t
            }
            throw VlxVmException(t)
        }
    }

    private fun f2l() {
        try {
            val f = pop() as Float
            pushl(f.toLong())
        } catch (t: Throwable) {
            if (t is VlxVmException) {
                throw t
            }
            throw VlxVmException(t)
        }
    }

    private fun f2d() {
        try {
            val f = pop() as Float
            pushl(f.toDouble())
        } catch (t: Throwable) {
            if (t is VlxVmException) {
                throw t
            }
            throw VlxVmException(t)
        }
    }

    private fun d2i() {
        try {
            val d = popd()
            push(d.toInt())
        } catch (t: Throwable) {
            if (t is VlxVmException) {
                throw t
            }
            throw VlxVmException(t)
        }
    }

    private fun d2l() {
        try {
            val d = popd()
            pushl(d.toLong())
        } catch (t: Throwable) {
            if (t is VlxVmException) {
                throw t
            }
            throw VlxVmException(t)
        }
    }

    private fun d2f() {
        try {
            val d = popd()
            push(d.toFloat())
        } catch (t: Throwable) {
            if (t is VlxVmException) {
                throw t
            }
            throw VlxVmException(t)
        }
    }

    private fun i2b() {
        try {
            val i = NumberUtils.lvalueToInt(pop()!!)
            push(i.toByte())
        } catch (t: Throwable) {
            if (t is VlxVmException) {
                throw t
            }
            throw VlxVmException(t)
        }
    }

    private fun i2c() {
        try {
            val i = NumberUtils.lvalueToInt(pop()!!)
            push(i.toChar())
        } catch (t: Throwable) {
            if (t is VlxVmException) {
                throw t
            }
            throw VlxVmException(t)
        }
    }

    private fun i2s() {
        try {
            val i = NumberUtils.lvalueToInt(pop()!!)
            push(i.toShort())
        } catch (t: Throwable) {
            if (t is VlxVmException) {
                throw t
            }
            throw VlxVmException(t)
        }
    }

    private fun getfield(static: Boolean) {
        try {
            val index = sequence.readUnsignedShort()
            verbose("#$index")
            val field = getConstantValue(index) as Field? ?: throw VlxVmException("Can't get field at index: $index")
            field.isAccessible = true

            val obj = if (static) null else pop()
            val value = field.get(obj)
            verbose(field)
            if (value is Long || value is Double) {
                pushl(value)
            } else {
                push(value)
            }
        } catch (t: Throwable) {
            if (t is VlxVmException) {
                throw t
            }
            throw VlxVmException(t)
        }
    }


    private fun putfield(static: Boolean) {

        try {
            val index = sequence.readUnsignedShort()
            val field = getConstantValue(index) as Field? ?: throw Exception("Can't get field at index: $index")
            val value = when (field.type) {
                PrimitiveTypes.LONG -> {
                    popl()
                }

                PrimitiveTypes.DOUBLE -> {
                    popd()
                }

                else -> {
                    pop()
                }
            }
            val obj = if (static) null else pop()

            when (field.type) {
                PrimitiveTypes.BYTE -> {
                    if (value is Byte) {
                        ClassUtils.setFieldValue(obj, field, value)
                    } else {
                        ClassUtils.setFieldValue(obj, field, NumberUtils.lvalueToInt(value!!).toByte())
                    }
                }

                PrimitiveTypes.BOOLEAN -> {
                    if (value is Boolean) {
                        ClassUtils.setFieldValue(obj, field, value)
                    } else {
                        val i = value.toString().toInt()
                        when (i) {
                            1 -> {
                                ClassUtils.setFieldValue(obj, field, true)
                            }

                            0 -> {
                                ClassUtils.setFieldValue(obj, field, false)
                            }

                            else -> {
                                throw VlxVmException("Invalid value[$value] from boolean")
                            }
                        }
                    }
                }

                PrimitiveTypes.CHAR -> {
                    if (value is Char) {
                        ClassUtils.setFieldValue(obj, field, value)
                    } else {
                        ClassUtils.setFieldValue(obj, field, NumberUtils.lvalueToInt(value!!).toChar())
                    }
                }

                PrimitiveTypes.SHORT -> {
                    if (value is Short) {
                        ClassUtils.setFieldValue(obj, field, value)
                    } else {
                        ClassUtils.setFieldValue(obj, field, NumberUtils.lvalueToInt(value!!).toShort())
                    }
                }

                else -> {
                    ClassUtils.setFieldValue(obj, field, value)
                }

            }
        } catch (t: Throwable) {
            if (t is VlxVmException) {
                throw t
            }
            throw VlxVmException(t)
        }


    }

    private fun new() {
        try {
            val index = sequence.readUnsignedShort()
            val clazz = getConstantValue(index) as Class<*>
            verbose(clazz)
            push(allocateInstance(clazz))
        } catch (t: Throwable) {
            if (t is VlxVmException) {
                throw t
            }
            throw VlxVmException(t)
        }
    }

    private fun allocateInstance(clazz: Class<*>): Any {
        try {
            if (!Options.handleInit) {
                return InstanceToCreate(clazz)
            }

            return ClassUtils.createUnsafeInstance(clazz, thread.loader)
        } catch (t: Throwable) {
            if (t is VlxVmException) {
                throw t
            }
            throw VlxVmException(t)
        }

    }

    private fun newarray() {
        try {
            val atype = sequence.readUnsignedByte()
            val size = NumberUtils.lvalueToInt(pop()!!)

            val arr = when (atype) {
                T_BOOLEAN -> {
                    BooleanArray(size)
                }

                T_BYTE -> {
                    ByteArray(size)
                }

                T_CHAR -> {
                    CharArray(size)
                }

                T_SHORT -> {
                    ShortArray(size)
                }

                T_INT -> {
                    IntArray(size)
                }

                T_FLOAT -> {
                    FloatArray(size)
                }

                T_LONG -> {
                    LongArray(size)
                }

                T_DOUBLE -> {
                    DoubleArray(size)
                }

                else -> {
                    throw VlxVmException("Unknown array type")
                }
            }
            push(arr)
        } catch (t: Throwable) {
            if (t is VlxVmException) {
                throw t
            }
            throw VlxVmException(t)
        }
    }

    private fun anewarray() {
        try {
            val index = sequence.readUnsignedShort()
            val clazz = getConstantValue(index) as Class<*>
            val instance = allocateInstance(clazz)
            val size = NumberUtils.lvalueToInt(pop()!!)
            val arr = Array(size) { instance }
            push(arr)
        } catch (t: Throwable) {
            if (t is VlxVmException) {
                throw t
            }
            throw VlxVmException(t)
        }
    }

    private fun arraylength() {
        try {
            var ref = pop()
            when (ref) {
                is ByteArray -> {
                    push(ref.size)
                }

                is BooleanArray -> {
                    push(ref.size)
                }

                is CharArray -> {
                    push(ref.size)
                }

                is ShortArray -> {
                    push(ref.size)
                }

                is IntArray -> {
                    push(ref.size)
                }

                is FloatArray -> {
                    push(ref.size)
                }

                is LongArray -> {
                    push(ref.size)
                }

                is DoubleArray -> {
                    push(ref.size)
                }

                else -> {
                    ref as Array<Any?>
                    push(ref.size)
                }
            }
        } catch (t: Throwable) {
            if (t is VlxVmException) {
                throw t
            }
            throw VlxVmException(t)
        }
    }

    // 多维数组，本质也是数组，就是数组的数组，不考虑类型的情况，
    private fun multinewarray() {
        try {
            val type = getConstantValue(sequence.readUnsignedShort()) as Class<*>
            val dimensions = sequence.readUnsignedByte()
            // 堆栈头部应该是最外围数组大小，可以在代码中验证下

            val sizes = mutableListOf<Int>()
            repeat(dimensions) {
                sizes.add(NumberUtils.lvalueToInt(pop()!!))
            }

            val arr = createMultiDimensionalArray(sizes.reversed(), type)
            push(arr)
        } catch (t: Throwable) {
            if (t is VlxVmException) {
                throw t
            }
            throw VlxVmException(t)
        }
    }

    private fun instanceof() {
        try {
            val index = sequence.readUnsignedShort()
            val clazz = getConstantValue(index) as Class<*>
            val ref = pop()
            val result = instanceOf(ref, clazz)
            if (result) {
                push(1)
            } else {
                push(0)
            }
        } catch (t: Throwable) {
            if (t is VlxVmException) {
                throw t
            }
            throw VlxVmException(t)
        }
    }

    private fun checkcast() {
        try {
            val index = sequence.readUnsignedShort()
            val clazz = getConstantValue(index) as Class<*>
            val ref = pop() ?: return
            push(ref)
            val result = instanceOf(ref, clazz)
            if (!result) {
                throw ClassCastException("${ref!!.javaClass} can't be cast to ${clazz}")
            }
        } catch (t: Throwable) {
            if (t is VlxVmException) {
                throw t
            }
            throw VlxVmException(t)
        }
    }

    private fun instanceOf(obj: Any?, clazz: Class<*>): Boolean {
        try {
            val instance = ClassUtils.createUnsafeInstance(clazz, thread.loader)
            return instance.javaClass.isInstance(obj)
        } catch (t: Throwable) {
            if (t is VlxVmException) {
                throw t
            }
            throw VlxVmException(t)
        }
    }

    private fun lcmp() {
        try {
            val v2 = popl()
            val v1 = popl()
            verbose("$v1, $v2")
            if (v1 > v2) {
                push(1)
            } else if (v1 == v2) {
                push(0)
            } else {
                push(-1)
            }
        } catch (t: Throwable) {
            if (t is VlxVmException) {
                throw t
            }
            throw VlxVmException(t)
        }
    }

    private fun fcmp(g: Boolean) {
        try {
            val v2 = pop() as Float
            val v1 = pop() as Float
            verbose("$v1, $v2")
            if (v1.isNaN() || v2.isNaN()) {
                if (g) push(1) else push(-1)
                return
            }
            if (v1 > v2) {
                push(1)
            } else if (v1 == v2) {
                push(0)
            } else {
                push(-1)
            }
        } catch (t: Throwable) {
            if (t is VlxVmException) {
                throw t
            }
            throw VlxVmException(t)
        }
    }

    private fun dcmp(g: Boolean) {
        try {
            val v2 = popd()
            val v1 = popd()
            verbose("$v1, $v2")
            if (v1.isNaN() || v2.isNaN()) {
                if (g) push(1) else push(-1)
                return
            }
            if (v1 > v2) {
                push(1)
            } else if (v1 == v2) {
                push(0)
            } else {
                push(-1)
            }
        } catch (t: Throwable) {
            if (t is VlxVmException) {
                throw t
            }
            throw VlxVmException(t)
        }

    }

    private fun ifcmp(condition: Condition) {
        try {
            // 这里必然是一个有符号数
            val offset = sequence.readShort()
            val value = NumberUtils.lvalueToInt(pop()!!)

            verbose("$value $condition 0")

            val result: Boolean = when (condition) {
                Condition.EQ -> value == 0
                Condition.GT -> value > 0
                Condition.LT -> value < 0
                Condition.GTE -> value >= 0
                Condition.LTE -> value <= 0
                Condition.NEQ -> value != 0
            }

            if (result) {
                jmp(pc + offset)
            }
        } catch (t: Throwable) {
            if (t is VlxVmException) {
                throw t
            }
            throw VlxVmException(t)
        }
    }

    private fun if_icmp(condition: Condition) {
        try {
            val offset = sequence.readShort()
            val v2 = NumberUtils.lvalueToInt(pop()!!)
            val v1 = NumberUtils.lvalueToInt(pop()!!)

            verbose("$v1 $condition $v2")

            val result: Boolean = when (condition) {
                Condition.EQ -> v1 == v2
                Condition.GT -> v1 > v2
                Condition.LT -> v1 < v2
                Condition.GTE -> v1 >= v2
                Condition.LTE -> v1 <= v2
                Condition.NEQ -> v1 != v2

            }

            if (result) {
                jmp(pc + offset)
            }
        } catch (t: Throwable) {
            if (t is VlxVmException) {
                throw t
            }
            throw VlxVmException(t)
        }

    }

    private fun if_acmp(condition: Condition) {
        try {
            val offset = sequence.readShort()
            val v2 = pop()
            val v1 = pop()

            verbose("$v1 $condition $v2")

            val result: Boolean = when (condition) {
                Condition.EQ -> v1 == v2
                Condition.NEQ -> v1 != v2
                else -> {
                    throw VlxVmException("Invalid condition $condition")
                }
            }

            if (result) {
                jmp(pc + offset)
            }
        } catch (t: Throwable) {
            if (t is VlxVmException) {
                throw t
            }
            throw VlxVmException(t)
        }
    }

    private fun ifnull(condition: Condition) {
        try {
            val offset = sequence.readShort()
            val v1 = pop()

            verbose("$v1 $condition null")

            val result: Boolean = when (condition) {
                Condition.EQ -> v1 == null
                Condition.NEQ -> v1 != null
                else -> {
                    throw VlxVmException("Invalid condition $condition")
                }
            }

            if (result) {
                jmp(pc + offset)
            }
        } catch (t: Throwable) {
            if (t is VlxVmException) {
                throw t
            }
            throw VlxVmException(t)
        }
    }

    private fun invokestatic() {
        try {
            val index = sequence.readUnsignedShort()
            verbose("#$index")
            val method = getConstantValue(index) as Executable
            invokeMethod(method, false)
        } catch (t: Throwable) {
            if (t is VlxVmException) {
                throw t
            }
            throw VlxVmException(t)
        }
    }

    private fun invokevirtual() {
        try {
            val index = sequence.readUnsignedShort()
            verbose("#$index")
            val method = getConstantValue(index) as Executable?
            if (method == null) {
                println("error")
            }
            invokeMethod(method!!, true)
        } catch (t: Throwable) {
            if (t is VlxVmException) {
                throw t
            }
            throw VlxVmException(t)
        }
    }


    private fun invokeinterface() {
        try {
            val index = sequence.readUnsignedShort()
            verbose("#$index")
            sequence.readUnsignedShort()
            val method = getConstantValue(index) as Executable
            invokeMethod(method, true)
        } catch (t: Throwable) {
            if (t is VlxVmException) {
                throw t
            }
            throw VlxVmException(t)
        }
    }

    private fun invokespecial() {
        try {
            val index = sequence.readUnsignedShort()
            verbose("#$index")
            val method = getConstantValue(index) as Executable
            invokeMethod(method, true)
        } catch (t: Throwable) {
            if (t is VlxVmException) {
                throw t
            }
            throw VlxVmException(t)
        }

    }

    private fun generateArgs(method: Executable, instance: Boolean, handle: Boolean): List<Any?> {

        try {

            val args = mutableListOf<Any?>()

            if (handle) {
                for (param in method.parameters.reversed()) {
                    if (param.type == PrimitiveTypes.LONG || param.type == PrimitiveTypes.DOUBLE) {
                        args.add(pop())
                        args.add(pop())
                    } else {
                        args.add(pop())
                    }
                }
            } else {
                for (param in method.parameters.reversed()) {
                    when (param.type) {
                        PrimitiveTypes.BYTE -> args.add((NumberUtils.lvalueToInt(pop()!!)).toByte())
                        PrimitiveTypes.BOOLEAN -> {
                            when (NumberUtils.lvalueToInt(pop()!!)) {
                                1 -> {
                                    args.add(true)
                                }

                                0 -> {
                                    args.add(false)
                                }

                                else -> {
                                    // TODO
                                    throw VlxVmException("boolean exception")
                                }
                            }
                        }

                        PrimitiveTypes.CHAR -> args.add(NumberUtils.lvalueToInt(pop()!!).toChar())
                        PrimitiveTypes.SHORT -> args.add(NumberUtils.lvalueToInt(pop()!!).toShort())
                        PrimitiveTypes.INT -> args.add(NumberUtils.lvalueToInt(pop()!!))
                        PrimitiveTypes.FLOAT -> args.add(pop() as Float)
                        PrimitiveTypes.LONG -> args.add(popl())
                        PrimitiveTypes.DOUBLE -> args.add(popd())
                        else -> args.add(pop())
                    }
                }
            }

            if (instance) {
                val receiver = pop()
                args.add(receiver)
            }

            return args.reversed()
        } catch (t: Throwable) {
            if (t is VlxVmException) {
                throw t
            }
            throw VlxVmException(t)
        }

    }

    private fun invokeMethod(method: Executable, instance: Boolean) {
        try {
            verbose(method)
            method.isAccessible = true

            if (method is Constructor<*>) {
                invokeConstructor(method)
            } else {
                val args = generateArgs(method, instance, Options.handleSubMethod)
                val receiver = if (instance) {
                    args[0]
                } else {
                    null
                }

                val retValue = thread.execute(receiver, method, args, Options.handleSubMethod, frame.level + 1)
                retValue?.let {
                    when (retValue.value) {
                        is Double, is Long -> {
                            pushl(retValue.value)
                        }

                        else -> {
                            push(retValue.value)
                        }
                    }

                }
            }
        } catch (t: Throwable) {
            if (t is VlxVmException) {
                throw t
            }
            throw VlxVmException(t)
        }


    }

    private fun invokeConstructor(method: Constructor<*>) {
        try {
            val args = generateArgs(method, false, Options.handleInit)
            if (Options.handleInit) {
                thread.execute(null, method, args, Options.handleSubMethod, frame.level + 1)
            } else {
                verbose("Execute new instance: $method")
                verbose("Args: $args")
                val instance = method.newInstance(*args.toTypedArray())
                val instanceToCreate = pop()

                repeat(this.frame.localVars.size) {
                    if (this.frame.localVars[it] == instanceToCreate) {
                        this.frame.localVars[it] = instance
                    }
                }

                repeat(this.frame.operandStack.size) {
                    if (this.frame.operandStack[it] == instanceToCreate) {
                        this.frame.operandStack[it] = instance
                    }
                }
            }
        } catch (t: Throwable) {
            if (t is VlxVmException) {
                throw t
            }
            throw VlxVmException(t)
        }
    }


    private fun goto() {
        try {
            val offset = sequence.readShort()
            jmp(pc + offset)
        } catch (t: Throwable) {
            if (t is VlxVmException) {
                throw t
            }
            throw VlxVmException(t)
        }
    }

    private fun goto_w() {
        try {
            val offset = sequence.readInt()
            jmp(pc + offset)
        } catch (t: Throwable) {
            if (t is VlxVmException) {
                throw t
            }
            throw VlxVmException(t)
        }
    }

    private fun lookupswitch() {
        try {
            skipPadding()
            val defaultOffset = sequence.readInt()
            val paris = sequence.readInt()

            val branches = HashMap<Int, Int>()
            repeat(paris) {
                val key = sequence.readInt()
                val offset = sequence.readInt()
                branches[key] = offset
            }

            val index = NumberUtils.lvalueToInt(pop()!!)
            val target = branches[index]
            if (target != null) {
                jmp(target + pc)
            } else {
                jmp(defaultOffset + pc)
            }
        } catch (t: Throwable) {
            if (t is VlxVmException) {
                throw t
            }
            throw VlxVmException(t)
        }
    }

    private fun tableswitch() {
        try {
            skipPadding()
            val defaultOffset = sequence.readInt()
            val low = sequence.readInt()
            val high = sequence.readInt()
            val arr = IntArray(high - low + 1)
            repeat(arr.size) {
                arr[it] = sequence.readInt()
            }
            val index = NumberUtils.lvalueToInt(pop()!!)
            val target = if (index in 0 until arr.size) {
                arr[index] + pc
            } else {
                defaultOffset + pc
            }
            jmp(target)
        } catch (t: Throwable) {
            if (t is VlxVmException) {
                throw t
            }
            throw VlxVmException(t)
        }
    }


    private fun setLocalVar(index: Int, value: Any?) {
        try {
            verbose("localVars[$index] = $value")
            this.frame.localVars[index] = value
        } catch (t: Throwable) {
            if (t is VlxVmException) {
                throw t
            }
            throw VlxVmException(t)
        }
    }

    private fun jmp(target: Int) {
        try {
            verbose("jmp $target")
            sequence.seek(target)
        } catch (t: Throwable) {
            if (t is VlxVmException) {
                throw t
            }
            throw VlxVmException(t)
        }
    }

    private fun skipPadding() {
        try {
            while (sequence.index() % 4 != 0) {
                sequence.readByte()
            }
        } catch (t: Throwable) {
            if (t is VlxVmException) {
                throw t
            }
            throw VlxVmException(t)
        }
    }

    private fun invokeDynamic() {
        try {

            val index = sequence.readUnsignedShort()
            verbose("#$index")
            val info = getConstantValue(index) as InvokeDynamicInfo

            // 指向创建CallSite的Method
            val methodHandle = getConstantValue(info.bsMethod.bootstrapMethodRef) as MethodHandle

            val lookup = createLookup(frame.clazz, thread.loader)
            // 托管在CallSite的MethodHandle的name与Type
            val name = info.nameAndType.name
            val methodType = ClassUtils.createMethodType(info.nameAndType.type, thread.loader)

            // invokeDynamic调用的前三个参数
            val args = mutableListOf<Any?>()
            args.add(lookup)
            args.add(name)
            args.add(methodType)

            repeat(info.bsMethod.bootstrapArguments.size) {
                args.add(getConstantValue(info.bsMethod.bootstrapArguments[it]))
            }

            val parameters = mutableListOf<Any?>()
            for (type in methodType.parameterArray().reversed()) {
                if (type == PrimitiveTypes.DOUBLE) {
                    parameters.add(popd())
                } else if (type == PrimitiveTypes.LONG) {
                    parameters.add(popl())
                } else {
                    parameters.add(pop())
                }
            }

            val callSite = methodHandle.invokeWithArguments(args) as CallSite
            val invoker = callSite.dynamicInvoker()
            // 需要reversed两遍
            val result = invoker.invokeWithArguments(parameters.reversed())

            if (methodType.returnType() == PrimitiveTypes.VOID) {
                return
            }

            if (methodType.returnType() == PrimitiveTypes.DOUBLE || methodType.returnType() == PrimitiveTypes.LONG) {
                pushl(result)
            } else {
                push(result)
            }
        } catch (t: Throwable) {
            if (t is VlxVmException) {
                throw t
            }
            throw VlxVmException(t)
        }

    }

    private fun athrow() {
        val exception = pop()

        if (exception is Throwable) {
            throw exception
        }

        throw VlxVmException("Invalid throwable object: $exception")
    }

    private fun jsr() {
        try {
            val offset = sequence.readUnsignedShort()
            val nextPC = sequence.index()
            push(nextPC)
            jmp(offset + pc)
        } catch (t: Throwable) {
            if (t is VlxVmException) {
                throw t
            }
            throw VlxVmException(t)
        }
    }

    private fun jsr_w() {
        try {
            val offset = sequence.readInt()
            val nextPC = sequence.index()
            push(nextPC)
            jmp(offset + pc)
        } catch (t: Throwable) {
            if (t is VlxVmException) {
                throw t
            }
            throw VlxVmException(t)
        }
    }

    private fun ret() {
        try {
            val target = NumberUtils.lvalueToInt(pop()!!)
            jmp(target)
        } catch (t: Throwable) {
            if (t is VlxVmException) {
                throw t
            }
            throw VlxVmException(t)
        }
    }

    private fun createMultiDimensionalArray(sizes: List<Int>, type: Class<*>): Any {
        try {
            val size = sizes.first()

            if (sizes.size == 1) {
                return when (type) {
                    PrimitiveTypes.BYTE -> ByteArray(size)
                    PrimitiveTypes.BOOLEAN -> BooleanArray(size)
                    PrimitiveTypes.CHAR -> CharArray(size)
                    PrimitiveTypes.SHORT -> ShortArray(size)
                    PrimitiveTypes.INT -> IntArray(size)
                    PrimitiveTypes.FLOAT -> FloatArray(size)
                    PrimitiveTypes.LONG -> LongArray(size)
                    PrimitiveTypes.DOUBLE -> DoubleArray(size)
                    else -> Array<Any?>(size) { null }
                }
            }

            val arr = Array<Any?>(size) { null }
            repeat(size) {
                arr[it] = createMultiDimensionalArray(sizes.subList(1, sizes.size), type)
            }
            return arr
        } catch (t: Throwable) {
            if (t is VlxVmException) {
                throw t
            }
            throw VlxVmException(t)
        }
    }

    private fun handleException(t: Throwable) {
        verbose("Handle exception: $t")

        val exceptionTable = frame.method.code.exceptionTable

        for (exception in exceptionTable) {
            if (pc >= exception.startPC && pc <= exception.endPC) {
                if (exception.catchType == 0) {
                    push(t)
                    sequence.seek(exception.handlerPC)
                    return
                }

                val clazz = getConstantValue(exception.catchType) as Class<*>
                if (instanceOf(t, clazz)) {
                    push(t)
                    sequence.seek(exception.handlerPC)
                    return
                }
            }
        }

        throw t

    }

    fun verbose(vararg args: Any?) {

        val builder = StringBuilder()
        for (arg in args) {
            if (ClassUtils.isArray(arg!!)) {
                builder.append(ClassUtils.arrToString(arg))
            } else if (arg is String) {
                builder.append("\"$arg\"")
            } else {
                builder.append(arg)
            }
            builder.append(" ")
        }
        val str = builder.toString().trimEnd()

        try {
            Logging.verbose("$tab$str")
        } catch (t: Throwable) {
            if (t is VlxVmException) {
                throw t
            }
            throw VlxVmException(t)
        }
    }

}