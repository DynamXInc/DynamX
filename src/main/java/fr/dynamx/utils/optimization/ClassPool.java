package fr.dynamx.utils.optimization;

import fr.dynamx.client.handlers.ClientDebugSystem;
import fr.dynamx.common.DynamXMain;
import net.minecraftforge.fml.common.FMLCommonHandler;

/**
 * Class pool utility for optimization <br>
 * Permits recycling objects instead of filling the memory
 */
public abstract class ClassPool<T> {
    protected T[] pool;
    protected SubClassPool<T> root;
    protected int subPoolCount;

    public ClassPool(int initialCapacity) {
        this.pool = createNewPool(0, initialCapacity);
    }

    /**
     * Opens a sub pool, all objects affected after this called will be released once you call closeSubPool
     */
    public void openSubPool() {
        if (root == null)
            root = new SubClassPool<>(null, 0);
        else
            root = new SubClassPool<>(root, root.getStartIndex() + root.getAffectedObjectsCount());
        subPoolCount++;
    }

    /**
     * Closes a sub pool and releases all previously affected objects
     */
    public void closeSubPool() {
        if (root != null) {
            root = root.getParent();
            subPoolCount--;
        } else
            DynamXMain.log.warn(new IllegalStateException("Tried to close a pool that was not opened"));
    }

    /**
     * Recycles an instance or enlarges the pool with new clean instances, then returns one of them
     */
    public T provideNewInstance() {
        T instance;
        if (root == null) {
            DynamXMain.log.throwing(new IllegalStateException("No sub-pool opened ! Opening a default one"));
            openSubPool();
        }
        if (root.getStartIndex() + root.getAffectedObjectsCount() >= pool.length) //If the pool is too small
        {
            T[] nPool = createNewPool(pool.length, root.getStartIndex() + root.getAffectedObjectsCount() + getGrowthSize()); //Allocate a bigger pool
            System.arraycopy(pool, 0, nPool, 0, pool.length);
            pool = nPool;

            if (this instanceof Vector3fPool) {
                if (FMLCommonHandler.instance().getSide().isClient() && ClientDebugSystem.enableDebugDrawing)
                    new NullPointerException("Bigger pool : " + pool.length + " ! " + this + " open c " + subPoolCount).printStackTrace();
                else
                    DynamXMain.log.debug("Bigger pool : " + pool.length + " ! " + this + " open c " + subPoolCount);
            }
        }
        instance = pool[root.getStartIndex() + root.getAffectedObjectsCount()]; //Take an unused instance
        root.affectObject(instance); //Instance is now used
        return instance;
    }

    /**
     * Used to enlarge the pool, not called often
     *
     * @return A array, empty from 0 to newInstancesStart-1, containing fresh instances from newInstancesStart to size
     */
    public abstract T[] createNewPool(int newInstancesStart, int size);

    /**
     * @return The number of "slots" too allocate when pool is too small
     */
    public abstract int getGrowthSize();

    public int getCurrentPoolObjectCount() {
        return root == null ? -10 : root.getAffectedObjectsCount();
    }

    public int getTotalAffectedObject() {
        if (root != null) {
            return root.getStartIndex() + root.getAffectedObjectsCount();
        }
        return 0;
    }

    public int getUnaffectedObjectsCount() {
        return pool.length - getTotalAffectedObject();
    }
}
