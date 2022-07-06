package fr.dynamx.utils.optimization;

import net.minecraftforge.fml.common.FMLCommonHandler;

import java.util.HashMap;
import java.util.Map;

public class PooledHashMap<K, V> extends HashMap<K, V>
{
    private final HashMapPool owningPool;
    private volatile boolean locked;

    public static boolean DISABLE_POOL = FMLCommonHandler.instance().getSide().isClient();

    protected PooledHashMap(HashMapPool owningPool) {
        this.owningPool = owningPool;
    }

    public HashMapPool getOwningPool() {
        return owningPool;
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        if(locked) {
            throw new IllegalStateException("Locked: Available in pool");
        }
        super.putAll(m);
    }

    @Override
    public V put(K key, V value) {
        if(locked) {
            throw new IllegalStateException("Locked: Available in pool");
        }
        return super.put(key, value);
    }

    @Override
    public V get(Object key) {
        if(locked) {
            throw new IllegalStateException("Locked: Available in pool");
        }
        return super.get(key);
    }

    @Override
    public boolean remove(Object key, Object value) {
        if(locked) {
            throw new IllegalStateException("Locked: Available in pool");
        }
        return super.remove(key, value);
    }

    protected PooledHashMap<K, V> unlock() {
        locked = false;
        return this;
    }

    public void release() {
        locked = true;
        clear();
        if(!DISABLE_POOL)
            owningPool.releaseMap(this);
    }
}
