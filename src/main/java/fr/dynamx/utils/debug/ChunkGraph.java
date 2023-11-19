package fr.dynamx.utils.debug;

import fr.dynamx.common.DynamXMain;
import fr.dynamx.common.physics.terrain.chunk.ChunkCollisions;
import fr.dynamx.common.physics.terrain.chunk.ChunkLoadingTicket;
import fr.dynamx.common.physics.terrain.chunk.DebugChunkCollisions;
import fr.dynamx.utils.DynamXConfig;
import fr.dynamx.utils.VerticalChunkPos;

import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Keeps history of all {@link ChunkCollisions} events
 */
public class ChunkGraph {
    private static int LISTEN;
    private static final ConcurrentHashMap<VerticalChunkPos, ChunkGraph> graphs = new ConcurrentHashMap<>(0, 0.75f, 2);

    public static void addToGrah(VerticalChunkPos pos, ChunkActions action, ActionLocation location, ChunkCollisions chunk) {
        addToGrah(pos, action, location, chunk, null);
    }

    public static void addToGrah(VerticalChunkPos pos, ChunkActions action, ActionLocation location, ChunkLoadingTicket ticket) {
        addToGrah(pos, action, location, null, ticket.toString());
    }

    public static void addToGrah(VerticalChunkPos pos, ChunkActions action, ActionLocation location, ChunkCollisions chunk, String info) {
        try {
            if ((chunk == null || chunk instanceof DebugChunkCollisions) && ((LISTEN == 0 && DynamXConfig.enableDebugTerrainManager) || LISTEN == 2 || (LISTEN == 1 && DynamXConfig.chunkDebugPoses.contains(pos)))) {
                if (LISTEN == 0 && graphs.size() > 60) {
                    graphs.clear();
                }
                if (!graphs.containsKey(pos)) {
                    graphs.put(pos, new ChunkGraph(pos));
                }

                graphs.get(pos).actions.add(new HistoryEntry(action, location, System.currentTimeMillis(), chunk == null ? null : ((DebugChunkCollisions) chunk).getId(), info));
                if (LISTEN == 0) {
                    while (graphs.get(pos).actions.size() > 25) { //TODOOLD DISABLE THIS SO EXPENSIVE THING
                        graphs.get(pos).actions.remove(0);
                    }
                }
            }
        } catch (Exception e) {
            DynamXMain.log.error("Error while adding debug for chunk " + pos, e);
        }
    }

    public static void start(int mode) {
        if (mode == 0 && LISTEN >= 0)
            finish();
        if (mode == -1 && LISTEN != -1)
            finish();
        LISTEN = mode;
    }

    private static void finish() {
        graphs.forEach((p, g) -> {
            DynamXMain.log.info("======================= " + p + " =======================");
            g.prettyPrint();
            DynamXMain.log.info("=====================================================");
        });
        graphs.clear();
    }

    public static ChunkGraph getAt(VerticalChunkPos pos) {
        return graphs.get(pos);
    }

    public final VerticalChunkPos listenedPos;
    public final ArrayList<HistoryEntry> actions = new ArrayList<>();

    public ChunkGraph(VerticalChunkPos listenedPos) {
        this.listenedPos = listenedPos;
    }

    public void prettyPrint() {
        try {
            for (int i = 0; i < actions.size(); i++) {
                DynamXMain.log.info("[" + i + "] => " + actions.get(i).pretty());
            }
            actions.clear();
        } catch (Exception e) {
            DynamXMain.log.error("Error while printing debug for chunk " + listenedPos, e);
        }
    }

    public enum ChunkActions {
        CREATE_INSTANCE, ADD_TO_WORLD, HOTSWAP, ASYNC_MANAGER_QUERY, LOAD_NOW, LOAD_ASYNC, DESTROY, CHK_UNLOAD,
        CHK_UPDATE, RESET, LOAD_INTERNAL, LOAD_INTERNAL_DOING, SEND_SAVE, LOAD_FROM_SAVE, SEND_INVALIDATE, SAVE_TO_FILE, INVALIDATED,
        ERR_ASYNC_CACHE_LOAD_STARTED, ERR_ASYNC_CACHE_LOAD_FAIL, ASYNC_REMOTE_RCV, ASYNC_COMPLETE_FUTURE_EXEC,
        SET_LOADING, REMOVE_FROM_WORLD, INITIATE_LOAD, OVERRIDE_LOAD, SET_LOADED,
    }

    public enum ActionLocation {
        MAIN, LOADER, SAVER, UNKNOWN
    }

    public static class HistoryEntry {
        public final ChunkActions action;
        public final ActionLocation location;
        public final String info;
        public final UUID chunkInstance;
        public final long stamp;

        public HistoryEntry(ChunkActions action, ActionLocation location, long stamp, UUID chunkInstance, String info) {
            this.action = action;
            this.location = location;
            this.stamp = stamp;
            this.info = info;
            this.chunkInstance = chunkInstance;
        }

        public String pretty() {
            String time = String.format("%1$TH:%1$TM:%1$TS", stamp);
            return "At " + time + " => " + action + " in " + location + (info == null ? "" : " // info => " + info) + (chunkInstance == null ? "" : " // instance => " + chunkInstance);
        }
    }
}
