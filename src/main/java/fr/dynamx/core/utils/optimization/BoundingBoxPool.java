package fr.dynamx.core.utils.optimization;

import com.jme3.bounding.BoundingBox;

import javax.annotation.concurrent.ThreadSafe;

@ThreadSafe
public class BoundingBoxPool extends ClassPool<BoundingBox> {
    private static final ThreadLocal<BoundingBoxPool> LOCAL_POOL = ThreadLocal.withInitial(BoundingBoxPool::new);

    public static BoundingBox get() {
        BoundingBox v = getPool().provideNewInstance();
        v.setMinMax(JmeVector3fPool.get(), JmeVector3fPool.get());
        return v;
    }

    public BoundingBoxPool() {
        super(100, 30);
    }

    @Override
    public BoundingBox[] createNewPool(int newInstancesStart, int size) {
        BoundingBox[] pool = new BoundingBox[size];
        for (int i = newInstancesStart; i < size; i++) {
            pool[i] = new BoundingBox();
        }
        return pool;
    }

    @Override
    public int getGrowthSize() {
        return 10;
    }

    /**
     * @return The current thread's instance
     */
    public static BoundingBoxPool getPool() {
        return LOCAL_POOL.get();
    }
}
