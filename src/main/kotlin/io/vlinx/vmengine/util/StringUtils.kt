package io.vlinx.vmengine.util

object StringUtils {

    // TODO 比较与验证这里的escape
    fun escape(str: String): String? {
        val len = str.length
        val buf = StringBuilder(len + 5)
        val ch = str.toCharArray()
        for (i in 0 until len) {
            when (ch[i]) {
                '\n' -> buf.append("\\n")
                '\r' -> buf.append("\\r")
                '\t' -> buf.append("\\t")
                '\b' -> buf.append("\\b")
                '"' -> buf.append("\\\"")
                else -> buf.append(ch[i])
            }
        }
        return buf.toString()
    }

}