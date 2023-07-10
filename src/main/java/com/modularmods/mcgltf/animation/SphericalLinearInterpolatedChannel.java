package com.modularmods.mcgltf.animation;

public abstract class SphericalLinearInterpolatedChannel extends InterpolatedChannel {

    /**
     * The values. Each element of this array corresponds to one key
     * frame time
     */
    protected final float[][] values;

    public SphericalLinearInterpolatedChannel(float[] timesS, float[][] values) {
        super(timesS);
        this.values = values;
    }

    @Override
    public void update(float timeS) {
        float[] output = getListener();
        if (timeS <= timesS[0]) {
            System.arraycopy(values[0], 0, output, 0, output.length);
        } else if (timeS >= timesS[timesS.length - 1]) {
            System.arraycopy(values[timesS.length - 1], 0, output, 0, output.length);
        } else {
            int previousIndex = computeIndex(timeS, timesS);
            int nextIndex = previousIndex + 1;

            if (nextIndex >= timesS.length) {
                nextIndex = timesS.length - 1;
            }

            float local = timeS - timesS[previousIndex];
            float delta = timesS[nextIndex] - timesS[previousIndex];
            float alpha = local / delta;

            float[] previousPoint = values[previousIndex];
            float[] nextPoint = values[nextIndex];

            // Adapted from javax.vecmath.Quat4f
            float ax = previousPoint[0];
            float ay = previousPoint[1];
            float az = previousPoint[2];
            float aw = previousPoint[3];
            float bx = nextPoint[0];
            float by = nextPoint[1];
            float bz = nextPoint[2];
            float bw = nextPoint[3];

            float dot = ax * bx + ay * by + az * bz + aw * bw;
            if (dot < 0) {
                bx = -bx;
                by = -by;
                bz = -bz;
                bw = -bw;
                dot = -dot;
            }
            float epsilon = 1e-6f;
            float s0, s1;
            if ((1.0 - dot) > epsilon) {
                float omega = (float) Math.acos(dot);
                float invSinOmega = 1.0f / (float) Math.sin(omega);
                s0 = (float) Math.sin((1.0 - alpha) * omega) * invSinOmega;
                s1 = (float) Math.sin(alpha * omega) * invSinOmega;
            } else {
                s0 = 1.0f - alpha;
                s1 = alpha;
            }
            float rx = s0 * ax + s1 * bx;
            float ry = s0 * ay + s1 * by;
            float rz = s0 * az + s1 * bz;
            float rw = s0 * aw + s1 * bw;

            output[0] = rx;
            output[1] = ry;
            output[2] = rz;
            output[3] = rw;
        }
    }

}
