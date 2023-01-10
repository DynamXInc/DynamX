package fr.dynamx.utils.optimization;

import com.jme3.math.Quaternion;

import javax.annotation.concurrent.ThreadSafe;

@ThreadSafe
public class QuaternionPool extends ClassPool<Quaternion> {
    private static final ThreadLocal<QuaternionPool> LOCAL_POOL = ThreadLocal.withInitial(QuaternionPool::new);

    public static void openPool() {
        getINSTANCE().openSubPool();
    }

    public static void closePool() {
        getINSTANCE().closeSubPool();
    }

    public static Quaternion get() {
        Quaternion v = getINSTANCE().provideNewInstance();
        v.set(0, 0, 0, 0);
        return v;
    }

    public static Quaternion get(float x, float y, float z, float w) {
        Quaternion v = getINSTANCE().provideNewInstance();
        v.set(x, y, z, w);
        return v;
    }

    public static Quaternion get(Quaternion from) {
        Quaternion v = getINSTANCE().provideNewInstance();
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

    public static QuaternionPool getINSTANCE() {
        return LOCAL_POOL.get();
    }
}
