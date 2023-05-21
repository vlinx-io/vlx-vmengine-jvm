package io.vlinx.image.analyzer.util

import io.vlinx.vmengine.VlxVmException
import org.apache.commons.lang3.SystemUtils

object SystemInfo {

    fun getOSFamily(): String {

        return if (SystemUtils.IS_OS_MAC) {
            "darwin"
        } else if (SystemUtils.IS_OS_LINUX) {
            "linux"
        } else if (SystemUtils.IS_OS_WINDOWS) {
            "windows"
        } else {
            throw VlxVmException("Unsupported OS: ${SystemUtils.OS_NAME}")
        }

    }


}