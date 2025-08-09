package fr.dynamx.core.utils.optimization;

import fr.dynamx.core.common.DynamXMain;
import net.minecraft.util.text.TextFormatting;

/**
 * Class pool utility for optimization <br>
 * Permits recycling objects instead of filling the memory
 */
public abstract class ClassPool<T> {
    protected final int capacityWarning;

    protected T[] pool;
    protected SubClassPool<T> root;
    protected int subPoolCount;
    protected int sizeWarnings;

    public ClassPool(int capacityWarning, int initialCapacity) {
        this.capacityWarning = capacityWarning;
        this.pool = createNewPool(0, initialCapacity);
    }

    /**
     * Opens a sub pool, all objects affected after this called will be released once you call closeSubPool
     */
    public void openSubPool(String identifier) {
        if (root == null) {
            root = new SubClassPool<>(null, 0, identifier);
        } else {
            root = new SubClassPool<>(root, root.getStartIndex() + root.getAffectedObjectsCount(), identifier);
        }
        subPoolCount++;
    }

    /**
     * Closes a sub pool and releases all previously affected objects
     */
    public void closeSubPool() {
        if (root != null) {
            //root.onClose();
            root = root.getParent();
            subPoolCount--;
        } else {
            DynamXMain.log.warn(new IllegalStateException("Tried to close a pool that was not opened"));
        }
    }

    /**
     * Recycles an instance or enlarges the pool with new clean instances, then returns one of them
     */
    public T provideNewInstance() {
        T instance;
        if (root == null) {
            DynamXMain.log.throwing(new IllegalStateException("No sub-pool opened ! Opening a default one"));
            openSubPool(SubClassPool.DEFAULT_DEFAULT);
        }
        if (root.getStartIndex() + root.getAffectedObjectsCount() >= pool.length) //If the pool is too small
        {
            T[] nPool = createNewPool(pool.length, root.getStartIndex() + root.getAffectedObjectsCount() + getGrowthSize()); //Allocate a bigger pool
            System.arraycopy(pool, 0, nPool, 0, pool.length);
            pool = nPool;

            if (pool.length > capacityWarning) {
                DynamXMain.log.warn("Optimization issue : Pool is very large : {} ! open c {} of type {}", pool.length, subPoolCount, this);
                if (sizeWarnings < 8) {
                    Thread.dumpStack();
                }
                sizeWarnings++;
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

    public String getDebugInfo() {
        return "spc=" + subPoolCount + ", tt=" + pool.length + ", used=" + getCurrentPoolObjectCount() + ", tt_used=" + getTotalAffectedObject() + ", tt_free=" + getUnaffectedObjectsCount();
    }

    public String getExpandedDebugInfo() {
        if(root == null) {
            return "empty";
        }
        int curDepth = subPoolCount;
        StringBuilder result = new StringBuilder();
        SubClassPool<T> current = root;
        int maxDepth = curDepth - 400;
        if(curDepth > 400) {
            result.append(TextFormatting.RED).append("Current pool depth is larger than 400. You have a leak somewhere.").append("\n");
        }
        while (current != null && curDepth > maxDepth) {
            result.append("At: " ).append(curDepth).append(": ").append(current).append("\n");
            current = current.getParent();
            curDepth -= 1;
        }
        return result.toString();
    }
}
