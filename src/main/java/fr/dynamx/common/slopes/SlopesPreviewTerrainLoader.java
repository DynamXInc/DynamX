package fr.dynamx.common.slopes;

import fr.dynamx.api.physics.terrain.ITerrainManager;
import fr.dynamx.common.DynamXMain;
import fr.dynamx.common.items.tools.ItemSlopes;
import fr.dynamx.common.physics.terrain.PhysicsEntityTerrainLoader;
import fr.dynamx.common.physics.terrain.chunk.ChunkLoadingTicket;
import fr.dynamx.utils.VerticalChunkPos;
import fr.dynamx.utils.debug.Profiler;
import fr.dynamx.utils.optimization.Vector3fPool;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Loads the slopes around you when you hold an {@link ItemSlopes}, so you can see them
 */
public class SlopesPreviewTerrainLoader extends PhysicsEntityTerrainLoader {
    private boolean added;

    public SlopesPreviewTerrainLoader() {
        super(null);
    }

    @Override
    public void update(ITerrainManager terrain, Profiler profiler) {
        if (terrain.getWorld().isRemote || DynamXMain.proxy.getClientWorld() != null) {
            Vector3fPool.openPool();
            refresh(terrain, profiler);
            Vector3fPool.closePool();
        }
    }

    @SideOnly(Side.CLIENT)
    private void refresh(ITerrainManager terrain, Profiler profiler) {
        EntityPlayer player = Minecraft.getMinecraft().player;
        if (player != null && player.getHeldItemMainhand().getItem() instanceof ItemSlopes) {
            if (!added || lastChunkX != player.chunkCoordX || lastChunkY != player.chunkCoordY || lastChunkZ != player.chunkCoordZ) {
                VerticalChunkPos.Mutable pos = new VerticalChunkPos.Mutable();
                VerticalChunkPos.Mutable prevPos = new VerticalChunkPos.Mutable();
                for (int i = 0; i < 3; i++) {
                    for (int j = 0; j < 25; j++) {
                        int dx = (j % 5) - 2;
                        int dz = (j / 5) - 2;
                        pos.setPos(player.chunkCoordX + dx, player.chunkCoordY + i - 1, player.chunkCoordZ + dz);

                        prevPos.setPos(lastChunkX + dx, lastChunkY + i - 1, lastChunkZ + dz);
                        if (loadMatrice[i][j] != -1) {
                            terrain.unsubscribeFromChunk(prevPos.toImmutable());
                            loadMatrice[i][j] = -1;
                        }
                        int deltaX = pos.x * 16 + 8 - (int) player.posX;
                        int deltaY = pos.y * 16 + 8 - (int) player.posY;
                        int deltaZ = pos.z * 16 + 8 - (int) player.posZ;
                        if (needsToBeLoaded(Vector3fPool.get(), deltaX, deltaY, deltaZ)) {
                            //only slopes
                            ChunkLoadingTicket.TicketPriority priority = ChunkLoadingTicket.TicketPriority.LOW;
                            loadMatrice[i][j] = (byte) priority.ordinal();
                            terrain.subscribeToChunk(pos.toImmutable(), priority, profiler);
                        }
                    }
                }
                lastChunkX = player.chunkCoordX;
                lastChunkY = player.chunkCoordY;
                lastChunkZ = player.chunkCoordZ;
                added = true;
            }
        } else if (added) {
            onRemoved(terrain);
            added = false;
        }
    }

    @Override
    public void onRemoved(ITerrainManager terrain) {
        VerticalChunkPos.Mutable pos = new VerticalChunkPos.Mutable();
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 25; j++) {
                int dx = (j % 5) - 2;
                int dz = (j / 5) - 2;
                pos.setPos(lastChunkX + dx, lastChunkY + i - 1, lastChunkZ + dz);
                if (loadMatrice[i][j] != -1) {
                    terrain.unsubscribeFromChunk(pos.toImmutable());
                    loadMatrice[i][j] = -1;
                }
            }
        }
    }
}
