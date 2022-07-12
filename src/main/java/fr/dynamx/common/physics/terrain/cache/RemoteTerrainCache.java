package fr.dynamx.common.physics.terrain.cache;

import fr.dynamx.api.physics.terrain.ITerrainCache;
import fr.dynamx.api.physics.terrain.ITerrainElement;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.DynamXMain;
import fr.dynamx.common.network.packets.MessageQueryChunks;
import fr.dynamx.common.physics.terrain.chunk.ChunkCollisions;
import fr.dynamx.common.physics.terrain.chunk.ChunkLoadingTicket;
import fr.dynamx.common.physics.terrain.chunk.ChunkTerrain;
import fr.dynamx.common.physics.terrain.element.TerrainElementType;
import fr.dynamx.common.physics.terrain.element.TerrainElementsFactory;
import fr.dynamx.utils.DynamXConfig;
import fr.dynamx.utils.VerticalChunkPos;
import fr.dynamx.utils.debug.ChunkGraph;
import fr.dynamx.utils.debug.Profiler;
import fr.dynamx.utils.optimization.HashMapPool;
import fr.dynamx.utils.optimization.PooledHashMap;
import fr.dynamx.utils.optimization.Vector3fPool;

import javax.annotation.Nullable;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.zip.GZIPInputStream;

/**
 * Keeps a local copy of the terrain loaded from a remote server
 */
public class RemoteTerrainCache implements ITerrainCache {
    private static boolean HAD_THE_ERROR;

    private final VirtualTerrainFile rawSlopeDataCache = new VirtualTerrainFile();
    private final Map<VerticalChunkPos, ChunkTerrain> dataCache = new HashMap<>();

    /**
     * Queue for loading chunks
     */
    private final Map<ChunkLoadingTicket.Snap, CompletableChunkLoading> queries = new HashMap<>();
    private final LinkedBlockingQueue<ChunkLoadingTicket.Snap> sendQueue = new LinkedBlockingQueue<>();

    private final List<VerticalChunkPos> erroredChunks = new ArrayList<>();

    @Override
    public void invalidate(VerticalChunkPos pos, boolean changed, boolean syncChanges) {
        dataCache.remove(pos);
        erroredChunks.remove(pos);
        rawSlopeDataCache.removeChunk(pos);
    }

    @Override
    public void invalidate(ChunkLoadingTicket pos, boolean changed, boolean syncChanges) {
        invalidate(pos.getPos(), changed, syncChanges);
    }

    @Override
    public void clear() {
        dataCache.clear();
        erroredChunks.clear();
        queries.clear();
    }

    @Override
    public CompletableFuture<ChunkTerrain> asyncLoad(ChunkLoadingTicket ticket, TerrainElementType terrainType) {
        ChunkTerrain chunkTerrain = load(ticket, Profiler.get());
        if (chunkTerrain != null) {
            return CompletableFuture.completedFuture(chunkTerrain);
        } else if (!queries.containsKey(ticket)) {
            if (erroredChunks.contains(ticket.getPos())) {
                ChunkGraph.addToGrah(ticket.getPos(), ChunkGraph.ChunkActions.ERR_ASYNC_CACHE_LOAD_FAIL, ChunkGraph.ActionLocation.LOADER, null, ticket + " " + terrainType);
                return CompletableFuture.completedFuture(null);
            } else {
                CompletableFuture<ChunkTerrain> future = new CompletableFuture<>();
                //oh no don't do this it breaks chunk collision's ticket ticket.incrStatusIndex();
                ChunkLoadingTicket.Snap snap = ticket.snapshot();
                ChunkGraph.addToGrah(snap.getPos(), ChunkGraph.ChunkActions.ASYNC_MANAGER_QUERY, ChunkGraph.ActionLocation.LOADER, null, "STEP1 " + snap.getTicket() + " " + snap.getSnapIndex() + " " + terrainType + " " + rawSlopeDataCache.getAllKeys().contains(snap.getTicket().getPos()));
                queries.put(snap, new CompletableChunkLoading((byte) (snap.getSnapIndex() % 255), future));
                //System.out.println("Query "+ticket+" "+snap+" "+terrainType);
                sendQueue.add(snap);
                return future;
            }
        } else {
            CompletableFuture<ChunkTerrain> old = queries.get(ticket).getFuture();

            CompletableFuture<ChunkTerrain> future = new CompletableFuture<>();
            //oh no don't do this it breaks chunk collision's ticket ticket.incrStatusIndex();
            ChunkLoadingTicket.Snap snap = ticket.snapshot();
            queries.put(snap, new CompletableChunkLoading((byte) (snap.getSnapIndex() % 255), future));
            ChunkGraph.addToGrah(ticket.getPos(), ChunkGraph.ChunkActions.ERR_ASYNC_CACHE_LOAD_STARTED, ChunkGraph.ActionLocation.LOADER, null, ticket + " " + terrainType + " " + old + " " + future + " " + queries.get(snap.getPos()));
            //System.out.println("RE-Query "+ticket+" "+snap+" "+terrainType);
            sendQueue.add(snap);

            old.complete(null);

            return future;
        }
    }

    @Override
    public void tick() {
        if (!sendQueue.isEmpty()) {
            PooledHashMap<VerticalChunkPos, byte[]> requests = HashMapPool.get();
            while (!sendQueue.isEmpty()) {
                ChunkLoadingTicket.Snap snap = sendQueue.remove();
                if (snap.isValid()) {
                    boolean slopesOnly = snap.getTicket().getPriority() == ChunkLoadingTicket.TicketPriority.LOW;
                    ChunkGraph.addToGrah(snap.getPos(), ChunkGraph.ChunkActions.ASYNC_MANAGER_QUERY, ChunkGraph.ActionLocation.LOADER, null, "STEP2 " + snap.getTicket() + " " + snap.getSnapIndex() + " " + slopesOnly + " " + rawSlopeDataCache.getAllKeys().contains(snap.getTicket().getPos()));
                    requests.put(snap.getTicket().getPos(), new byte[]{(byte) (slopesOnly ? 2 : (rawSlopeDataCache.getAllKeys().contains(snap.getTicket().getPos()) ? 1 : 0)), (byte) (snap.getSnapIndex() % 255)});
                }
            }
            if (!requests.isEmpty()) {
                DynamXContext.getNetwork().sendToServer(new MessageQueryChunks(requests));
            } else {
                requests.release();
            }
        }
    }

    @Override
    public void addChunkToSave(ChunkLoadingTicket loadingTicket, ChunkCollisions collisions) {
    } //Don't remember of locally computed collisions, we can't trust it

    /**
     * Handles received chunk data from the server
     *
     * @param pos       The chunk
     * @param dataType  Data type : 1 : only normal elements, 2 : only persistent elements, 0 : both
     * @param snapIdMod The snapshot id of the data request
     * @param rawData   The received data
     */
    public void receiveChunkData(VerticalChunkPos pos, byte dataType, byte snapIdMod, @Nullable byte[] rawData) {
        if (queries.containsKey(pos)) {
            CompletableChunkLoading snap = queries.get(pos);
            if (snap.getSnapIdMod() != snapIdMod) {
                if (DynamXConfig.enableDebugTerrainManager)
                    DynamXMain.log.error("PRE: Ignoring request answer " + snapIdMod + ". Now we want " + snap.getSnapIdMod() + " " + pos + ". Some data was " + (rawData == null));
                return;
            }
            if (DynamXContext.getPhysicsWorld() != null) {
                ChunkTerrain data = null;
                //If received data is not null, and if there are normal elements
                if (rawData != null && dataType != 2) {
                    List<ITerrainElement> elements = new ArrayList<>();
                    List<ITerrainElement.IPersistentTerrainElement> persistents = new ArrayList<>();
                    Vector3fPool.openPool();
                    //long start = System.currentTimeMillis();
                    long start2;
                    ObjectInputStream in = null;
                    try {
                        in = new ObjectInputStream(new GZIPInputStream(new ByteArrayInputStream(rawData)));
                        int size = in.readInt();
                        for (int i = 0; i < size; i++) { //Read all received elements
                            start2 = System.currentTimeMillis();
                            ITerrainElement o = TerrainElementsFactory.getById(in.readByte());
                            if (o.load(ITerrainElement.TerrainSaveType.NETWORK, in, pos)) {
                                if (o instanceof ITerrainElement.IPersistentTerrainElement) {
                                    if (dataType == 0)
                                        persistents.add((ITerrainElement.IPersistentTerrainElement) o);
                                    else //This dataType does not allow persistent elements
                                        DynamXMain.log.error("Persistent elements should be sent before normal elements. Data " + pos + " " + dataType + " " + o);
                                } else {
                                    elements.add(o);
                                }
                            }
                            start2 = System.currentTimeMillis() - start2;
                            if (start2 > DynamXConfig.networkChunkComputeWarnTime) {
                                DynamXMain.log.warn("Took " + start2 + " ms to load terrain from network at " + pos + " for element " + i + " of type " + o);
                            }
                        }
                        if (dataType == 1) { //Add cached persistent elements to the received data
                            List<?> t = rawSlopeDataCache.loadChunk(pos, this);
                            if (t != null) {
                                persistents.addAll((Collection<? extends ITerrainElement.IPersistentTerrainElement>) t);
                            }
                        }
                    } catch (Exception e) {
                        if (!HAD_THE_ERROR) {
                            DynamXMain.log.fatal("Cannot unserialize terrain element at " + pos, e);
                            HAD_THE_ERROR = true;
                        }
                        elements = new ArrayList<>(); //Mark the error
                        persistents = new ArrayList<>();
                    } finally {
                        if (in != null) {
                            try {
                                in.close();
                            } catch (IOException e) {
                                DynamXMain.log.error("I/O error closing data stream", e);
                            }
                        }
                        /*start = (System.currentTimeMillis() - start);
                        if (start > DynamXConfig.networkChunkComputeWarnTime) {
                            DynamXMain.log.warn("Took " + start + " ms to load terrain from network at " + pos + " ! Loaded " + elements + " elements and " + persistents + " persistent elements");
                        }*/
                        Vector3fPool.closePool();
                        data = new ChunkTerrain(elements, persistents);
                    }
                }
                //Handle received data
                ChunkTerrain finalData = data;
                DynamXContext.getPhysicsWorld().schedule(() -> receiveChunkData(pos, dataType, snapIdMod, rawData, finalData));
            }
        } else {
            //TODO DEBUG THIS AND THE BOY BELOW
            DynamXMain.log.error("NO CORRESPONDING QUERY FOUND FOR " + pos + " " + dataType + " " + snapIdMod + " " + (rawData == null) + " at step: receiving data");
        }
    }

    private void receiveChunkData(VerticalChunkPos pos, byte dataType, byte snapIdMod, @Nullable byte[] rawData, @Nullable ChunkTerrain parsedData) {
        ChunkGraph.addToGrah(pos, ChunkGraph.ChunkActions.ASYNC_REMOTE_RCV, ChunkGraph.ActionLocation.LOADER, null, "DataType " + dataType + " " + snapIdMod + " " + parsedData + " " + queries.containsKey(pos) + " " + (rawData == null));
        if (queries.containsKey(pos)) {
            CompletableChunkLoading future = queries.get(pos);
            if (future.getSnapIdMod() != snapIdMod) { //Re-check validity
                if (DynamXConfig.enableDebugTerrainManager)
                    DynamXMain.log.error("HD: Ignoring request answer " + snapIdMod + ". Now we want " + future.getSnapIdMod() + " " + pos + ". Some data was " + (rawData == null));
                return;
            } else {
                queries.remove(pos);
            }
            if (dataType == 2) { //Persistent elements
                if (rawData != null) { //Not empty : load received elements and complete the query
                    rawSlopeDataCache.putData(pos, rawData);
                    Vector3fPool.openPool();
                    future.complete(new ChunkTerrain((List<ITerrainElement.IPersistentTerrainElement>) (List<?>) rawSlopeDataCache.loadChunk(pos, this)));
                    Vector3fPool.closePool();
                } else { //Empty : complete the query
                    future.complete(new ChunkTerrain());
                }
            } else {
                if (parsedData != null) { //Not empty : store received elements
                    dataCache.put(pos, parsedData);
                } else { //Empty : it's an error
                    DynamXMain.log.error("Found an empty errored chunk at " + pos + " " + dataType + " " + (rawData == null));
                    erroredChunks.add(pos);
                }
                //Complete the query
                Vector3fPool.openPool();
                future.complete(parsedData);
                Vector3fPool.closePool();
            }
        } else {
            DynamXMain.log.error("NO CORRESPONDING QUERY FOUND FOR " + pos + " " + dataType + " " + (rawData == null) + " at step: handling data");
        }
    }

    @Override
    public ChunkTerrain load(ChunkLoadingTicket ticket, Profiler profiler) {
        if (dataCache.containsKey(ticket.getPos())) {
            return dataCache.get(ticket.getPos());
        } else if (ticket.getPriority() == ChunkLoadingTicket.TicketPriority.LOW && rawSlopeDataCache.getAllKeys().contains(ticket.getPos())) {
            DynamXMain.log.warn("[This is not an error] Using cached slopes at " + ticket);
            return new ChunkTerrain((List<ITerrainElement.IPersistentTerrainElement>) (List<?>) rawSlopeDataCache.loadChunk(ticket.getPos(), this));
        }
        return null;
    }

    @Override
    public boolean isRemoteCache() {
        return true;
    }

    private static class CompletableChunkLoading {
        private final byte snapIdMod;
        private final CompletableFuture<ChunkTerrain> future;

        private CompletableChunkLoading(byte snapIdMod, CompletableFuture<ChunkTerrain> future) {
            this.snapIdMod = snapIdMod;
            this.future = future;
        }

        public byte getSnapIdMod() {
            return snapIdMod;
        }

        public CompletableFuture<ChunkTerrain> getFuture() {
            return future;
        }

        public void complete(ChunkTerrain terrain) {
            future.complete(terrain);
        }
    }
}
