package io.vlinx.vmengine;

import io.vlinx.logging.Logger;

import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

public class VMEngine {

    HashMap<Object, ReentrantLock> locks = new HashMap();
    public static VMEngine instance = new VMEngine();


}
