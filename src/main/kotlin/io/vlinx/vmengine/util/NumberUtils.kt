package io.vlinx.image.analyzer.util

import io.vlinx.vmengine.VlxVmException
import io.vlinx.vmengine.util.ByteUtils
import org.apache.commons.codec.binary.Hex
import java.nio.ByteOrder

object NumberUtils {

    fun isValidNumber(obj: Any?): Boolean {

        obj ?: return false

        return try {
            toLong(obj)
            true
        } catch (t: Throwable) {
            try {
                toDouble(obj)
                true
            } catch (t: Throwable) {
                false
            }
        }
    }

    fun toLong(obj: Any): Long {
        return try {
            java.lang.Long.decode(obj.toString())
        } catch (t: Throwable) {
            // long才有这种0x模式
            try {
                val data = Hex.decodeHex(obj.toString().removePrefix("0x").trim())
                ByteUtils.toLong(data, ByteOrder.BIG_ENDIAN)
            } catch (t: Throwable) {
                throw t
            }
        }
    }

    fun toInt(obj: Any): Int {
        return toLong(obj).toInt()
    }

    fun toDouble(obj: Any): Double {
        return java.lang.Double.parseDouble(obj.toString())
    }

    fun toFloat(obj: Any): Float {
        return toDouble(obj).toFloat()
    }

    fun equalsInt(obj1: Any, obj2: Any): Boolean {
        if (NumberUtils.isValidNumber(obj1.toString()) && NumberUtils.isValidNumber(obj2.toString())) {
            return NumberUtils.toInt(obj1.toString()) == NumberUtils.toInt(obj2.toString())
        }

        return false
    }

    fun lvalueToInt(value: Any): Int {
        if (value is Int) return value

        if (value is Boolean) {
            return if (value) 1 else 0
        }

        if(value is Byte) return value.toInt()

        if(value is Char) return value.code

        if(value is Short) return value.toInt()

        throw VlxVmException("invalid low value[$value] to int")
    }
}
