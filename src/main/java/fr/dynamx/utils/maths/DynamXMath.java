package fr.dynamx.utils.maths;

import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import fr.dynamx.utils.optimization.QuaternionPool;
import fr.dynamx.utils.optimization.Vector3fPool;
import net.minecraft.util.math.MathHelper;

import javax.vecmath.Quat4f;
import javax.vecmath.Vector4f;
import java.nio.FloatBuffer;
import java.util.Arrays;

/**
 * General math and interpolation methods
 *
 * @see DynamXGeometry
 */
public class DynamXMath {
    private static final ThreadLocal<SlerpInterpolation> SLERP_INTERPOLATION = ThreadLocal.withInitial(SlerpInterpolation::new);
    public static final float TO_RADIAN = FastMath.PI / 180.f;

    /**
     * Linear interpolation from startValue to endValue by the given percent.
     * Basically: ((1 - percent) * startValue) + (percent * endValue)
     *
     * @param scale      scale value to use. if 1, use endValue, if 0, use startValue.
     * @param startValue Beginning value. 0% of f
     * @param endValue   ending value. 100% of f
     * @return The interpolated value between startValue and endValue.
     */
    public static float interpolateLinear(float scale, float startValue, float endValue) {
        if (startValue == endValue) {
            return startValue;
        }
        if (scale <= 0f) {
            return startValue;
        }
        if (scale >= 1f) {
            return endValue;
        }
        return ((1f - scale) * startValue) + (scale * endValue);
    }

    /**
     * Linear interpolation from startValue to endValue by the given percent.
     * Basically: ((1 - percent) * startValue) + (percent * endValue)
     *
     * @param scale      scale value to use. if 1, use endValue, if 0, use startValue.
     * @param startValue Beginning value. 0% of f
     * @param endValue   ending value. 100% of f
     * @param store      a vector3f to store the result
     * @return The interpolated value between startValue and endValue.
     */
    public static Vector3f interpolateLinear(float scale, Vector3f startValue, Vector3f endValue, Vector3f store) {
        if (store == null) {
            store = Vector3fPool.get();
        }
        store.x = interpolateLinear(scale, startValue.x, endValue.x);
        store.y = interpolateLinear(scale, startValue.y, endValue.y);
        store.z = interpolateLinear(scale, startValue.z, endValue.z);
        return store;
    }

    public static Vector3f interpolateLinear(float scale, Vector3f startValue, Vector3f endValue) {
        return interpolateLinear(scale, startValue, endValue, null);
    }

    /**
     * Interpolates the given quaternions, without modifying them
     */
    public static Quaternion slerp(float scale, Quaternion startValue, Quaternion endValue) {
        return slerp(scale, startValue, endValue, QuaternionPool.get());
    }

    /**
     * Interpolates the given quaternions, without modifying them <br>
     * Result is stored in non-null store parameter, and returned
     */
    public static Quaternion slerp(float scale, Quaternion startValue, Quaternion endVlue, Quaternion store) {
        SLERP_INTERPOLATION.get().slerp(startValue, endVlue, store, scale);
        return store;
    }

    /**
     * Angles are in degrees
     *
     * @param netAngle    The correct value
     * @param entityAngle The old value
     * @param step        The interpolation step
     * @return And array containing the fixed entityAngle (to have -180<entityAngle-newAngle<180) and the newAngle : (entityAngle + (netAngle - entityAngle) / step), between 180 and -180 degrees
     */
    public static float[] interpolateAngle(float netAngle, float entityAngle, int step) {
        double da = MathHelper.wrapDegrees(netAngle - entityAngle);
        float newAngle = (float) (entityAngle + da / step);
        if (newAngle > 180) {
            newAngle -= 360;
            entityAngle -= 360;
        } else if (newAngle < -180) {
            newAngle += 360;
            entityAngle += 360;
        }
        return new float[]{entityAngle, newAngle};
    }

    /**
     * @param netValue The correct value
     * @param value    The old value
     * @param step     The interpolation step
     * @return (netValue - value) / step
     */
    public static double interpolateDoubleDelta(double netValue, double value, int step) {
        return (netValue - value) / (double) (step);
    }

    /**
     * Take a float input and clamp it between min and max.
     *
     * @param input
     * @param min
     * @param max
     * @return clamped input
     */
    public static float clamp(float input, float min, float max) {
        return (input < min) ? min : Math.min(input, max);
    }

    /**
     * Converts a range of min/max to a 0-1 range.
     *
     * @param value the value between min-max (inclusive).
     * @param min   the minimum of the range.
     * @param max   the maximum of the range.
     * @return A value between 0-1 if the given value is between min/max.
     */
    public static float normalize(float value, float min, float max) {
        return (value - min) / (max - min);
    }

    /**
     * Normalize a value between in range [valueRangeMin, valueRangeMin] valueMin and valueMax
     *
     * @param value         the value between min-max (inclusive).
     * @param valueRangeMin the minimum of the range.
     * @param valueRangeMax the maximum of the range.
     * @param valueMin      the min of the value
     * @param valueMax      the max of the value
     * @return A value between valueMin- valueMax if the given value is between min/max.
     */
    public static float normalizeBetween(float value, float valueRangeMin, float valueRangeMax, float valueMin, float valueMax) {
        return normalize(value, valueRangeMin, valueRangeMax) * (valueMax - valueMin) + valueMin;
    }

    /**
     * Returns 1/sqrt(fValue)
     *
     * @param fValue The value to process.
     * @return 1/sqrt(fValue)
     * @see java.lang.Math#sqrt(double)
     */
    public static float invSqrt(float fValue) {
        return (float) (1.0f / Math.sqrt(fValue));
    }

    public static float getMin(float a, float b, float c, float d) {
        return Math.min(Math.min(a, b), Math.min(c, d));
    }

    public static float getMax(float a, float b, float c, float d) {
        return Math.max(Math.max(a, b), Math.max(c, d));
    }

    public static double getMin(double a, double b, double c, double d, double e, double f) {
        return Math.min(Math.min(Math.min(a, b), Math.min(c, d)), Math.min(e, f));
    }

    public static double getMax(double a, double b, double c, double d, double e, double f) {
        return Math.max(Math.max(Math.max(a, b), Math.max(c, d)), Math.max(e, f));
    }

    /**
     * @return The closest int near to of
     */
    public static int preciseRound(double of) {
        int c = (int) of;
        return of - c > 0.5 ? c + 1 : of - c < -0.5 ? c - 1 : c;
    }

    public static float roundFloat(float f, float precision) {
        return (float) Math.round(f * precision) / precision;
    }

    private static void invertMat4(float[] matOut, float[] matIn) {
        matOut[0] = matIn[5] * matIn[10] * matIn[15] - matIn[5] * matIn[11] * matIn[14] - matIn[9] * matIn[6] * matIn[15] + matIn[9] * matIn[7] * matIn[14] + matIn[13] * matIn[6] * matIn[11] - matIn[13] * matIn[7] * matIn[10];
        matOut[1] = -matIn[1] * matIn[10] * matIn[15] + matIn[1] * matIn[11] * matIn[14] + matIn[9] * matIn[2] * matIn[15] - matIn[9] * matIn[3] * matIn[14] - matIn[13] * matIn[2] * matIn[11] + matIn[13] * matIn[3] * matIn[10];
        matOut[2] = matIn[1] * matIn[6] * matIn[15] - matIn[1] * matIn[7] * matIn[14] - matIn[5] * matIn[2] * matIn[15] + matIn[5] * matIn[3] * matIn[14] + matIn[13] * matIn[2] * matIn[7] - matIn[13] * matIn[3] * matIn[6];
        matOut[3] = -matIn[1] * matIn[6] * matIn[11] + matIn[1] * matIn[7] * matIn[10] + matIn[5] * matIn[2] * matIn[11] - matIn[5] * matIn[3] * matIn[10] - matIn[9] * matIn[2] * matIn[7] + matIn[9] * matIn[3] * matIn[6];
        matOut[4] = -matIn[4] * matIn[10] * matIn[15] + matIn[4] * matIn[11] * matIn[14] + matIn[8] * matIn[6] * matIn[15] - matIn[8] * matIn[7] * matIn[14] - matIn[12] * matIn[6] * matIn[11] + matIn[12] * matIn[7] * matIn[10];
        matOut[5] = matIn[0] * matIn[10] * matIn[15] - matIn[0] * matIn[11] * matIn[14] - matIn[8] * matIn[2] * matIn[15] + matIn[8] * matIn[3] * matIn[14] + matIn[12] * matIn[2] * matIn[11] - matIn[12] * matIn[3] * matIn[10];
        matOut[6] = -matIn[0] * matIn[6] * matIn[15] + matIn[0] * matIn[7] * matIn[14] + matIn[4] * matIn[2] * matIn[15] - matIn[4] * matIn[3] * matIn[14] - matIn[12] * matIn[2] * matIn[7] + matIn[12] * matIn[3] * matIn[6];
        matOut[7] = matIn[0] * matIn[6] * matIn[11] - matIn[0] * matIn[7] * matIn[10] - matIn[4] * matIn[2] * matIn[11] + matIn[4] * matIn[3] * matIn[10] + matIn[8] * matIn[2] * matIn[7] - matIn[8] * matIn[3] * matIn[6];
        matOut[8] = matIn[4] * matIn[9] * matIn[15] - matIn[4] * matIn[11] * matIn[13] - matIn[8] * matIn[5] * matIn[15] + matIn[8] * matIn[7] * matIn[13] + matIn[12] * matIn[5] * matIn[11] - matIn[12] * matIn[7] * matIn[9];
        matOut[9] = -matIn[0] * matIn[9] * matIn[15] + matIn[0] * matIn[11] * matIn[13] + matIn[8] * matIn[1] * matIn[15] - matIn[8] * matIn[3] * matIn[13] - matIn[12] * matIn[1] * matIn[11] + matIn[12] * matIn[3] * matIn[9];
        matOut[10] = matIn[0] * matIn[5] * matIn[15] - matIn[0] * matIn[7] * matIn[13] - matIn[4] * matIn[1] * matIn[15] + matIn[4] * matIn[3] * matIn[13] + matIn[12] * matIn[1] * matIn[7] - matIn[12] * matIn[3] * matIn[5];
        matOut[11] = -matIn[0] * matIn[5] * matIn[11] + matIn[0] * matIn[7] * matIn[9] + matIn[4] * matIn[1] * matIn[11] - matIn[4] * matIn[3] * matIn[9] - matIn[8] * matIn[1] * matIn[7] + matIn[8] * matIn[3] * matIn[5];
        matOut[12] = -matIn[4] * matIn[9] * matIn[14] + matIn[4] * matIn[10] * matIn[13] + matIn[8] * matIn[5] * matIn[14] - matIn[8] * matIn[6] * matIn[13] - matIn[12] * matIn[5] * matIn[10] + matIn[12] * matIn[6] * matIn[9];
        matOut[13] = matIn[0] * matIn[9] * matIn[14] - matIn[0] * matIn[10] * matIn[13] - matIn[8] * matIn[1] * matIn[14] + matIn[8] * matIn[2] * matIn[13] + matIn[12] * matIn[1] * matIn[10] - matIn[12] * matIn[2] * matIn[9];
        matOut[14] = -matIn[0] * matIn[5] * matIn[14] + matIn[0] * matIn[6] * matIn[13] + matIn[4] * matIn[1] * matIn[14] - matIn[4] * matIn[2] * matIn[13] - matIn[12] * matIn[1] * matIn[6] + matIn[12] * matIn[2] * matIn[5];
        matOut[15] = matIn[0] * matIn[5] * matIn[10] - matIn[0] * matIn[6] * matIn[9] - matIn[4] * matIn[1] * matIn[10] + matIn[4] * matIn[2] * matIn[9] + matIn[8] * matIn[1] * matIn[6] - matIn[8] * matIn[2] * matIn[5];
        float det = matIn[0] * matOut[0] + matIn[1] * matOut[4] + matIn[2] * matOut[8] + matIn[3] * matOut[12];
        if (det != 0.0) {
            for (int i = 0; i < 16; i++)
                matOut[i] = matOut[i] / det;
        } else {
            Arrays.fill(matOut, 0.0F);
        }
    }

    /**
     *  fbMatIn =  faMat
     *  faInv = inverse(faMat)
     *  fbInvOut = faInv
     */
    public static void invertMat4(FloatBuffer fbInvOut, FloatBuffer fbMatIn, float[] faInv, float[] faMat) {
        fbMatIn.get(faMat);
        invertMat4(faInv, faMat);
        fbInvOut.put(faInv);
    }

    public static void multiplyMat4xVec3(Vector4f vecOut, float[] matA, Vector4f vecB) {
        float x,y,z, w;
        x = matA[0] * vecB.x + matA[4] * vecB.y + matA[8] * vecB.z + matA[12] * vecB.w;
        y = matA[1] * vecB.x + matA[5] * vecB.y + matA[9] * vecB.z + matA[13] * vecB.w;
        z = matA[2] * vecB.x + matA[6] * vecB.y + matA[10] * vecB.z + matA[14] * vecB.w;
        w = matA[3] * vecB.x + matA[7] * vecB.y + matA[11] * vecB.z + matA[15] * vecB.w;
        vecOut.set(x,y,z,w);
    }

    /**
     * A thread-safe slerp interpolation
     */
    private static class SlerpInterpolation {
        private final Quat4f v0 = new Quat4f(), v1 = new Quat4f();
        private volatile boolean slerping;

        private void slerp(Quaternion v0b, Quaternion v1b, Quaternion store, float t) {
            v0.set(v0b.getX(), v0b.getY(), v0b.getZ(), v0b.getW());
            v1.set(v1b.getX(), v1b.getY(), v1b.getZ(), v1b.getW());
            v0.interpolate(v1, t);
            store.set(v0.x, v0.y, v0.z, v0.w);
        }
    }
}
