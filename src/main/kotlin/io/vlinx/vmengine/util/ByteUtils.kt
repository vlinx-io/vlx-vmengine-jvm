package io.vlinx.vmengine.util

import io.vlinx.vmengine.VlxVmException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.min


/**
 * @author vlinx <vlinx@vlinx.io>
 * @create 2022-04-09
 * @version 1.0.0
 */

object ByteUtils {

    fun toShort(array: ByteArray, order: ByteOrder): Short {
        val buffer = ByteBuffer.wrap(array)
        buffer.order(order)
        return buffer.short
    }

    fun toInt(array: ByteArray, order: ByteOrder): Int {
        val buffer = ByteBuffer.wrap(array)
        buffer.order(order)
        return buffer.int
    }

    fun toLong(array: ByteArray, order: ByteOrder): Long {
        val buffer = ByteBuffer.wrap(array)
        buffer.order(order)
        return buffer.long
    }

    fun toFloat(array: ByteArray, order: ByteOrder): Float {
        val buffer = ByteBuffer.wrap(array)
        buffer.order(order)
        return buffer.float
    }

    fun toDouble(array: ByteArray, order: ByteOrder): Double {
        val buffer = ByteBuffer.wrap(array)
        buffer.order(order)
        return buffer.double
    }

    fun toByteArray(value: Short, order: ByteOrder): ByteArray {
        val bytes = ByteArray(2)
        ByteBuffer.wrap(bytes).order(order).putShort(value)
        return bytes
    }

    fun toByteArray(value: Int, order: ByteOrder): ByteArray {
        val bytes = ByteArray(4)
        ByteBuffer.wrap(bytes).order(order).putInt(value)
        return bytes
    }

    fun toByteArray(value: Long, order: ByteOrder): ByteArray {
        val bytes = ByteArray(8)
        ByteBuffer.wrap(bytes).order(order).putLong(value)
        return bytes
    }

    fun toByteArray(value: Float, order: ByteOrder): ByteArray {
        val bytes = ByteArray(4)
        ByteBuffer.wrap(bytes).order(order).putFloat(value)
        return bytes
    }

    fun toByteArray(value: Double, order: ByteOrder): ByteArray {
        val bytes = ByteArray(8)
        ByteBuffer.wrap(bytes).order(order).putDouble(value)
        return bytes
    }

    fun copy(src: ByteArray, dest: ByteArray) {
        copy(src, dest, min(src.size, dest.size))
    }

    fun copy(src: ByteArray, dest: ByteArray, size: Int) {
        System.arraycopy(src, 0, dest, 0, size)
    }

    fun toByteArray(value: Any, order: ByteOrder): ByteArray {

        when (value) {
            is Short -> {
                return toByteArray(value, order)
            }

            is Int -> {
                return toByteArray(value, order)
            }

            is Float -> {
                return toByteArray(value, order)
            }

            is Long -> {
                return toByteArray(value, order)
            }

            is Double -> {
                return toByteArray(value, order)
            }

            else -> {
                throw VlxVmException("Invalid value $value")
            }
        }

    }

}