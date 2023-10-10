package edu.illinois.confuzz.internal;

public class LogUtils {
    public static boolean print = Boolean.getBoolean("confuzz.log");

    public static void println(String msg) {
        if (print) {
            System.out.println(msg);
        }
    }
}
