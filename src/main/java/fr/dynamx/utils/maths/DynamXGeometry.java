package fr.dynamx.utils.maths;

import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import fr.dynamx.common.DynamXMain;
import fr.dynamx.utils.optimization.QuaternionPool;
import fr.dynamx.utils.optimization.Vector3fPool;
import fr.dynamx.utils.physics.DynamXPhysicsHelper;

/**
 * General geometrical operations with Vector3f and Quaternions
 *
 * @see DynamXMath
 * @see DynamXPhysicsHelper
 * @see fr.dynamx.utils.client.ClientDynamXUtils
 */
public class DynamXGeometry {

    public final static Vector3f FORWARD_DIRECTION = new Vector3f(0.0f,0.0f,1.0f);
    public final static Vector3f LEFT_DIRECTION = new Vector3f(1.0f,0.0f,0.0f);
    public final static Vector3f UP_DIRECTION = new Vector3f(0.0f,1.0f,0.0f);

    /**
     * Returns the angle in radians between this vector and the vector
     * parameter; the return value is constrained to the range [0,PI].
     *
     * @param v1 the other vector
     * @return the angle in radians in the range [0,PI]
     */
    public static float angle(Vector3f t, Vector3f v1) {
        double vDot = t.dot(v1) / (t.length() * v1.length());
        if (vDot < -1.0) vDot = -1.0;
        if (vDot > 1.0) vDot = 1.0;
        return ((float) (Math.acos(vDot)));
    }

    /**
     * @param pos The pos to rotate, not modified
     * @return The rotated pos, this Vector3f is got from Vector3fPool, so only valid in the current sub-pool (generally this tick)
     */
    public static Vector3f getRotatedPoint(Vector3f pos, float pitch, float yaw, float roll) {
        double a, b, c, d, e, f; //thread-safe
        float x, y, z;

        a = Math.cos(pitch * 0.017453292F);//A
        b = Math.sin(pitch * 0.017453292F);//B
        c = Math.cos(yaw * 0.017453292F);//C
        d = Math.sin(yaw * 0.017453292F);//D
        e = Math.cos(roll * 0.017453292F);//E
        f = Math.sin(roll * 0.017453292F);//F

        x = (float) (pos.x * (c * e - b * d * f) + pos.y * (-b * d * e - c * f) + pos.z * (-a * d));
        y = (float) (pos.x * (a * f) + pos.y * (a * e) + pos.z * (-b));
        z = (float) (pos.x * (d * e + b * c * f) + pos.y * (b * c * e - d * f) + pos.z * (a * c));

        return Vector3fPool.get(x, y, z);
    }

    /**
     * @param pos The pos to rotate, not modified
     * @return The rotated pos, creates a new object
     */
    public static double[] getRotatedPoint(double[] pos, double pitch, double yaw, double roll) {
        double a, b, c, d, e, f; //thread-safe
        double x, y, z;

        a = Math.cos(pitch * Math.PI / 180);//A
        b = Math.sin(pitch * Math.PI / 180);//B
        c = Math.cos(yaw * Math.PI / 180);//C
        d = Math.sin(yaw * Math.PI / 180);//D
        e = Math.cos(roll * Math.PI / 180);//E
        f = Math.sin(roll * Math.PI / 180);//F

        x = (pos[0] * (c * e - b * d * f) + pos[1] * (-b * d * e - c * f) + pos[2] * (-a * d));
        y = (pos[0] * (a * f) + pos[1] * (a * e) + pos[2] * (-b));
        z = (pos[0] * (d * e + b * c * f) + pos[1] * (b * c * e - d * f) + pos[2] * (a * c));

        return new double[]{x, y, z};
    }

    public static Vector3f toAngles(Quaternion quat) {
        Vector3f euler = new Vector3f();

        float sqw = quat.getW() * quat.getW();
        float sqx = quat.getX() * quat.getX();
        float sqy = quat.getY() * quat.getY();
        float sqz = quat.getZ() * quat.getZ();
        float unit = sqx + sqy + sqz + sqw; // if normalized is one, otherwise
        // is correction factor
        float test = quat.getX() * quat.getY() + quat.getZ() * quat.getW();
        if (test > 0.499 * unit) { // singularity at north pole
            euler.y = 2 * FastMath.atan2(quat.getX(), quat.getW());
            euler.z = FastMath.HALF_PI;
            euler.x = 0;
        } else if (test < -0.499 * unit) { // singularity at south pole
            euler.y = -2 * FastMath.atan2(quat.getX(), quat.getW());
            euler.z = -FastMath.HALF_PI;
            euler.x = 0;
        } else {
            euler.y = FastMath.atan2(2 * quat.getY() * quat.getW() - 2 * quat.getX() * quat.getZ(), sqx - sqy - sqz + sqw); // roll or heading
            euler.z = (float) Math.asin(2 * test / unit); // pitch or attitude
            euler.x = FastMath.atan2(2 * quat.getX() * quat.getW() - 2 * quat.getY() * quat.getZ(), -sqx + sqy - sqz + sqw); // yaw or bank
        }
        return euler;
    }

    public static final double SINGULARITY_NORTH_POLE = 0.49999;
    public static final double SINGULARITY_SOUTH_POLE = -0.49999;

    public static Vector3f quaternionToEuler(Quaternion quat) {
        return new Vector3f(toYaw(quat), toPitch(quat), toRoll(quat));
    }


    private static float toRoll(Quaternion quat) {
        // This is a test for singularities
        double test = quat.getX() * quat.getY() + quat.getZ() * quat.getW();

        // Special case for north pole
        if (test > SINGULARITY_NORTH_POLE)
            return 0;

        // Special case for south pole
        if (test < SINGULARITY_SOUTH_POLE)
            return 0;

        return (float) Math.atan2(
                2 * quat.getX() * quat.getW() - 2 * quat.getY() * quat.getZ(),
                1 - 2 * quat.getX() * quat.getX() - 2 * quat.getZ() * quat.getZ()
        );
    }

    private static float toPitch(Quaternion quat) {
        // This is a test for singularities
        double test = quat.getX() * quat.getY() + quat.getZ() * quat.getW();

        // Special case for north pole
        if (test > SINGULARITY_NORTH_POLE)
            return (float) (Math.PI / 2);

        // Special case for south pole
        if (test < SINGULARITY_SOUTH_POLE)
            return (float) (-Math.PI / 2);

        return (float) Math.asin(2 * test);
    }

    private static float toYaw(Quaternion quat) {
        // This is a test for singularities
        double test = quat.getX() * quat.getY() + quat.getZ() * quat.getW();

        // Special case for north pole
        if (test > SINGULARITY_NORTH_POLE)
            return (float) (2 * Math.atan2(quat.getX(), quat.getW()));

        // Special case for south pole
        if (test < SINGULARITY_SOUTH_POLE)
            return (float) (-2 * Math.atan2(quat.getX(), quat.getW()));

        return (float) Math.atan2(
                2 * quat.getY() * quat.getW() - 2 * quat.getX() * quat.getZ(),
                1 - 2 * quat.getY() * quat.getY() - 2 * quat.getZ() * quat.getZ()
        );

    }

    /**
     * The given Quaternion is returned from {@link QuaternionPool} so make sure to open, and then close, the pool
     *
     * @param horizontalDir I think it's the horizontal enum facing index
     */
    public static Quaternion rotationYawToQuaternion(int horizontalDir) {
        double pitch = horizontalDir * -22.5f * Math.PI / 180f;
        // Thanks to wikipedia
        double cp = Math.cos(pitch * 0.5);
        double sp = Math.sin(pitch * 0.5);

        float w = (float) (cp);
        float x = 0f;
        float y = (float) (sp);
        float z = 0f;
        return QuaternionPool.get(x, y, z, w);
    }

    /**
     * Rotates a vector with the given quaternion
     *
     * @param v The vector to rotate, not modified
     * @param q The rotation
     * @return The rotated vector, this Vector3f is got from Vector3fPool, so only valid in the current sub-pool (generally this tick)
     */
    public static Vector3f rotateVectorByQuaternion(Vector3f v, Quaternion q) {
        // Extract the vector part of the quaternion
        Vector3f u = Vector3fPool.get(q.getX(), q.getY(), q.getZ());

        // Extract the scalar part of the quaternion
        float s = q.getW();

        // Do the math
        Vector3f v1 = Vector3fPool.get(u);
        v1.multLocal(2 * u.dot(v));
        Vector3f v2 = Vector3fPool.get(v);
        v2.multLocal(s * s - u.dot(u));
        v1.addLocal(v2);
        v2.set(u.cross(v, v2));
        v1.addLocal(v2.multLocal(2 * s));
        /*vprime = u.mult(u.dot(v) * 2)
                + (s*s - dot(u, u)) * v
                + 2.0f * s * cross(u, v);*/
        return v1;
    }

    /**
     * Computes a quaternion from the given rotation yaw
     *
     * @param yawDeg The yaw in degrees
     * @return The corresponding Quaternion, from a QuaternionPool
     */
    public static Quaternion rotationYawToQuaternion(float yawDeg) {
        return QuaternionPool.get().fromAngleNormalAxis((float) Math.toRadians(-yawDeg), new Vector3f(0, 1, 0));
    }

    /**
     * Computes a quaternion from the given euler rotation angles
     *
     * @param roll  rotation on Z axis in degrees
     * @param yaw   rotation on Y axis in degrees
     * @param pitch rotation on X axis in degrees
     * @return A new quaternion from the given euler angles
     */
    public static Quaternion eulerToQuaternion(float roll, float yaw, float pitch) {
        return QuaternionPool.get().fromAngles((float) Math.toRadians(-pitch), (float) Math.toRadians(-yaw), (float) Math.toRadians(-roll));
    }

    /**
     * Euclidean distance between this and the specified vector, returned as double
     */
    public static double distanceBetween(Vector3f vec, Vector3f vec2) {
        double d0 = vec2.x - vec.x;
        double d1 = vec2.y - vec.y;
        double d2 = vec2.z - vec.z;
        return Math.sqrt(d0 * d0 + d1 * d1 + d2 * d2);
    }

    /**
     * Normalizes the input, doesn't create a new one
     */
    public static void normalizeVector(Vector3f vecToNormalize) {
        float length = vecToNormalize.length();
        if (length != 1f && length != 0f) {
            length = 1.0f / FastMath.sqrt(length);
            vecToNormalize.set(vecToNormalize.x * length, vecToNormalize.y * length, vecToNormalize.z * length);
        }
    }

    public static Quaternion inverseQuaternion(Quaternion quat, Quaternion result) {
        if(result == null){
            result = new Quaternion();
        }
        float norm = quat.norm();
        if (norm > 0.0) {
            float invNorm = 1.0f / norm;
            result.set(-quat.getX() * invNorm, -quat.getY() * invNorm, -quat.getZ() * invNorm, quat.getW() * invNorm);
        }
        return result;
        // error
    }

    public static Vector3f getCenter(Vector3f p1, Vector3f p2, Vector3f p3, Vector3f p4) {
        Vector3f min = Vector3fPool.get(DynamXMath.getMin(p1.x, p2.x, p3.x, p4.x), DynamXMath.getMin(p1.y, p2.y, p3.y, p4.y), DynamXMath.getMin(p1.z, p2.z, p3.z, p4.z));
        return Vector3fPool.get((DynamXMath.getMax(p1.x, p2.x, p3.x, p4.x) - min.x) / 2, (DynamXMath.getMax(p1.y, p2.y, p3.y, p4.y) - min.y) / 2, (DynamXMath.getMax(p1.z, p2.z, p3.z, p4.z) - min.z) / 2).addLocal(min);
    }

    /**
     * <code>getRotationColumn</code> returns one of three columns specified
     * by the parameter. This column is returned as a <code>Vector3f</code>
     * object.
     *
     * @param i the column to retrieve. Must be between 0 and 2.
     * @return the column specified by the index.
     */
    public static Vector3f getRotationColumn(Quaternion quaternion, int i) {
        return getRotationColumn(quaternion, i, null);
    }

    /**
     * <code>getRotationColumn</code> returns one of three columns specified
     * by the parameter. This column is returned as a <code>Vector3f</code>
     * object. The value is retrieved as if this quaternion was first normalized.
     *
     * @param i     the column to retrieve. Must be between 0 and 2.
     * @param store the vector object to store the result in. if null, a new one
     *              is created.
     * @return the column specified by the index.
     */
    public static Vector3f getRotationColumn(Quaternion quaternion, int i, Vector3f store) {
        if (store == null) {
            store = Vector3fPool.get();
        }

        float norm = quaternion.norm();
        if (norm != 1.0f) {
            norm = DynamXMath.invSqrt(norm);
        }

        float xx = quaternion.getX() * quaternion.getX() * norm;
        float xy = quaternion.getX() * quaternion.getY() * norm;
        float xz = quaternion.getX() * quaternion.getZ() * norm;
        float xw = quaternion.getX() * quaternion.getW() * norm;
        float yy = quaternion.getY() * quaternion.getY() * norm;
        float yz = quaternion.getY() * quaternion.getZ() * norm;
        float yw = quaternion.getY() * quaternion.getW() * norm;
        float zz = quaternion.getZ() * quaternion.getZ() * norm;
        float zw = quaternion.getZ() * quaternion.getW() * norm;

        switch (i) {
            case 0:
                store.x = 1 - 2 * (yy + zz);
                store.y = 2 * (xy + zw);
                store.z = 2 * (xz - yw);
                break;
            case 1:
                store.x = 2 * (xy - zw);
                store.y = 1 - 2 * (xx + zz);
                store.z = 2 * (yz + xw);
                break;
            case 2:
                store.x = 2 * (xz + yw);
                store.y = 2 * (yz - xw);
                store.z = 1 - 2 * (xx + yy);
                break;
            default:
                DynamXMain.log.warn("Invalid column index.");
                throw new IllegalArgumentException("Invalid column index. " + i);
        }

        return store;
    }

    public static float getYawFromRotationVector(Vector3f forwardRotationVector) {
        Vector3f horizontalRotationVec = Vector3fPool.get(forwardRotationVector.x, 0, forwardRotationVector.z);
        Vector3f tmpVector = LEFT_DIRECTION.clone();
        float yaw = (horizontalRotationVec.length() == 0 ? 0 : DynamXGeometry.angle(tmpVector, horizontalRotationVec) + FastMath.HALF_PI);

        tmpVector.cross(horizontalRotationVec, tmpVector);

        if (tmpVector.y < 0)
            yaw = FastMath.PI - yaw;

        return (float) Math.toDegrees(-yaw);
    }

    public static float getPitchFromRotationVector(Vector3f forwardRotationVector) {
        return (float) Math.toDegrees(FastMath.HALF_PI - DynamXGeometry.angle(UP_DIRECTION, forwardRotationVector));
    }

    public static float getRollFromRotationVector(Vector3f leftRotationVector, Vector3f forwardRotationVector) {
        Vector3f tmpVector = Vector3fPool.get();
        UP_DIRECTION.cross(forwardRotationVector, tmpVector);
        float roll = -DynamXGeometry.angle(tmpVector, leftRotationVector);

        if (leftRotationVector.y > 0)
            roll = -roll;

        return (float) Math.toDegrees(roll);
    }

    /**
     * Convert the specified vector from Cartesian coordinates to spherical
     * coordinates (r, theta, phi) per ISO 80000.
     * <p>
     * In particular:
     * <ul>
     * <li>{@code r} is a distance measured from the origin. It ranges from 0 to
     * infinity and is stored in the 1st (X) vector component.
     *
     * <li>{@code theta} is the polar angle, measured (in radians) from the +Z
     * axis. It ranges from 0 to PI and is stored in the 2nd (Y) vector
     * component.
     *
     * <li>{@code phi} is the azimuthal angle, measured (in radians) from the +X
     * axis to the projection of the vector onto the X-Y plane. It ranges from
     * -PI to PI and is stored in the 3rd (Z) vector component.
     * </ul>
     *
     * @param vec the vector to convert (not null, modified)
     */
    public static void toSpherical(Vector3f vec) {
        double xx = vec.x;
        double yy = vec.y;
        float rxy = (float) Math.sqrt(xx * xx + yy * yy); // MyMath
        float phi = FastMath.atan2(rxy, vec.z);
        float theta = FastMath.atan2(vec.y, vec.x);
        float r = vec.length();

        vec.x = r;
        vec.y = theta; // polar angle
        vec.z = phi;   // azimuthal angle
    }

}