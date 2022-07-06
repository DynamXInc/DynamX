package fr.dynamx.utils.optimization;

import java.util.ArrayDeque;
import java.util.Queue;

public class HashMapPool {
    private static final ThreadLocal<HashMapPool> LOCAL_POOL = ThreadLocal.withInitial(HashMapPool::new);

    private final Queue<PooledHashMap<?, ?>> freeMaps = new ArrayDeque<>();

    public static <A, B> PooledHashMap<A, B> get() {
        return getINSTANCE().provideMapInstance();
    }

    public static HashMapPool getINSTANCE() {
        return LOCAL_POOL.get();
    }

    /**
     * Recycles an instance or enlarges the pool with new clean instances, then returns one of them
     */
    public <A, B> PooledHashMap<A, B> provideMapInstance() {
        PooledHashMap<A, B> map = (PooledHashMap<A, B>) freeMaps.poll();
        if (map != null) {
            return map.unlock();
        }
        return new PooledHashMap<>(this); //TODO TRACK THIS
    }

    protected void releaseMap(PooledHashMap<?, ?> map) {
        freeMaps.add(map);
    }
}
