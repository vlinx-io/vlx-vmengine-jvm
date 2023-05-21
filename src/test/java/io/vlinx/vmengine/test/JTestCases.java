package io.vlinx.vmengine.test;

import org.apache.commons.lang3.SystemUtils;
import org.junit.jupiter.api.Test;


public class JTestCases {


    public static String HOME = System.getProperty("user.home");

    static {
        if (SystemUtils.IS_OS_WINDOWS) {
            HOME = "d:/";
        }
    }


    @Test
    public void testClass() {
        System.out.println(java.lang.Long.class);
        System.out.println(long.class);
    }

    @Test
    public void testCast() {
        Object i = 123;
        String str = (String) i;

        byte[] arr = new byte[]{1, 2, 3};
    }

    @Test
    public void testInstanceOf() {
        Object i = 123;
        System.out.println((i instanceof java.lang.Integer));
    }

    @Test
    public void testArr() {
        int[] a = new int[3];
        System.out.println(a.getClass().getName());
        String str = "hello";
        System.out.println(str.getClass().getName());
    }

}
