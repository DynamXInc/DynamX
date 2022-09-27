package fr.dynamx.common.network.packets;

import fr.dynamx.api.network.EnumNetworkType;
import fr.dynamx.api.network.EnumPacketTarget;
import fr.dynamx.api.network.IDnxPacket;
import fr.dynamx.api.physics.terrain.ITerrainElement;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.DynamXMain;
import fr.dynamx.common.physics.terrain.cache.FileTerrainCache;
import fr.dynamx.common.physics.terrain.cache.RemoteTerrainCache;
import fr.dynamx.common.physics.terrain.chunk.ChunkLoadingTicket;
import fr.dynamx.common.physics.terrain.chunk.ChunkState;
import fr.dynamx.common.physics.terrain.chunk.ChunkTerrain;
import fr.dynamx.utils.VerticalChunkPos;
import fr.dynamx.utils.debug.Profiler;
import fr.dynamx.utils.optimization.HashMapPool;
import fr.dynamx.utils.optimization.PooledHashMap;
import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

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
        private void processElements(MessageContext ctx, VerticalChunkPos pos, byte[] dataType, ChunkTerrain terrainElements) {
            //System.out.println("======> J'accepte "+pos+" "+terrainElements);
            if (terrainElements == null) {
                PooledHashMap<VerticalChunkPos, byte[]> empty = HashMapPool.get();
                empty.put(pos, dataType);
                DynamXContext.getNetwork().sendToClientFromOtherThread(new MessageQueryChunks(empty), EnumPacketTarget.PLAYER, ctx.getServerHandler().player);
            } else {
                ArrayList<ITerrainElement> dest = new ArrayList<>(terrainElements.getElements());
                if (dataType[0] == 0)
                    dest.addAll(terrainElements.getPersistentElements());
                DynamXContext.getNetwork().sendToClientFromOtherThread(new MessageChunkData(pos, dataType, dest), EnumPacketTarget.PLAYER, ctx.getServerHandler().player);
            }
        }

        @Override
        public IDnxPacket onMessage(MessageQueryChunks message, MessageContext ctx) {
            if (ctx.side.isServer()) {
                PooledHashMap<VerticalChunkPos, byte[]> emptyGuys = HashMapPool.get();
                PooledHashMap<ChunkLoadingTicket, byte[]> boysToLoad = HashMapPool.get();
                message.requests.forEach((pos, data) -> {
                    byte dataType = data[0];
                    if (dataType == 0 || dataType == 1) {
                        ChunkLoadingTicket ticket = DynamXContext.getPhysicsWorld().getTerrainManager().getTicket(pos);
                        if (ticket.getStatus() == ChunkState.LOADED) {
                            // System.out.println("Already loaded "+ticket);
                            processElements(ctx, pos, data, ticket.getCollisions().getElements());
                        } else {
                            boysToLoad.put(ticket, data);
                        }
                    } else if (dataType == 2) {
                        byte[] dt = ((FileTerrainCache) DynamXContext.getPhysicsWorld().getTerrainManager().getCache()).getSlopesFile().getRawChunkData(pos);
                        //System.out.println("Found "+dt+" at "+pos);
                        if (dt == null) {
                            emptyGuys.put(pos, data);
                        } else {
                            DynamXContext.getNetwork().sendToClientFromOtherThread(new MessageChunkData(pos, data, dt), EnumPacketTarget.PLAYER, ctx.getServerHandler().player);
                        }
                    }
                });
                if (!emptyGuys.isEmpty()) {
                    //Copy the map for packet sending
                    DynamXContext.getNetwork().sendToClientFromOtherThread(new MessageQueryChunks(emptyGuys), EnumPacketTarget.PLAYER, ctx.getServerHandler().player);
                } else {
                    emptyGuys.release();
                }
                if (!boysToLoad.isEmpty()) {
                    DynamXContext.getPhysicsWorld().schedule(() -> { //Be sync with physics/terrain thread
                        boysToLoad.forEach((ticket, data) -> {
                            if (ticket.getStatus() != ChunkState.LOADING) {
                                DynamXContext.getPhysicsWorld().getTerrainManager().subscribeToChunk(ticket.getPos(), ChunkLoadingTicket.TicketPriority.MEDIUM, Profiler.get());
                            } else if (ticket.getStatus() == ChunkState.LOADING && ticket.getPriority() == ChunkLoadingTicket.TicketPriority.LOW) {
                                //System.out.println("Other case "+ticket);
                                ticket.getLoadedCallback().thenAccept((collisions -> {
                                    if (ticket.getPriority() == ChunkLoadingTicket.TicketPriority.LOW) { //If it stills low (not loaded at another location)
                                        DynamXContext.getPhysicsWorld().getTerrainManager().subscribeToChunk(ticket.getPos(), ChunkLoadingTicket.TicketPriority.MEDIUM, Profiler.get());
                                        ticket.getLoadedCallback().whenComplete((collisions2, e) -> {
                                            if (collisions2 != null) {
                                                processElements(ctx, ticket.getPos(), data, collisions2.getElements());
                                            } else if (e != null) {
                                                DynamXMain.log.error("0x54 Failed to load chunk " + ticket + ", for client " + ctx.getServerHandler().player.getName(), e);
                                            }
                                        }).exceptionally(e -> {
                                            DynamXMain.log.error("0x52 Failed to send chunk " + ticket + ", for client " + ctx.getServerHandler().player.getName(), e);
                                            return null;
                                        });
                                    }
                                })).exceptionally(e -> {
                                    DynamXMain.log.error("0x51 Failed to mark chunk " + ticket + " for load, for client " + ctx.getServerHandler().player.getName(), e);
                                    return null;
                                });
                                return;
                            }
                            //System.out.println("Other case "+ticket);
                            if (ticket.getLoadedCallback() != null) {
                                ticket.getLoadedCallback().whenComplete((collisions2, e) -> {
                                    if (collisions2 != null) {
                                        processElements(ctx, ticket.getPos(), data, collisions2.getElements());
                                    } else if (e != null) {
                                        DynamXMain.log.error("0x55 Failed to load chunk " + ticket + ", for client " + ctx.getServerHandler().player.getName(), e);
                                    }
                                }).exceptionally(e -> {
                                    DynamXMain.log.error("0x53 Failed to send chunk " + ticket + " to client " + ctx.getServerHandler().player.getName(), e);
                                    boysToLoad.release();
                                    message.requests.release();
                                    return null;
                                });
                            } else {
                                DynamXMain.log.error("Ticket " + ticket + " has no loading callback, but it should be loading i think. 0x10301.");
                            }
                        });
                        boysToLoad.release();
                    });
                } else {
                    boysToLoad.release();
                }
                message.requests.release();
                //Don't use reply (return) system, because it may interfere with packet sending (weird bugs seen, maybe due to Mohist)
            } else if (DynamXContext.getPhysicsWorld() != null) {
                message.requests.forEach((pos, dataType) -> ((RemoteTerrainCache) DynamXContext.getPhysicsWorld().getTerrainManager().getCache()).receiveChunkData(pos, dataType[0], dataType[1], null));
                message.requests.release();
            }
            return null;
        }
    }
}
