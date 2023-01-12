package fr.dynamx.common.physics.terrain.cache;

import fr.aym.acslib.services.impl.thrload.DynamXThreadedModLoader;
import fr.dynamx.api.network.EnumPacketTarget;
import fr.dynamx.api.physics.terrain.ITerrainCache;
import fr.dynamx.api.physics.terrain.ITerrainElement;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.DynamXMain;
import fr.dynamx.common.network.packets.MessageUpdateChunk;
import fr.dynamx.common.physics.terrain.chunk.ChunkCollisions;
import fr.dynamx.common.physics.terrain.chunk.ChunkLoadingTicket;
import fr.dynamx.common.physics.terrain.chunk.ChunkTerrain;
import fr.dynamx.utils.DynamXConfig;
import fr.dynamx.utils.VerticalChunkPos;
import fr.dynamx.utils.debug.ChunkGraph;
import fr.dynamx.utils.debug.Profiler;
import fr.dynamx.utils.optimization.Vector3fPool;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.FMLCommonHandler;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * A {@link ITerrainCache} stored in a file
 */
public class FileTerrainCache implements ITerrainCache {
    private static final ThreadFactory factory = new DynamXThreadedModLoader.DefaultThreadFactory("DnxTerrainCache");
    private final ExecutorService POOL = Executors.newFixedThreadPool(1, factory);

    private final File storageDir;
    private TerrainFile slopesFile;

    private final Map<ChunkPos, TerrainFile> terrainFiles = new HashMap<>();

    //The Set avoids duplicates
    protected Set<VerticalChunkPos> dirtyChunks = ConcurrentHashMap.newKeySet();

    public FileTerrainCache(World world) {
        storageDir = new File(world.getSaveHandler().getWorldDirectory(), "DnxChunks");
        storageDir.mkdirs();

        File f = new File(storageDir, "dnxregion_main.dnx");
        if (f.exists()) {
            throw new UnsupportedOperationException("V2 terrain formats are no longer supported, please delete file DnxChunks/dnxregion_main.dnx of your map (note : your custom slopes will be deleted)");
        } else {
            f = new File(storageDir, "custom_slopes.dnx");
            if (f.exists() || storageDir.listFiles().length == 0) {
                initV4(f);
            } else {
                initV3(f);
            }
        }
    }

    private void initV4(File slopes) {
        DynamXMain.log.debug("V4 terrain format detected !");
        try {
            if (!slopes.exists()) //when the world is created
                isSlopesToSave = true; //ensure to write a new file, and don't detect V3 the next time
            slopesFile = new TerrainFile(slopes, true);
            slopesFile.load();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            slopes.delete(); //reset
            slopesFile = new TerrainFile(slopes, true);
        }
    }

    private void initV3(File slopes) {
        DynamXMain.log.warn("V3 terrain format detected ! You should convert your slopes.");
        throw new UnsupportedOperationException("Terrain V3 no longer supported. Use the version 2.17.0 to convert it, or delete your DnxChunks folder (this will delete your custom slopes)");
    }

    /**
     * Adds a chunk to the save list, removing old versions of this chunk
     *
     * @param collisions The chunk to save
     */
    @Override
    public void addChunkToSave(ChunkLoadingTicket loadingTicket, ChunkCollisions collisions) {
        if (DynamXConfig.enableDebugTerrainManager)
            ChunkGraph.addToGrah(collisions.getPos(), ChunkGraph.ChunkActions.SEND_SAVE, ChunkGraph.ActionLocation.UNKNOWN, collisions);
        //don't incr index here, but todoold take care if it's not incr just after chunk has been loaded
        //System.out.println("Adding dirty "+loadingTicket.getPos());
        dirtyChunks.add(loadingTicket.getPos());
        ChunkLoadingTicket.Snap snap = loadingTicket.snapshot();
        POOL.submit(() -> {
            if (snap.isValid())
                saveFile(collisions.getPos(), collisions.getElements());
            //else if(DynamXConfig.enableDebugTerrainManager)
            //  System.out.println("Skipped save of "+snap+" : not valid anymore");
        });
    }

    @Override
    public void invalidate(VerticalChunkPos pos, boolean changed, boolean syncChanges) {
        if (DynamXConfig.enableDebugTerrainManager)
            ChunkGraph.addToGrah(pos, ChunkGraph.ChunkActions.SEND_INVALIDATE, ChunkGraph.ActionLocation.UNKNOWN, null, "Changed: " + changed);
        if (changed) {
            invalidate(pos, syncChanges);
        }
    }

    @Override
    public void invalidate(ChunkLoadingTicket ticket, boolean changed, boolean syncChanges) {
        VerticalChunkPos pos = ticket.getPos();
        if (DynamXConfig.enableDebugTerrainManager)
            ChunkGraph.addToGrah(pos, ChunkGraph.ChunkActions.SEND_INVALIDATE, ChunkGraph.ActionLocation.UNKNOWN, null, "Changed: " + changed + " Status " + ticket.getStatusIndex());
        if (changed) {
            ticket.incrStatusIndex("invalidated_changed"); //will prevent any other tasks like loading
            invalidate(pos, syncChanges);
        }
    }

    private void invalidate(VerticalChunkPos pos, boolean syncChanges) {
        //invalidatingChunks.add(pos);
        if (syncChanges)
            dirtyChunks.add(pos);
        ChunkPos cpos = new ChunkPos(pos.x >> 5, pos.z >> 5); //16x16 chunks
        POOL.submit(() -> {
            TerrainFile FILE = getFileAt(cpos);
            FILE.removeChunk(pos);
            if (DynamXConfig.enableDebugTerrainManager)
                ChunkGraph.addToGrah(pos, ChunkGraph.ChunkActions.INVALIDATED, ChunkGraph.ActionLocation.SAVER, null, "Done");
        });
    }

    @Override
    public void tick() {
        if (!dirtyChunks.isEmpty()) {
            //Avoid concurrency problems
            VerticalChunkPos[] array = dirtyChunks.toArray(new VerticalChunkPos[0]);
            dirtyChunks.clear();

            //System.out.println("Send dirty "+ Arrays.toString(array));
            //TODO CLEAN CONDITION AND CODE
            if (FMLCommonHandler.instance().getMinecraftServerInstance() != null && FMLCommonHandler.instance().getMinecraftServerInstance().isDedicatedServer()) {
                DynamXContext.getNetwork().sendToClient(new MessageUpdateChunk(array), EnumPacketTarget.ALL);
            }
        }
        timeCounter++;
        if (needsTerrainSave && timeCounter % 200 == 0) {
            POOL.submit(this::writeModifiedFiles);
        }
    }

    @Override
    public ChunkTerrain load(ChunkLoadingTicket ticket, Profiler profiler) {
        //We assume it's not called while the chunk is saving : it already is in an upper cache
        VerticalChunkPos pos = ticket.getPos();
        if (DynamXConfig.enableDebugTerrainManager)
            ChunkGraph.addToGrah(pos, ChunkGraph.ChunkActions.LOAD_FROM_SAVE, ChunkGraph.ActionLocation.UNKNOWN, null, "Status: " + ticket.getStatusIndex());

        profiler.start(Profiler.Profiles.CHUNK_COLLS_LOAD_FROM_FILE);
        Vector3fPool.openPool();
        ChunkPos cpos = new ChunkPos(pos.x >> 5, pos.z >> 5); //16x16 chunks

        List<ITerrainElement> elements = getFileAt(cpos).loadChunk(pos, this);
        List<?> persistentElements = getSlopesFile().loadChunk(pos, this);
        if (persistentElements != null && searchForDuplicatesAndRemove(pos, (List<ITerrainElement.IPersistentTerrainElement>) persistentElements)) {
            DynamXMain.log.info("Saving modified chunk " + ticket + " due to duplicated slopes");
            saveFile(pos, new ChunkTerrain(elements == null ? new ArrayList<>() : elements, persistentElements == null ? new ArrayList<>() : (List<ITerrainElement.IPersistentTerrainElement>) persistentElements));
        }

        //System.out.println("LOADE "+persistentElements+" at "+pos);
        Vector3fPool.closePool();
        profiler.end(Profiler.Profiles.CHUNK_COLLS_LOAD_FROM_FILE);
        return new ChunkTerrain(elements == null ? new ArrayList<>() : elements, persistentElements == null ? new ArrayList<>() : (List<ITerrainElement.IPersistentTerrainElement>) persistentElements);
    }

    private boolean searchForDuplicatesAndRemove(VerticalChunkPos pos, List<ITerrainElement.IPersistentTerrainElement> elements) {
        List<ITerrainElement.IPersistentTerrainElement> duplicates = new ArrayList<>();
        for (int i = 0; i < elements.size() - 1; i++) {
            for (int j = i + 1; j < elements.size(); j++) {
                ITerrainElement.IPersistentTerrainElement element = elements.get(i);
                ITerrainElement.IPersistentTerrainElement comp = elements.get(j);
                if (!duplicates.contains(element) && !duplicates.contains(comp) && element.toString().equals(comp.toString())) {
                    duplicates.add(comp);
                }
            }
        }
        if (!duplicates.isEmpty()) {
            DynamXMain.log.warn("Found " + duplicates.size() + " duplicates at " + pos + " ! Removing them...");
            elements.removeAll(duplicates);
            return true;
        } else
            return false;
    }

    private TerrainFile getFileAt(ChunkPos pos) {
        if (terrainFiles.containsKey(pos))
            return terrainFiles.get(pos);
        else {
            File f = new File(storageDir, "region_" + pos.x + "_" + pos.z + ".dnx");
            TerrainFile FILE;
            try {
                FILE = new TerrainFile(f);
                terrainFiles.put(pos, FILE);
                FILE.load();
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
                f.delete(); //reset
                FILE = new TerrainFile(f);
                terrainFiles.put(pos, FILE);
            }
            return FILE;
        }
    }

    public TerrainFile getSlopesFile() {
        return slopesFile;
    }

    private volatile boolean needsTerrainSave;
    private final Queue<ChunkPos> terrainFileSaveQueue = new ArrayDeque<>();
    private boolean isSlopesToSave;
    private int timeCounter;

    /**
     * Saves a chunk collisions
     *
     * @param pos      The pos of the chunk
     * @param elements The elements of the chunk to save
     */
    protected void saveFile(VerticalChunkPos pos, ChunkTerrain elements) {
        if (DynamXConfig.enableDebugTerrainManager)
            ChunkGraph.addToGrah(pos, ChunkGraph.ChunkActions.SAVE_TO_FILE, ChunkGraph.ActionLocation.SAVER, null, "elements: " + elements);
        try {
            ChunkPos cpos = new ChunkPos(pos.x >> 5, pos.z >> 5); //16x16 chunks
            //System.out.println("Saving "+pos+" "+cpos);
            TerrainFile FILE = getFileAt(cpos);
            try {
                FILE.setChunk(pos, elements.getElements());
                boolean saveSlopes = !elements.getPersistentElements().isEmpty() || getSlopesFile().getAllKeys().contains(pos);
                if (saveSlopes)
                    getSlopesFile().setChunk(pos, (List<ITerrainElement>) (List<?>) elements.getPersistentElements());
                if (DynamXConfig.enableDebugTerrainManager)
                    ChunkGraph.addToGrah(pos, ChunkGraph.ChunkActions.SAVE_TO_FILE, ChunkGraph.ActionLocation.SAVER, null, "removed invalidating at " + pos);

                if (!terrainFileSaveQueue.contains(cpos))
                    terrainFileSaveQueue.offer(cpos);
                if (saveSlopes)
                    isSlopesToSave = true;
                needsTerrainSave = true;
            } catch (IOException e) {
                throw new RuntimeException("Chunk save failed", e);
            }
        } catch (Exception e) {
            e.printStackTrace();
            invalidate(pos, false);
        }
    }

    protected void writeModifiedFiles() {
        while (!terrainFileSaveQueue.isEmpty()) {
            ChunkPos cpos = terrainFileSaveQueue.remove();
            TerrainFile FILE = getFileAt(cpos);
            try {
                FILE.save();
            } catch (IOException e) {
                throw new RuntimeException("Chunk save failed", e);
            }
        }
        if (isSlopesToSave) {
            try {
                getSlopesFile().save();
            } catch (IOException e) {
                throw new RuntimeException("Slopes save failed", e);
            }
            isSlopesToSave = false;
        }
        needsTerrainSave = false;
    }

    @Override
    public boolean isRemoteCache() {
        return false;
    }

    /**
     * @return The directory used for storing terrain data
     */
    public File getStorageDir() {
        return storageDir;
    }

    @Override
    public void clear() {
        POOL.shutdown();
        terrainFiles.clear();
    }
}
