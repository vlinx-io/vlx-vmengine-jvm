package io.vlinx.vmengine


/**
 * @author vlinx <vlinx@vlinx.io>
 * @create 2023-03-04
 * @version 1.0.0
 */

class InstanceToCreate(val clazz: Class<*>) {

    override fun toString(): String {
        return "InstanceToCreate(clazz=$clazz)"
    }
}