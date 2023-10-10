package edu.illinois.confuzz.internal;

import java.io.Serializable;

public class Utils {

    public static boolean onlyCheckDefault() {
        return Boolean.getBoolean("onlyCheckDefault");
    }

    public static void setMavenGoal(String goal) {
        System.setProperty("confuzz.goal", goal);
    }

    public static String getMavenGoal() {
        return System.getProperty("confuzz.goal");
    }

    public static boolean isCoverageGoal() {
        return getMavenGoal().equals("coverage");
    }

    public static boolean isFuzzGoal() {
        return getMavenGoal().equals("fuzz");
    }

    public static boolean isDebugGoal() {
        return getMavenGoal().equals("debug");
    }

    public static boolean checkObjectSerilizable(Object obj) {
        if (obj == null) {
            return false;
        }
        return obj instanceof Serializable;
    }
}
