package fr.dynamx.core.common.physics.terrain;

import com.jme3.math.Vector3f;
import fr.dynamx.api.physics.terrain.IPhysicsTerrainLoader;
import fr.dynamx.api.physics.terrain.ITerrainManager;
import fr.dynamx.core.common.DynamXMain;
import fr.dynamx.core.common.entities.PhysicsEntity;
import fr.dynamx.core.common.physics.terrain.chunk.ChunkLoadingTicket;
import fr.dynamx.core.utils.VerticalChunkPos;
import fr.dynamx.core.utils.debug.Profiler;
import fr.hermes.api.mc.entities.HmEntity;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Loads terrain around all {@link PhysicsEntity}, so they don't fall in the ground
 */
public class PhysicsEntityTerrainLoader implements IPhysicsTerrainLoader {
    private static final Map<VerticalChunkPos, ChunkLoadingTicket.TicketPriority> toLoad = new ConcurrentHashMap<>();
    private static final Map<VerticalChunkPos, ChunkLoadingTicket.TicketPriority> toUnLoad = new ConcurrentHashMap<>();
    protected int lastChunkX, lastChunkY = Integer.MAX_VALUE, lastChunkZ; //note that this precises coordinates are an edge case where the chunk won't be loaded on entity spawn :O

    private static final int radiusY = 3;//3
    private static final int radiusYHalf = 1;//1
    private static final int radiusH = 7;//7
    private static final int squareRadiusH = radiusH * radiusH;
    private static final int radiusHHalf = 3;//3
    protected final byte[][] loadMatrice = new byte[3][49];
    //private final Map<VerticalChunkPos, ChunkLoadingTicket.TicketPriority> states = new HashMap<>(49 * 3);
    private final PhysicsEntity<?> entityIn;

    public PhysicsEntityTerrainLoader(PhysicsEntity<?> entityIn) {
        this.entityIn = entityIn;
        for (int i = 0; i < radiusY; i++) {
            for (int j = 0; j < radiusH * radiusH; j++) {
                loadMatrice[i][j] = -1;
            }
        }
    }

    @Override
    public void update(ITerrainManager terrain, Profiler profiler) {
        HmEntity mcEntity = entityIn.getMcEntity();
        if (lastChunkX != mcEntity.hm$getChunkX() || lastChunkY != mcEntity.hm$getChunkY() || lastChunkZ != mcEntity.hm$getChunkZ()) {
            profiler.start(Profiler.Profiles.DELTA_COMPUTE);
            VerticalChunkPos.Mutable pos = new VerticalChunkPos.Mutable();
            VerticalChunkPos.Mutable prevPos = new VerticalChunkPos.Mutable();
            int curChunkX = mcEntity.hm$getChunkX();
            int curChunkY = mcEntity.hm$getChunkY();
            int curChunkZ = mcEntity.hm$getChunkZ();
            for (int i = 0; i < radiusY; i++) { //TODO DEPENDS ON SPEED ?
                for (int j = 0; j < squareRadiusH; j++) {
                    int dx = (j % radiusH) - radiusHHalf;
                    int dz = (j / radiusH) - radiusHHalf;
                    pos.setPos(curChunkX + dx, curChunkY + i - radiusYHalf, curChunkZ + dz);
                    prevPos.setPos(lastChunkX + dx, lastChunkY + i - radiusYHalf, lastChunkZ + dz);
                    //boolean border = isBorderChunkUnsub(entityIn.chunkCoordX - lastChunkX, entityIn.chunkCoordY - lastChunkY, entityIn.chunkCoordZ - lastChunkZ, dx, i - radiusYHalf, dz);
                    if (loadMatrice[i][j] != -1) {
                        //if(border)
                        //    terrain.unsubscribeFromChunk(prevPos.toImmutable());
                        ChunkLoadingTicket.TicketPriority oldPriority = ChunkLoadingTicket.TicketPriority.values()[loadMatrice[i][j]];
                        VerticalChunkPos prevPosImmutable = prevPos.toImmutable();
                        ChunkLoadingTicket.TicketPriority newPriority = toLoad.get(prevPosImmutable);
                        if (newPriority != oldPriority)
                            toUnLoad.put(prevPosImmutable, oldPriority);
                        else
                            toLoad.remove(prevPosImmutable);
                        loadMatrice[i][j] = -1;
                    }
                    int deltaX = pos.x * 16 + 8 - (int) mcEntity.hm$getPosX();
                    int deltaY = pos.y * 16 + 8 - (int) mcEntity.hm$getPosY();
                    int deltaZ = pos.z * 16 + 8 - (int) mcEntity.hm$getPosZ();
                    if (needsToBeLoaded(entityIn.physicsHandler.getLinearVelocity(), deltaX, deltaY, deltaZ)) {
                        ChunkLoadingTicket.TicketPriority priority = getPriority(entityIn.physicsHandler.getLinearVelocity(), dx, i - radiusYHalf, dz, deltaX, deltaY, deltaZ);
                        loadMatrice[i][j] = (byte) priority.ordinal();
                        //border = isBorderChunkSub(entityIn.chunkCoordX - lastChunkX, entityIn.chunkCoordY - lastChunkY, entityIn.chunkCoordZ - lastChunkZ, dx, i - radiusYHalf, dz);
                        //if(border)
                        //mais attention bordel priorités
                        //terrain.subscribeToChunk(pos.toImmutable(), priority, profiler);
                        VerticalChunkPos posImmutable = pos.toImmutable();
                        ChunkLoadingTicket.TicketPriority oldPriority = toUnLoad.get(posImmutable);
                        if (oldPriority != priority)
                            toLoad.put(posImmutable, priority);
                        else
                            toUnLoad.remove(posImmutable);
                    }
                }
            }
            if (!toUnLoad.isEmpty()) {
                toUnLoad.keySet().forEach(terrain::unsubscribeFromChunk);
                toUnLoad.clear();
            }
            if (!toLoad.isEmpty()) {
                toLoad.forEach((load, priority) -> {
                    if (!terrain.subscribeToChunk(load, priority, profiler)) {
                        // failed to load (because the chunk is not loaded into Minecraft), so we mark it as not loaded here
                        int i = load.y - curChunkY + radiusYHalf;
                        int j = (load.x - curChunkX + radiusHHalf) + (load.z - curChunkZ + radiusHHalf) * radiusH;
                        if (i < 0 || i > loadMatrice.length || j < 0 || j > loadMatrice[i].length) {
                            DynamXMain.log.error("Local chunk pos computation error {} {} {} {} {} {} {} {} " + radiusY + " " + radiusYHalf + " " + radiusH + " " + radiusHHalf, i, j, lastChunkX, lastChunkY, lastChunkZ, curChunkX, curChunkY, curChunkZ);
                            return;
                        }
                        loadMatrice[i][j] = -1;
                    }
                });
                toLoad.clear();
            }
            lastChunkX = curChunkX;
            lastChunkY = curChunkY;
            lastChunkZ = curChunkZ;
            profiler.end(Profiler.Profiles.DELTA_COMPUTE);
        }
    }

    @Override
    public void onRemoved(ITerrainManager terrain) {
        VerticalChunkPos.Mutable pos = new VerticalChunkPos.Mutable();
        for (int i = 0; i < radiusY; i++) {
            for (int j = 0; j < radiusH * radiusH; j++) {
                int dx = (j % radiusH) - radiusHHalf;
                int dz = (j / radiusH) - radiusHHalf;
                pos.setPos(lastChunkX + dx, lastChunkY + i - radiusYHalf, lastChunkZ + dz);
                if (loadMatrice[i][j] != -1) {
                    terrain.unsubscribeFromChunk(pos.toImmutable());
                    loadMatrice[i][j] = -1;
                }
            }
        }
    }

    protected boolean isSameDir(int d1, float d2) {
        return (d1 >= 0) == (d2 >= 0);
    }

    protected boolean needsToBeLoaded(Vector3f entitySpeed, int dx, int dy, int dz) {
        //if(true)
        //  return true;
        if (!isSameDir(dx, entitySpeed.x) || !isSameDir(dz, entitySpeed.z)) { //Going far
            return Math.abs(dx) < ChunkLoadDistance.IMMOBILE.blockRadiusHoriz || Math.abs(dz) < ChunkLoadDistance.IMMOBILE.blockRadiusHoriz || Math.abs(dy) < ChunkLoadDistance.IMMOBILE.blockRadiusVertical;
        }
        dx = Math.abs(dx);
        dy = Math.abs(dy);
        dz = Math.abs(dz);
        ChunkLoadDistance speed = ChunkLoadDistance.IMMOBILE;
        for (ChunkLoadDistance distance : ChunkLoadDistance.values()) {
            if (entitySpeed.x > distance.speed || entitySpeed.z > distance.speed || entitySpeed.y > distance.speed) {
                speed = distance;
            }
        }
        return dx < speed.blockRadiusHoriz || dz < speed.blockRadiusHoriz || dy < speed.blockRadiusVertical;
    }

    protected ChunkLoadingTicket.TicketPriority getPriority(Vector3f entitySpeed, int dx, int dy, int dz, int blockDeltaX, int blockDeltaY, int blockDeltaZ) {
        //boolean yHigh = Math.abs(blockDeltaY) < 8 || Math.abs(entitySpeed.y) > 8;
        //boolean yMed = Math.abs(blockDeltaY) < 16 || Math.abs(entitySpeed.y) > 5;
        if (Math.abs(dx) <= 1 && Math.abs(dy) <= ((Math.abs(blockDeltaY) < 8 || Math.abs(entitySpeed.y) > 8) ? 1 : 0) && Math.abs(dz) <= 1)
            return ChunkLoadingTicket.TicketPriority.HIGH;
        if (Math.abs(dx) <= 2 && Math.abs(dy) <= ((Math.abs(blockDeltaY) < 16 || Math.abs(entitySpeed.y) > 5) ? 1 : 0) && Math.abs(dz) <= 2)
            return ChunkLoadingTicket.TicketPriority.MEDIUM;
        return ChunkLoadingTicket.TicketPriority.LOW;
    }

    public enum ChunkLoadDistance {
        IMMOBILE(0, 32, 22),
        MOVING_QUICK(15, 40, 30),
        MOVING_LIGHT(30, 48, 36);

        private final float speed;
        private final int blockRadiusHoriz, blockRadiusVertical;

        ChunkLoadDistance(float speed, int blockRadiusHoriz, int blockRadiusVertical) {
            this.speed = speed;
            this.blockRadiusHoriz = blockRadiusHoriz;
            this.blockRadiusVertical = blockRadiusVertical;
        }
    }
}
