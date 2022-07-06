package fr.dynamx.utils.optimization;

import com.jme3.math.Vector3f;
import net.minecraft.util.math.Vec3d;
import scala.Int;

import javax.annotation.concurrent.ThreadSafe;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Stores different Vector3fPool for each thread, referenced by their id (named "instance" here)
 */
@ThreadSafe
public class Vector3fPool extends ClassPool<Vector3f> {
    private static final Queue<Long> freePools = new ConcurrentLinkedQueue<>();
    private static final Map<Long, RetainedVector3fPool> INSTANCES = new HashMap<>();

    private static final ThreadLocal<Vector3fPool> LOCAL_POOL = ThreadLocal.withInitial(Vector3fPool::new);

  //  private final Map<StackTraceElement, Integer> callers = new HashMap<>();

    public static Vector3fPool getPool() {
        return LOCAL_POOL.get();
    }

   /* @Override
    public Vector3f provideNewInstance() {
        StackTraceElement[] st = Thread.currentThread().getStackTrace();
        callers.put(st[3], callers.getOrDefault(st[3], 0)+1);
        return super.provideNewInstance();
    }*/

    public static RetainedVector3fPool getPool(long id, String retainer) {
        RetainedVector3fPool instance = INSTANCES.get(id);
        if (instance == null) {
            //System.out.println("Creating pool "+id+" "+retainer);
            INSTANCES.put(id, instance = new RetainedVector3fPool(retainer));
        }
        return instance;
    }

    /**
     * @return A <strong>new</strong> vector initialized with from's data
     */
    public static Vector3f getPermanentVector(Vector3f from) {
        return new Vector3f(from.x, from.y, from.z);
    }

    public static void openPool() {
        getPool().openSubPool();
    }

    public static void closePool() {
        getPool().closeSubPool();
    }

    public static Vector3f get() {
        return get(0, 0, 0);
    }

    public static Vector3f get(long instance, String retainer) {
        return get(instance, retainer, 0, 0, 0);
    }

    public static Vector3f get(Vector3f from) {
        Vector3f v = getPool().provideNewInstance();
        v.set(from);
        return v;
    }

    public static Vector3f get(Vec3d from) {
        Vector3f v = getPool().provideNewInstance();
        v.set((float) from.x, (float) from.y, (float) from.z);
        return v;
    }

    public static Vector3f get(long instance, String retainer, Vector3f from) {
        Vector3f v = getPool(instance, retainer).provideNewInstance();
        v.set(from);
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

    public static Vector3f get(long instance, String retainer, float x, float y, float z) {
        Vector3f v = getPool(instance, retainer).provideNewInstance();
        v.set(x, y, z);
        return v;
    }

    public Vector3fPool() {
        super(1000);
    }

    public static Map<Long, RetainedVector3fPool> getInstances() {
        return INSTANCES;
    }

    public static long findFreePool() {
        if (!freePools.isEmpty()) {
            return freePools.poll();
        }
        return UUID.randomUUID().getMostSignificantBits();
    }

    public static void disposePool(long id) {
        //System.out.println("Disposing pool "+id+" "+INSTANCES.size());
        freePools.add(id);
        if (freePools.size() > 30) {
            INSTANCES.remove(freePools.remove(0));
        }
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

    public static class RetainedVector3fPool extends Vector3fPool {
        private static final String DEFAULT_RETAINER = "i";

        public final String retainer;

        public RetainedVector3fPool(String retainer) {
            this.retainer = retainer;
        }

        public String getRetainer() {
            return retainer;
        }

        @Override
        public String toString() {
            return "RetainedVector3fPool{" +
                    "retainer='" + retainer + '\'' + " size=" + pool.length + " " +
                    '}';
        }
    }
}
