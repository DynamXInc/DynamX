package fr.dynamx.core.utils.debug;

public class SyncHelper {
    public static float EPS = 0.00035f;

    public static boolean different(float f1, float f2) {
        return different(f1, f2, EPS);
    }

    public static boolean different(float f1, float f2, float epsilon) {
        return Math.abs(f1 - f2) > epsilon;
    }
}
