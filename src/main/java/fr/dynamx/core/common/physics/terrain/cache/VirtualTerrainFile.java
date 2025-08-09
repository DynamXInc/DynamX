package fr.dynamx.core.common.physics.terrain.cache;

import fr.dynamx.api.physics.terrain.ITerrainCache;
import fr.dynamx.api.physics.terrain.ITerrainElement;
import fr.dynamx.core.common.DynamXMain;
import fr.dynamx.core.common.physics.terrain.element.TerrainElementsFactory;
import fr.dynamx.forge.DynamXConfig;
import fr.dynamx.core.utils.DynamXUtils;
import fr.dynamx.core.utils.VerticalChunkPos;

import java.io.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Virtual terrain data cache (used as is on remote clients)
 *
 * @see FileTerrainCache
 */
public class VirtualTerrainFile {
    protected final ConcurrentHashMap<VerticalChunkPos, byte[]> dataCache = new ConcurrentHashMap<>();
    protected final ConcurrentHashMap<VerticalChunkPos, Lock> loadingLocks = new ConcurrentHashMap<>();

    public void lock(VerticalChunkPos pos) {
        synchronized (loadingLocks) {
            if (!loadingLocks.containsKey(pos)) {
                loadingLocks.put(pos, new ReentrantLock());
            }
            loadingLocks.get(pos).lock();
        }
    }

    public void unlock(VerticalChunkPos pos) {
        loadingLocks.get(pos).unlock();
    }

    public void setChunk(VerticalChunkPos pos, List<ITerrainElement> elements) throws IOException {
        boolean debug = DynamXConfig.enableDebugTerrainManager && DynamXConfig.chunkDebugPoses.contains(pos);
        if (debug) {
            DynamXMain.log.info("[CHUNK DEBUG] Saving chunk " + pos + " with " + elements.size() + " elements in " + this);
        }
        lock(pos);
        ByteArrayOutputStream data = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(new GZIPOutputStream(data));
        out.writeInt(elements.size());
        for (ITerrainElement e : elements) {
            if (debug)
                DynamXMain.log.info("[CHUNK DEBUG] Saving element " + e);
            out.writeByte(e.getFactory().ordinal());
            e.save(ITerrainElement.TerrainSaveType.DISK, out);
        }
        out.close();
        byte[] arr = data.toByteArray();
        dataCache.put(pos, arr);
        unlock(pos);
    }

    public List<ITerrainElement> loadChunk(VerticalChunkPos pos, ITerrainCache terrainCache) {
        boolean debug = DynamXConfig.enableDebugTerrainManager && DynamXConfig.chunkDebugPoses.contains(pos);
        if (debug) {
            DynamXMain.log.info("[CHUNK DEBUG] Loading chunk " + pos + " in " + this);
        }

        lock(pos);
        byte[] dt = dataCache.get(pos);
        if (dt == null) {
            if (debug) {
                DynamXMain.log.error("[CHUNK DEBUG] Chunk not found in save file " + this);
            }
            unlock(pos);
            return null;
        }
        List<ITerrainElement> terrainElements = new ArrayList<>();
        ObjectInputStream in = null;
        try {
            in = DynamXUtils.getTerrainObjectsIS(new GZIPInputStream(new ByteArrayInputStream(dt)));
            int size = in.readInt();
            if (debug)
                DynamXMain.log.info("[CHUNK DEBUG] Found " + size + " elements");
            for (int i = 0; i < size; i++) {
                ITerrainElement o;
                o = TerrainElementsFactory.getById(in.readByte());
                if (debug)
                    DynamXMain.log.info("[CHUNK DEBUG] Loading element " + o);
                if (o.load(ITerrainElement.TerrainSaveType.DISK, in, pos)) {
                    terrainElements.add(o);
                } else {
                    throw new IllegalStateException("Terrain element " + o + " failed to load");
                }
            }
            in.close();
        } catch (InvalidClassException | IllegalArgumentException e) {
            DynamXMain.log.warn("Invalid terrain save version at " + pos + ", invalidating it... Error is " + e.getMessage());
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ignored) {
                }
            }
            terrainCache.invalidate(pos, true, false); //remove loaded elements
        } catch (Exception e) {
            DynamXMain.log.error("Cannot load terrain save at " + pos + ", invalidating it...", e);
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ignored) {
                }
            }
            terrainCache.invalidate(pos, true, false); //remove loaded elements
        } finally {
            unlock(pos);
        }
        return terrainElements;
    }

    public void removeChunk(VerticalChunkPos pos) {
        dataCache.remove(pos);
    }

    public Collection<VerticalChunkPos> getAllKeys() {
        return dataCache.keySet();
    }

    public byte[] getRawChunkData(VerticalChunkPos pos) {
        return dataCache.getOrDefault(pos, null);
    }

    public void putData(VerticalChunkPos pos, byte[] newData) {
        dataCache.put(pos, newData);
    }
}
