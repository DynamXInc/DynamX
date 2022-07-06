package fr.dynamx.utils.optimization;

import com.jme3.math.Transform;

import javax.annotation.concurrent.ThreadSafe;

@ThreadSafe
public class TransformPool extends ClassPool<Transform> {
    private static final ThreadLocal<TransformPool> LOCAL_POOL = ThreadLocal.withInitial(TransformPool::new);

    private static final Transform cleanInstance = new Transform();

    public static Transform get() {
        Transform v = getPool().provideNewInstance();
        v.setTranslation(cleanInstance.getTranslation());
        v.setScale(cleanInstance.getScale().x);
        v.getRotation().set(cleanInstance.getRotation());
        return v;
    }

    public static Transform get(Transform from) {
        Transform v = getPool().provideNewInstance();
        v.setTranslation(from.getTranslation());
        v.setScale(from.getScale().x);
        v.getRotation().set(from.getRotation());
        return v;
    }

    public TransformPool() {
        super(100);
    }

    @Override
    public Transform[] createNewPool(int newInstancesStart, int size) {
        Transform[] pool = new Transform[size];
        for (int i = newInstancesStart; i < size; i++)
            pool[i] = new Transform();
        return pool;
    }

    @Override
    public int getGrowthSize() {
        return 50;
    }

    public static TransformPool getPool() {
        return LOCAL_POOL.get();
    }
}
