package fr.dynamx.utils.optimization;

import com.jme3.math.Quaternion;

import javax.annotation.concurrent.ThreadSafe;

@ThreadSafe
public class QuaternionPool extends ClassPool<Quaternion> {
    private static final ThreadLocal<QuaternionPool> LOCAL_POOL = ThreadLocal.withInitial(QuaternionPool::new);

    public static void openPool() {
        getPool().openSubPool(SubClassPool.QUATERNION_DEFAULT);
    }

    public static void openPool(String identifier) {
        getPool().openSubPool(identifier);
    }

    public static void closePool() {
        getPool().closeSubPool();
    }

    public static Quaternion get() {
        Quaternion v = getPool().provideNewInstance();
        v.set(0, 0, 0, 0);
        return v;
    }

    public static Quaternion get(float x, float y, float z, float w) {
        Quaternion v = getPool().provideNewInstance();
        v.set(x, y, z, w);
        return v;
    }

    public static Quaternion get(Quaternion from) {
        Quaternion v = getPool().provideNewInstance();
        v.set(from);
        return v;
    }

    public QuaternionPool() {
        super(3000, 600);
    }

    @Override
    public Quaternion[] createNewPool(int newInstancesStart, int size) {
        Quaternion[] pool = new Quaternion[size];
        for (int i = newInstancesStart; i < size; i++)
            pool[i] = new Quaternion();
        return pool;
    }

    @Override
    public int getGrowthSize() {
        return 200;
    }

    public static QuaternionPool getPool() {
        return LOCAL_POOL.get();
    }
}
