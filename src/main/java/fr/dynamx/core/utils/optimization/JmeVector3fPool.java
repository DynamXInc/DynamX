package fr.dynamx.core.utils.optimization;

import com.jme3.math.Vector3f;
import net.minecraft.util.math.Vec3d;

import javax.annotation.concurrent.ThreadSafe;

/**
 * Stores different Vector3fPool for each thread, referenced by their id (named "instance" here)
 */
@ThreadSafe
public class JmeVector3fPool extends ClassPool<Vector3f> {
    private static final ThreadLocal<JmeVector3fPool> LOCAL_POOL = ThreadLocal.withInitial(JmeVector3fPool::new);

    //  private final Map<StackTraceElement, Integer> callers = new HashMap<>();

    public static JmeVector3fPool getPool() {
        return LOCAL_POOL.get();
    }

   /* @Override
    public Vector3f provideNewInstance() {
        StackTraceElement[] st = Thread.currentThread().getStackTrace();
        callers.put(st[3], callers.getOrDefault(st[3], 0)+1);
        return super.provideNewInstance();
    }*/

    /**
     * @return A <strong>new</strong> vector initialized with from's data
     */
    public static Vector3f getPermanentVector(Vector3f from) {
        return new Vector3f(from.x, from.y, from.z);
    }

    public static void openPool() {
        getPool().openSubPool(SubClassPool.VECTOR3F_DEFAULT);
    }

    public static void openPool(String identifier) {
        getPool().openSubPool(identifier);

        Vector3fPool.openPool(identifier); //TODO SPARADRAP TO REMOVE
    }

    public static void closePool() {
        getPool().closeSubPool();

        Vector3fPool.closePool(); //TODO SPARADRAP TO REMOVE
    }

    public static Vector3f get() {
        return get(0, 0, 0);
    }

    public static Vector3f get(Vector3f from) {
        Vector3f v = getPool().provideNewInstance();
        v.set(from);
        return v;
    }

    public static Vector3f get(org.joml.Vector3f from) {
        Vector3f v = getPool().provideNewInstance();
        v.set(from.x, from.y, from.z);
        return v;
    }

    public static Vector3f get(Vec3d from) {
        Vector3f v = getPool().provideNewInstance();
        v.set((float) from.x, (float) from.y, (float) from.z);
        return v;
    }

    public static Vector3f get(float x, float y, float z) {
        Vector3f v = getPool().provideNewInstance();
        v.set(x, y, z);
        return v;
    }

    public static Vector3f get(double x, double y, double z) {
        Vector3f v = getPool().provideNewInstance();
        v.set((float) x, (float) y, (float) z);
        return v;
    }

    public JmeVector3fPool() {
        super(40000, 4000);
    }

    @Override
    public Vector3f[] createNewPool(int newInstancesStart, int size) {
        Vector3f[] pool = new Vector3f[size];
        for (int i = newInstancesStart; i < size; i++)
            pool[i] = new Vector3f();
        return pool;
    }

    @Override
    public int getGrowthSize() {
        return 1000;
    }

    @Override
    public String toString() {
        return "Vector3fPool{" +
                "size=" + pool.length +
                ", subPoolCount=" + subPoolCount +
                '}';
    }
}
