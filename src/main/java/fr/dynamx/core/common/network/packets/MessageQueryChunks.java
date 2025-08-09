package fr.dynamx.core.common.network.packets;

import fr.dynamx.api.network.EnumNetworkType;
import fr.dynamx.api.network.EnumPacketTarget;
import fr.dynamx.api.network.IDnxPacket;
import fr.dynamx.api.physics.IPhysicsWorld;
import fr.dynamx.api.physics.terrain.ITerrainElement;
import fr.dynamx.api.physics.terrain.ITerrainManager;
import fr.dynamx.core.client.handlers.ClientEventHandler;
import fr.dynamx.core.common.DynamXContext;
import fr.dynamx.core.common.DynamXMain;
import fr.dynamx.core.common.physics.terrain.cache.FileTerrainCache;
import fr.dynamx.core.common.physics.terrain.cache.RemoteTerrainCache;
import fr.dynamx.core.common.physics.terrain.chunk.ChunkLoadingTicket;
import fr.dynamx.core.common.physics.terrain.chunk.ChunkState;
import fr.dynamx.core.common.physics.terrain.chunk.ChunkTerrain;
import fr.dynamx.core.utils.VerticalChunkPos;
import fr.dynamx.core.utils.debug.ChunkGraph;
import fr.dynamx.core.utils.debug.Profiler;
import fr.dynamx.core.utils.optimization.HashMapPool;
import fr.dynamx.core.utils.optimization.PooledHashMap;
import fr.dynamx.forge.DynamXConfig;
import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.ArrayList;

public class MessageQueryChunks implements IDnxPacket {
    private PooledHashMap<VerticalChunkPos, byte[]> requests;

    public MessageQueryChunks() {
    }

    public MessageQueryChunks(PooledHashMap<VerticalChunkPos, byte[]> requests) {
        this.requests = requests;
    }

    @Override
    public EnumNetworkType getPreferredNetwork() {
        return EnumNetworkType.VANILLA_TCP;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        int size = buf.readInt();
        requests = HashMapPool.get();
        for (int i = 0; i < size; i++) {
            requests.put(new VerticalChunkPos(buf.readInt(), buf.readInt(), buf.readInt()), new byte[]{buf.readByte(), buf.readByte()});
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(requests.size());
        requests.forEach((pos, dataType) -> {
            buf.writeInt(pos.x);
            buf.writeInt(pos.y);
            buf.writeInt(pos.z);
            buf.writeByte(dataType[0]);
            buf.writeByte(dataType[1]);
        });
        requests.release();
    }

    public static class Handler implements IMessageHandler<MessageQueryChunks, IDnxPacket> {
        private void processLoadedElements(MessageContext ctx, VerticalChunkPos pos, byte[] dataType, ChunkTerrain terrainElements) {
            if (terrainElements == null) {
                PooledHashMap<VerticalChunkPos, byte[]> empty = HashMapPool.get();
                empty.put(pos, dataType);
                DynamXContext.getNetwork().sendToClientFromOtherThread(new MessageQueryChunks(empty), EnumPacketTarget.PLAYER, ctx.getServerHandler().player);
            } else {
                ArrayList<ITerrainElement> dest = new ArrayList<>(terrainElements.getElements());
                if (dataType[0] == 0) {
                    dest.addAll(terrainElements.getPersistentElements());
                }
                DynamXContext.getNetwork().sendToClientFromOtherThread(new MessageChunkData(pos, dataType, dest), EnumPacketTarget.PLAYER, ctx.getServerHandler().player);
            }
        }

        private void loadChunk(MessageContext ctx, ITerrainManager terrainManager, VerticalChunkPos pos, byte[] data) {
            ChunkLoadingTicket ticket = terrainManager.getTicket(pos);
            if (ticket.getStatus() == ChunkState.LOADING && ticket.getPriority() == ChunkLoadingTicket.TicketPriority.LOW) {
                ticket.getLoadedCallback().thenAccept((collisions -> {
                    if (ticket.getPriority() != ChunkLoadingTicket.TicketPriority.LOW) {
                        if (ticket.getStatus() == ChunkState.LOADED && ticket.getCollisions() != null) {
                            ChunkGraph.addToGrah(pos, ChunkGraph.ChunkActions.NETWORK_SEND, ChunkGraph.ActionLocation.NETWORK, ticket.getCollisions(), ticket + " => SEND AFTER EXTERNAL LOADING");
                            processLoadedElements(ctx, pos, data, ticket.getCollisions().getElements());
                        }
                        return;
                    }
                    //If it stills low (not loaded at another location)
                    if (!terrainManager.subscribeToChunk(pos, ChunkLoadingTicket.TicketPriority.MEDIUM, Profiler.get())) {
                        if (DynamXConfig.enableDebugTerrainManager) {
                            DynamXMain.log.error("0x57 Failed to load chunk {}, for client {}: chunk not loaded in vanilla Minecraft. But chunk loaded with LOW priority?", ticket, ctx.getServerHandler().player.getName());
                        }
                        return;
                    }
                    ticket.getLoadedCallback().whenComplete((collisions2, e) -> {
                        if (collisions2 != null) {
                            ChunkGraph.addToGrah(pos, ChunkGraph.ChunkActions.NETWORK_SEND, ChunkGraph.ActionLocation.NETWORK, collisions2, ticket + " => WAIT FOR MEDIUM_AFTER_LOW LOAD (" + data[0] + ")");
                            processLoadedElements(ctx, pos, data, collisions2.getElements());
                        } else if (e != null) {
                            DynamXMain.log.error("0x54 Failed to load chunk {}, for client {}", ticket, ctx.getServerHandler().player.getName(), e);
                        }
                        terrainManager.unsubscribeFromChunk(pos);
                    });
                })).exceptionally(e -> {
                    DynamXMain.log.error("0x51 Failed to mark chunk {} for load, for client {}", ticket, ctx.getServerHandler().player.getName(), e);
                    return null;
                });
                return;
            }
            // Here, we now the chunk isn't yet loaded
            final boolean subscribe = ticket.getStatus() != ChunkState.LOADING;
            if (subscribe) {
                if (!terrainManager.subscribeToChunk(pos, ChunkLoadingTicket.TicketPriority.MEDIUM, Profiler.get())) {
                    if (DynamXConfig.enableDebugTerrainManager) {
                        DynamXMain.log.error("0x56 Failed to load chunk {}, for client {}: chunk not loaded in vanilla Minecraft.", ticket, ctx.getServerHandler().player.getName());
                    }
                    return;
                }
            }
            if (ticket.getLoadedCallback() != null) {
                ticket.getLoadedCallback().whenComplete((collisions2, e) -> {
                    if (collisions2 != null) {
                        ChunkGraph.addToGrah(pos, ChunkGraph.ChunkActions.NETWORK_SEND, ChunkGraph.ActionLocation.NETWORK, collisions2, ticket + " => WAIT FOR LOAD (" + data[0] + ") Subscribed: " + subscribe);
                        processLoadedElements(ctx, pos, data, collisions2.getElements());
                    } else if (e != null) {
                        DynamXMain.log.error("0x55 Failed to load chunk {}, for client {}", ticket, ctx.getServerHandler().player.getName(), e);
                    }
                    if (subscribe) {
                        terrainManager.unsubscribeFromChunk(pos);
                    }
                });
                return;
            }
            if (subscribe) {
                terrainManager.unsubscribeFromChunk(pos);
            }
            if (!DynamXConfig.ignoreDangerousTerrainErrors) {
                throw new IllegalStateException("Ticket " + ticket + " has no loading callback, but it should be loading i think. 0x10301. Subscribe: " + subscribe + ".");
            }
            DynamXMain.log.fatal("[IgnoredDangerousTerrainError] Ticket {} has no loading callback, but it should be loading i think. 0x10301. Subscribe: {}.", ticket, subscribe);
        }

        @SideOnly(Side.CLIENT)
        private void onMessageClient(MessageQueryChunks message) {
            IPhysicsWorld physicsWorld = DynamXContext.getPhysicsWorld(ClientEventHandler.MC.world);
            if (physicsWorld != null) {
                message.requests.forEach((pos, dataType) -> ((RemoteTerrainCache) physicsWorld.getTerrainManager().getCache()).receiveChunkData(pos, dataType[0], dataType[1], null));
                message.requests.release();
            }
        }

        @Override
        public IDnxPacket onMessage(MessageQueryChunks message, MessageContext ctx) {
            if (!ctx.side.isServer()) {
                onMessageClient(message);
                return null;
            }
            PooledHashMap<VerticalChunkPos, byte[]> emptyGuys = HashMapPool.get();
            PooledHashMap<VerticalChunkPos, byte[]> boysToLoad = HashMapPool.get();
            IPhysicsWorld physicsWorld = DynamXContext.getPhysicsWorld(ctx.getServerHandler().player.world);
            ITerrainManager terrainManager = physicsWorld.getTerrainManager();
            message.requests.forEach((pos, data) -> {
                byte dataType = data[0];
                if (dataType == 0 || dataType == 1) {
                    ChunkLoadingTicket ticket = terrainManager.getTicket(pos);
                    if (ticket.getStatus() == ChunkState.LOADED) {
                        ChunkGraph.addToGrah(pos, ChunkGraph.ChunkActions.NETWORK_SEND, ChunkGraph.ActionLocation.NETWORK, ticket.getCollisions(), ticket + " => DIRECT SEND (" + dataType + ")");
                        processLoadedElements(ctx, pos, data, ticket.getCollisions().getElements());
                    } else {
                        boysToLoad.put(pos, data);
                    }
                } else if (dataType == 2) {
                    byte[] dt = ((FileTerrainCache) terrainManager.getCache()).getSlopesFile().getRawChunkData(pos);
                    if (dt == null) {
                        ChunkGraph.addToGrah(pos, ChunkGraph.ChunkActions.NETWORK_SEND, ChunkGraph.ActionLocation.NETWORK, null, "Empty guy (" + dataType + ")");
                        emptyGuys.put(pos, data);
                    } else {
                        //Don't use reply (return) system, because it may interfere with packet sending (weird bugs seen, maybe due to Mohist)
                        ChunkGraph.addToGrah(pos, ChunkGraph.ChunkActions.NETWORK_SEND, ChunkGraph.ActionLocation.NETWORK, null, "Slope data only (" + dataType + ")");
                        DynamXContext.getNetwork().sendToClientFromOtherThread(new MessageChunkData(pos, data, dt), EnumPacketTarget.PLAYER, ctx.getServerHandler().player);
                    }
                }
            });
            message.requests.release();
            if (!emptyGuys.isEmpty()) {
                //Copy the map for packet sending
                //Don't use reply (return) system, because it may interfere with packet sending (weird bugs seen, maybe due to Mohist)
                DynamXContext.getNetwork().sendToClientFromOtherThread(new MessageQueryChunks(emptyGuys), EnumPacketTarget.PLAYER, ctx.getServerHandler().player);
            } else {
                emptyGuys.release();
            }
            if (boysToLoad.isEmpty()) {
                boysToLoad.release();
                return null;
            }
            physicsWorld.schedule(() -> { //Be sync with physics/terrain thread
                try {
                    boysToLoad.forEach((ticket, data) -> loadChunk(ctx, terrainManager, ticket, data));
                } finally {
                    boysToLoad.release();
                }
            });
            return null;
        }
    }
}
