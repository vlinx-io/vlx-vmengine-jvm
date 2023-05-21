package io.vlinx.image.analyzer.util

import io.vlinx.vmengine.util.ByteUtils
import java.nio.ByteOrder


/**
 * @author vlinx <vlinx@vlinx.io>
 * @create 2022-04-10
 * @version 1.0.0
 */

object LEByteUtils {

    fun toShort(array: ByteArray): Short {
        return ByteUtils.toShort(array, ByteOrder.LITTLE_ENDIAN)
    }

    fun toInt(array: ByteArray): Int {
        return ByteUtils.toInt(array, ByteOrder.LITTLE_ENDIAN)
    }

    fun toLong(array: ByteArray): Long {
        return ByteUtils.toLong(array, ByteOrder.LITTLE_ENDIAN)
    }

    fun toFloat(array: ByteArray): Float {
        return ByteUtils.toFloat(array, ByteOrder.LITTLE_ENDIAN)
    }

    fun toDouble(array: ByteArray): Double {
        return ByteUtils.toDouble(array, ByteOrder.LITTLE_ENDIAN)
    }

}