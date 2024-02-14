package fr.dynamx.utils.optimization;

import org.lwjgl.util.vector.Quaternion;

import javax.annotation.concurrent.NotThreadSafe;

@NotThreadSafe
public class GlQuaternionPool extends ClassPool<Quaternion> {
    private static final GlQuaternionPool INSTANCE = new GlQuaternionPool();

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

    public static Quaternion get(Quaternion from) {
        Quaternion v = getINSTANCE().provideNewInstance();
        v.set(from);
        return v;
    }

    public static Quaternion get(com.jme3.math.Quaternion from) {
        Quaternion v = getINSTANCE().provideNewInstance();
        v.set(from.getX(), from.getY(), from.getZ(), from.getW());
        return v;
    }

    public GlQuaternionPool() {
        super(100, 10);
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
        return 10;
    }

    public static GlQuaternionPool getINSTANCE() {
        return INSTANCE;
    }

    public static Quaternion newGlQuaternion(com.jme3.math.Quaternion fromJmeQuaternion) {
        return fromJmeQuaternion == null ? null : new Quaternion(fromJmeQuaternion.getX(), fromJmeQuaternion.getY(), fromJmeQuaternion.getZ(), fromJmeQuaternion.getW());
    }
}
