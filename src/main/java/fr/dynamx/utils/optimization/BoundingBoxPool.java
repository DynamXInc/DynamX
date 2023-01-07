package fr.dynamx.utils.optimization;

import com.jme3.bounding.BoundingBox;

import javax.annotation.concurrent.ThreadSafe;

@ThreadSafe
public class BoundingBoxPool extends ClassPool<BoundingBox> {
    private static final ThreadLocal<BoundingBoxPool> LOCAL_POOL = ThreadLocal.withInitial(BoundingBoxPool::new);

    /*@Override
    public void openSubPool() {
        if(root == null)
            super.openSubPool();
    }
//03/02/21 : let default behavior :thinking:
    @Override
    public void closeSubPool() {}*/

    public static BoundingBox get() {
        BoundingBox v = getPool().provideNewInstance();
        v.setMinMax(Vector3fPool.get(), Vector3fPool.get());
        return v;
    }

    public BoundingBoxPool() {
        super(100, 30);
    }

    @Override
    public BoundingBox[] createNewPool(int newInstancesStart, int size) {
        BoundingBox[] pool = new BoundingBox[size];
        for (int i = newInstancesStart; i < size; i++)
            pool[i] = new BoundingBox();
        return pool;
    }

    @Override
    public int getGrowthSize() {
        return 10;
    }

    /**
     * @return The current threads's instance
     */
    public static BoundingBoxPool getPool() {
        return LOCAL_POOL.get();
    }
}
