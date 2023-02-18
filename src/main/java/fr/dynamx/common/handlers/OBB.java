package fr.dynamx.common.handlers;

import com.jme3.bullet.collision.PhysicsCollisionObject;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import fr.dynamx.utils.optimization.Vector3fPool;

import java.util.List;

public class OBB {
    private Vector3f center;
    private Vector3f[] axes;
    private float[] extents;

    public OBB(Vector3f center, Vector3f size, Quaternion rotation) {
        this.center = center;
        this.axes = new Vector3f[3];
        this.extents = new float[3];
        // Calculate the three axes of the OBB
        axes[0] = rotation.mult(Vector3f.UNIT_X, Vector3fPool.get()).normalize();
        axes[1] = rotation.mult(Vector3f.UNIT_Y, Vector3fPool.get()).normalize();
        axes[2] = rotation.mult(Vector3f.UNIT_Z, Vector3fPool.get()).normalize();

        // Calculate the extents of the OBB
        extents[0] = size.x / 2;
        extents[1] = size.y / 2;
        extents[2] = size.z / 2;

        
    }

    public Vector3f getCenter() {
        return center;
    }

    public Vector3f[] getAxes() {
        return axes;
    }

    public float[] getExtents() {
        return extents;
    }

    public static class CollisionDetector {
        public static Vector3f AABBMotionWithOBBs(Vector3f motion, Vector3f min, Vector3f max, List<OBB> obbs) {
            Vector3f result = motion.clone();
            for (OBB obb : obbs) {
                Vector3f separation = getSeparation(min, max, obb);
                if (separation.dot(motion) < 0) {
                    result = handleCollision(motion, separation);
                }
            }
            return result;
        }

        private static Vector3f handleCollision(Vector3f motion, Vector3f separation) {
            float projectMotion = motion.dot(separation.normalize());
            return motion.subtract(separation.normalize().mult(projectMotion));
        }

        private static Vector3f getSeparation(Vector3f min, Vector3f max, OBB obb) {
            Vector3f[] axes = obb.getAxes();
            float[] extents = obb.getExtents();
            Vector3f center = obb.getCenter();
            Vector3f[] points = new Vector3f[8];
            points[0] = center.add(axes[0].mult(extents[0])).add(axes[1].mult(extents[1])).add(axes[2].mult(extents[2]));
            points[1] = center.add(axes[0].mult(extents[0])).add(axes[1].mult(extents[1])).subtract(axes[2].mult(extents[2]));
            points[2] = center.add(axes[0].mult(extents[0])).subtract(axes[1].mult(extents[1])).add(axes[2].mult(extents[2]));
            points[3] = center.add(axes[0].mult(extents[0])).subtract(axes[1].mult(extents[1])).subtract(axes[2].mult(extents[2]));
            points[4] = center.subtract(axes[0].mult(extents[0])).add(axes[1].mult(extents[1])).add(axes[2].mult(extents[2]));
            points[5] = center.subtract(axes[0].mult(extents[0])).add(axes[1].mult(extents[1])).subtract(axes[2].mult(extents[2]));
            points[6] = center.subtract(axes[0].mult(extents[0])).subtract(axes[1].mult(extents[1])).add(axes[2].mult(extents[2]));
            points[7] = center.subtract(axes[0].mult(extents[0])).subtract(axes[1].mult(extents[1])).subtract(axes[2].mult(extents[2]));

            Vector3f[] normals = new Vector3f[]{axes[0], axes[1], axes[2],
                    axes[0].cross(axes[1]), axes[1].cross(axes[2]), axes[2].cross(axes[0])};
            Vector3f minSeparation = null;
            float minDistance = Float.MAX_VALUE;
            for (Vector3f normal : normals) {
                float[] a = project(normal, points);
                float b0 = normal.dot(min);
                float b1 = normal.dot(max);
                float distance = Math.max(a[0] - b1, b0 - a[1]);
                if (distance > 0) {
                    return new Vector3f();
                } else if (-distance < minDistance) {
                    minDistance = -distance;
                    minSeparation = normal.mult(distance);
                }
            }
            return minSeparation;
        }

        private static float[] project(Vector3f normal, Vector3f[] points) {
            float min = normal.dot(points[0]);
            float max = min;
            for (int i = 1; i < points.length; i++) {
                float value = normal.dot(points[i]);
                min = Math.min(min, value);
                max = Math.max(max, value);
            }
            return new float[]{min, max};
        }
    }
}
