package fr.dynamx.core.common.handlers;

import fr.dynamx.api.contentpack.object.IDynamXItem;
import fr.dynamx.api.contentpack.object.render.IResourcesOwner;
import fr.dynamx.api.entities.IModuleContainer;
import fr.dynamx.api.events.VehicleEntityEvent;
import fr.dynamx.api.network.EnumPacketTarget;
import fr.dynamx.api.physics.IPhysicsWorld;
import fr.dynamx.api.physics.player.DynamXPhysicsWorldBlacklistApi;
import fr.dynamx.api.physics.terrain.DynamXTerrainApi;
import fr.dynamx.api.physics.terrain.ITerrainUpdateBehavior;
import fr.dynamx.core.common.DynamXContext;
import fr.dynamx.core.common.DynamXMain;
import fr.dynamx.core.common.blocks.DynamXBlock;
import fr.dynamx.core.common.capability.DynamXChunkDataProvider;
import fr.dynamx.core.common.contentpack.DynamXObjectLoaders;
import fr.dynamx.core.common.contentpack.type.objects.BlockObject;
import fr.dynamx.core.common.entities.PhysicsEntity;
import fr.dynamx.core.common.entities.modules.movables.PickingObjectHelper;
import fr.dynamx.core.common.items.DynamXItemRegistry;
import fr.dynamx.core.common.items.tools.ItemSlopes;
import fr.dynamx.core.common.network.packets.MessageHandleExplosion;
import fr.dynamx.core.common.network.packets.MessageSyncConfig;
import fr.dynamx.core.common.network.sync.MessageSeatsSync;
import fr.dynamx.core.common.physics.player.PlayerPhysicsHandler;
import fr.dynamx.core.server.network.ServerPhysicsSyncManager;
import fr.dynamx.core.utils.DynamXConstants;
import fr.dynamx.core.utils.client.ContentPackUtils;
import fr.dynamx.core.utils.optimization.QuaternionPool;
import fr.dynamx.core.utils.optimization.SubClassPool;
import fr.hermes.api.HmEntityLogicMatcher;
import fr.hermes.api.events.HmEventPhase;
import fr.hermes.api.mc.blocks.HmBlockState;
import fr.hermes.api.mc.entities.HmPlayerEntity;
import fr.hermes.api.mc.events.HmMcServerEvents;
import fr.hermes.api.mc.events.HmPlayerEvents;
import fr.hermes.api.mc.events.HmWorldEvents;
import fr.hermes.api.mc.world.HmWorld;
import fr.hermes.forge.JmeVector3fPool;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.registries.IForgeRegistry;

import java.util.HashMap;
import java.util.Map;

import static fr.dynamx.core.common.handlers.TaskScheduler.schedule;

// TODO COMPLETE CONVERSION TO HERMES EVENTS
public class CommonEventHandler {
    public static final ResourceLocation CAPABILITY_LOCATION = new ResourceLocation(DynamXConstants.ID, "chunkaabb");

    public static final Map<ChunkPos, Map<BlockPos, AxisAlignedBB>> PENDING_CHUNKS_COLLISIONS = new HashMap<>();

    public static void register() {
        HmPlayerEvents.JOIN.register(player -> {
            if (player.hm$getServer().hm$isDedicatedServer()) {
                DynamXContext.getNetwork().sendToClient(new MessageSyncConfig(false, player.getEntityId()), EnumPacketTarget.PLAYER, player);
            }
        });

        HmPlayerEvents.LEAVE.register(player -> {
            if (player.hm$getServer().hm$isDedicatedServer()) {
                ServerPhysicsSyncManager.onDisconnect(player);
                DynamXContext.getWalkingPlayers().remove(player);
            }
            if (DynamXContext.getPlayerPickingObjects().containsKey(player.getEntityId())) {
                PickingObjectHelper.handlePlayerDisconnection(player);
            }
        });

        HmWorldEvents.LOAD.register(world -> {
            if (world.hm$isClient() || FMLCommonHandler.instance().getMinecraftServerInstance().isDedicatedServer()) {
                DynamXMain.getProxy().initPhysicsWorld(world);
            }

            world.hm$addEntityRemovedListener(entityIn -> {
                if (entityIn instanceof HmPlayerEntity) {
                    HmPlayerEntity player = (HmPlayerEntity) entityIn;
                    if (DynamXContext.getPlayerToCollision().containsKey(player)) {
                        DynamXContext.getPlayerToCollision().get(player).removeFromWorld(true, player.hm$getWorld());
                    }
                }
            });
        });

        HmWorldEvents.UNLOAD.register(world -> {
            try {
                IPhysicsWorld physicsWorld = DynamXContext.getPhysicsWorld(world);
                if (physicsWorld != null) {
                    physicsWorld.clearAll();
                    DynamXContext.getPlayerToCollision().clear();
                }
            } catch (Exception ex) {
                DynamXMain.log.fatal("Error while unloading the physics world", ex);
            }
        });

        HmMcServerEvents.TICK.register(phase -> {
            if (phase == HmEventPhase.POST) {
                /* Set floatingTickCount & vehicleFloatingTickCount to 0 to prevent being kicked because of the "Flying is not enable on this server"*/
                DynamXContext.getWalkingPlayers().forEach((player, physicsEntity) -> {
                    if (player instanceof EntityPlayerMP) {
                        ((EntityPlayerMP) player).connection.floatingTickCount = 0;
                        ((EntityPlayerMP) player).connection.vehicleFloatingTickCount = 0;
                    }
                });
            }
        });

        HmPlayerEvents.START_TRACKING.register((player, entity) -> {
            PhysicsEntity<?> physicsEntity = HmEntityLogicMatcher.cast(entity, PhysicsEntity.class);
            if (physicsEntity != null)// && event.getTarget().ticksExisted > 20 && event.getEntityPlayer().getServer().isDedicatedServer()) //If the entity was just spawned, the total sync is done by its net handler, only if we are in multiplayer
            {
                if (entity.hm$getTicksExisted() > 20) { //If the entity was just spawned, the total sync is done by its net handler, only if we are in multiplayer)
                    if (player.hm$getServer().hm$isDedicatedServer()) {
                        schedule(new TaskScheduler.ResyncItem(physicsEntity, player));
                    } else if (physicsEntity.getJointsHandler() != null) { // Resync joints when the entity was unloaded on the client side, but not server side, in singleplayer
                        physicsEntity.getJointsHandler().sync(player);
                    }
                } else { //If we were riding a vehicle, when we span we need to receive our seat : we do that here
                    if (physicsEntity instanceof IModuleContainer.ISeatsContainer && ((IModuleContainer.ISeatsContainer) physicsEntity).hasSeats()) {
                        schedule(new TaskScheduler.ScheduledTask((short) 20) {
                            @Override
                            public void run() {
                                DynamXContext.getNetwork().sendToClient(new MessageSeatsSync((IModuleContainer.ISeatsContainer) physicsEntity), EnumPacketTarget.PLAYER, player);
                                if (physicsEntity.getJointsHandler() != null) {
                                    physicsEntity.getJointsHandler().sync(player);
                                }
                            }
                        });
                    }
                }
            } else if (entity instanceof HmPlayerEntity) {
                if (entity.hm$getRidingEntity() instanceof IModuleContainer.ISeatsContainer && ((IModuleContainer.ISeatsContainer) entity.hm$getRidingEntity()).hasSeats()) {
                    schedule(new TaskScheduler.ScheduledTask((short) 10) {
                        @Override
                        public void run() {
                            //The player can dismount in between the 20 ticks delay
                            if (entity.hm$getRidingEntity() instanceof IModuleContainer.ISeatsContainer && ((IModuleContainer.ISeatsContainer) entity.hm$getRidingEntity()).hasSeats()) {
                                DynamXContext.getNetwork().sendToClient(new MessageSeatsSync((IModuleContainer.ISeatsContainer) entity.hm$getRidingEntity()), EnumPacketTarget.PLAYER, player);
                            }
                        }
                    });
                }
            }
        });

        HmWorldEvents.CHUNK_UNLOAD.register(chunk -> {
            if (DynamXMain.getProxy().shouldUseBulletSimulation(chunk.hm$getWorld())) {
                IPhysicsWorld physicsWorld = DynamXContext.getPhysicsWorld(chunk.hm$getWorld());
                physicsWorld.schedule(() -> physicsWorld.getTerrainManager().onChunkUnload(chunk));
            }
        });

        HmWorldEvents.EXPLOSION_DETONATE.register((world, pos, affectedEntities) -> {
            // Explosion effect
            DynamXContext.getNetwork().sendToClient(new MessageHandleExplosion(pos, affectedEntities), EnumPacketTarget.ALL);
        });

        HmPlayerEvents.RIGHT_CLICK_BLOCK.register((player, hand, pos, hitVec) -> {
            if (player.hm$getHeldItem(hand).hm$getItem() instanceof ItemSlopes) {
                ItemSlopes i = (ItemSlopes) player.hm$getHeldItem(hand).hm$getItem();
                if (!player.hm$isSneaking()) {
                    HmWorld world = player.hm$getWorld();
                    i.clickedWith(world, player, hand, ItemSlopes.fixPos(world, hitVec));
                }
            }
        });

        HmPlayerEvents.RIGHT_CLICK_ITEM.register((player, hand, itemStack) -> {
            if (itemStack.hm$getItem() instanceof ItemSlopes) {
                ItemSlopes i = (ItemSlopes) itemStack.hm$getItem();
                if (!player.hm$isSneaking()) {
                    i.clickedWith(player.hm$getWorld(), player, hand, ItemSlopes.fixPos(player.hm$getWorld(), player.hm$getPosition()));
                }
            }
        });

        HmPlayerEvents.TICK.register((player, phase) -> {
            if (!(player.hm$getRidingEntity() instanceof PhysicsEntity<?>) && DynamXContext.getPhysicsWorld(player.hm$getWorld()) != null && !player.hm$isDead()) {
                if (!DynamXContext.getPlayerToCollision().containsKey(player) && DynamXPhysicsWorldBlacklistApi.isBlacklisted(player)) {
                    return;
                }
                JmeVector3fPool.openPool(SubClassPool.PLAYER_COLL);
                QuaternionPool.openPool(SubClassPool.PLAYER_COLL);
                if (!DynamXContext.getPlayerToCollision().containsKey(player)) {
                    PlayerPhysicsHandler playerPhysicsHandler = new PlayerPhysicsHandler(player);
                    DynamXContext.getPlayerToCollision().put(player, playerPhysicsHandler);
                    playerPhysicsHandler.addToWorld();
                }
                DynamXContext.getPlayerToCollision().get(player).update(player.hm$getWorld());
                JmeVector3fPool.closePool();
                QuaternionPool.closePool();
            }
        });
    }

    // === TODO think about capabilities (this is so Forge specific)

    @SubscribeEvent
    public void attachCapability(AttachCapabilitiesEvent<Chunk> event) {
        event.addCapability(CAPABILITY_LOCATION, new DynamXChunkDataProvider());
    }

    @SubscribeEvent
    public void onChunkLoad(ChunkEvent.Load e) {
        if (PENDING_CHUNKS_COLLISIONS.containsKey(e.getChunk().getPos())) {
            e.getChunk().getCapability(DynamXChunkDataProvider.DYNAMX_CHUNK_DATA_CAPABILITY, null).getBlocksAABB().putAll(PENDING_CHUNKS_COLLISIONS.get(e.getChunk().getPos()));
        }
    }
    // === end

    /* World events */

    /**
     * Marks the physics terrain dirty and schedule a new computation <br>
     * Don't abuse as it may create some lag <br>
     * The updates are filtered by the {@link ITerrainUpdateBehavior}s
     *
     * @param world The world
     * @param pos   The modified position. The corresponding chunk will be reloaded if allowed by the terrain update behaviors.
     */
    public static void onBlockChange(HmWorld world, BlockPos pos, HmBlockState oldState, HmBlockState newState) {
        if (FMLCommonHandler.instance().getMinecraftServerInstance() == null || !DynamXContext.usesPhysicsWorld(world) // If we are on the client, we don't need to update the terrain (the server will notify changes)
                || DynamXTerrainApi.getTerrainUpdateBehavior(world, pos, oldState, newState) == ITerrainUpdateBehavior.Result.IGNORE) {
            return;
        }
        IPhysicsWorld physicsWorld = DynamXContext.getPhysicsWorld(world);
        if (physicsWorld == null) {
            return;
        }
        physicsWorld.getTerrainManager().onBlockChange(world, pos);
    }

    // === TODO DynamX events for walking players ===
    @SubscribeEvent
    public void onVehicleMount(VehicleEntityEvent.EntityMount e) {
        if (DynamXContext.getPlayerToCollision().containsKey(e.getEntityMounted())) {
            DynamXContext.getPlayerToCollision().get(e.getEntityMounted()).removeFromWorld(false, e.getEntityMounted().world);
        }
    }

    @SubscribeEvent
    public void onVehicleDismount(VehicleEntityEvent.EntityDismount e) {
        if (DynamXContext.getPlayerToCollision().containsKey(e.getEntityDismounted())) {
            DynamXContext.getPlayerToCollision().get(e.getEntityDismounted()).addToWorld();
        }
    }
    // === end ===

    /* TODO Registry */

    @Mod.EventBusSubscriber(modid = DynamXConstants.ID)
    public static class RegisterObjects {
        @SubscribeEvent
        public static void registerItems(RegistryEvent.Register<Item> event) {
            DynamXItemRegistry.injectItems(event);
        }

        @SubscribeEvent
        public static void registerBlocks(RegistryEvent.Register<Block> event) {
            IForgeRegistry<Block> blocks = event.getRegistry();
            for (IDynamXItem<BlockObject<?>> block : DynamXObjectLoaders.BLOCKS.owners) {
                blocks.register((Block) block);

                if (FMLCommonHandler.instance().getSide().isClient()) {
                    if (block.getInfo().isDxModel()) {
                        ContentPackUtils.registerBlockWithNoModel((Block) block);
                    } else {
                        ContentPackUtils.registerDynamXBlockStateMapper(block);
                        if (((DynamXBlock<?>) block).createJson()) {
                            ContentPackUtils.createBlockJson((IResourcesOwner) block, block.getInfo(), DynamXMain.getInstance().getResourcesDirectory());
                        }
                    }
                }
            }
        }
    }
}
