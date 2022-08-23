package fr.dynamx.common.handlers;

import com.jme3.math.Vector3f;
import fr.dynamx.api.contentpack.object.IInfoOwner;
import fr.dynamx.api.contentpack.object.render.IResourcesOwner;
import fr.dynamx.api.entities.IModuleContainer;
import fr.dynamx.api.events.VehicleEntityEvent;
import fr.dynamx.api.network.EnumPacketTarget;
import fr.dynamx.api.physics.player.DynamXPhysicsWorldBlacklistApi;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.DynamXMain;
import fr.dynamx.common.blocks.DynamXBlock;
import fr.dynamx.common.capability.DynamXChunkDataProvider;
import fr.dynamx.common.contentpack.ContentPackLoader;
import fr.dynamx.common.contentpack.DynamXObjectLoaders;
import fr.dynamx.common.contentpack.type.objects.BlockObject;
import fr.dynamx.common.entities.BaseVehicleEntity;
import fr.dynamx.common.entities.PhysicsEntity;
import fr.dynamx.common.items.DynamXItemRegistry;
import fr.dynamx.common.items.tools.ItemSlopes;
import fr.dynamx.common.network.packets.MessageSyncConfig;
import fr.dynamx.common.network.sync.MessageSeatsSync;
import fr.dynamx.common.physics.player.PlayerPhysicsHandler;
import fr.dynamx.common.physics.terrain.cache.TerrainFile;
import fr.dynamx.server.network.ServerPhysicsSyncManager;
import fr.dynamx.utils.DynamXConfig;
import fr.dynamx.utils.DynamXConstants;
import fr.dynamx.utils.VerticalChunkPos;
import fr.dynamx.utils.client.ContentPackUtils;
import fr.dynamx.utils.optimization.QuaternionPool;
import fr.dynamx.utils.optimization.Vector3fPool;
import fr.dynamx.utils.physics.DynamXPhysicsHelper;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.event.world.ExplosionEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.registries.IForgeRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static fr.dynamx.common.handlers.TaskScheduler.schedule;

public class CommonEventHandler {

    public static final ResourceLocation CAPABILITY_LOCATION = new ResourceLocation(DynamXConstants.ID, "chunkaabb");

    @SubscribeEvent
    public void attachCapability(AttachCapabilitiesEvent<Chunk> event) {
        event.addCapability(CAPABILITY_LOCATION, new DynamXChunkDataProvider());
    }

    @SubscribeEvent
    public void onLoggedIn(net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent event) {
        if (FMLCommonHandler.instance().getSide().isServer()) {
            DynamXContext.getNetwork().sendToClient(new MessageSyncConfig(false, DynamXConfig.mountedVehiclesSyncTickRate, ContentPackLoader.getBlocksGrip(), ContentPackLoader.slopes, ContentPackLoader.SLOPES_LENGTH, ContentPackLoader.PLACE_SLOPES, DynamXContext.getPhysicsSimulationMode(Side.CLIENT)), EnumPacketTarget.PLAYER, (EntityPlayerMP) event.player);
        }
    }

    @SubscribeEvent
    public void onDisconnect(net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedOutEvent event) {
        if (FMLCommonHandler.instance().getSide().isServer()) {
            ServerPhysicsSyncManager.onDisconnect(event.player);
        }
    }

    @SubscribeEvent
    public void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getTarget() instanceof PhysicsEntity)// && event.getTarget().ticksExisted > 20 && event.getEntityPlayer().getServer().isDedicatedServer()) //If the entity was just spawned, the total sync is done by its net handler, only if we are in multiplayer
        {
            if (event.getTarget().ticksExisted > 20 && event.getEntityPlayer().getServer().isDedicatedServer()) //If the entity was just spawned, the total sync is done by its net handler, only if we are in multiplayer)
            {
                schedule(new TaskScheduler.ResyncItem((PhysicsEntity<?>) event.getTarget(), (EntityPlayerMP) event.getEntityPlayer()));
            } else if (event.getTarget().ticksExisted <= 20) //If we were riding a vehicle, when we span we need to receive our seat : we do that here
            {
                if (event.getTarget() instanceof IModuleContainer.ISeatsContainer) {
                    //System.out.println("Send seat sync : just spawned " + event.getTarget() + " for " + event.getEntityPlayer());
                    schedule(new TaskScheduler.ScheduledTask((short) 10) {
                        @Override
                        public void run() {
                            DynamXContext.getNetwork().sendToClient(new MessageSeatsSync((IModuleContainer.ISeatsContainer) event.getTarget()), EnumPacketTarget.PLAYER, (EntityPlayerMP) event.getEntityPlayer());
                        }
                    });
                }
            }
        } else if (event.getTarget() instanceof EntityPlayer) {
            if (event.getTarget().getRidingEntity() instanceof IModuleContainer.ISeatsContainer) {
                //System.out.println("Send seat sync of entity " + event.getTarget().getRidingEntity() + " ridden by " + event.getTarget());
                schedule(new TaskScheduler.ScheduledTask((short) 10) {
                    @Override
                    public void run() {
                        //The player can dismount in between the 20 ticks delay
                        if (event.getTarget().getRidingEntity() instanceof IModuleContainer.ISeatsContainer) {
                            DynamXContext.getNetwork().sendToClient(new MessageSeatsSync((IModuleContainer.ISeatsContainer) event.getTarget().getRidingEntity()), EnumPacketTarget.PLAYER, (EntityPlayerMP) event.getEntityPlayer());
                        }
                    }
                });
            }
        }
    }

    /* World events */

   /* @SubscribeEvent
    public void onBlockPlace(BlockEvent.EntityPlaceEvent e) {
        onBlockChange(e.getWorld(), e.getPos());
    }

    @SubscribeEvent
    public void onBlockBreak(BlockEvent.BreakEvent e) {
        onBlockChange(e.getWorld(), e.getPos());
    }*/

    private static final ConcurrentHashMap<VerticalChunkPos, Byte> scheduledChunkReload = new ConcurrentHashMap<>();

    /**
     * Marks the physics terrain dirty and schedule a new computation <br>
     * Don't abuse as it may create some lag
     *
     * @param world The world
     * @param pos   The modified position. The corresponding chunk will be reloaded
     */
    public static void onBlockChange(World world, BlockPos pos) {
        if (world.provider.getDimension() == 0 && (!world.isRemote || (DynamXConfig.clientOwnsPhysicsInSolo && FMLCommonHandler.instance().getMinecraftServerInstance() != null))) {
            VerticalChunkPos p = new VerticalChunkPos(pos.getX() >> 4, pos.getY() >> 4, pos.getZ() >> 4);
            if (TerrainFile.ULTIMATEDEBUG)
                System.out.println("Notify " + p + " " + pos + " " + scheduledChunkReload);
            scheduledChunkReload.put(p, (byte) 10);
        }
    }

    private static final List<VerticalChunkPos> remove = new ArrayList<>();

    public static void tickBlockUpdates() {
        for (Map.Entry<VerticalChunkPos, Byte> en : scheduledChunkReload.entrySet()) {
            byte state = en.getValue();
            if (TerrainFile.ULTIMATEDEBUG)
                System.out.println("Exec " + state + " " + scheduledChunkReload);
            if (state == 1) {
                remove.add(en.getKey());
                scheduledChunkReload.remove(en.getKey());
                if (DynamXContext.getPhysicsWorld() != null)
                    DynamXContext.getPhysicsWorld().getTerrainManager().onChunkChanged(en.getKey());
            } else
                en.setValue((byte) (state - 1));
            if (TerrainFile.ULTIMATEDEBUG)
                System.out.println("End : " + scheduledChunkReload);
        }
        if (!remove.isEmpty()) {
            remove.forEach(scheduledChunkReload::remove);
            remove.clear();
        }
    }

    @SubscribeEvent
    public void onExplosion(ExplosionEvent.Detonate event) {
        if (event.getWorld().provider.getDimension() == 0 && !event.getWorld().isRemote) {
            //We mark as dirty all chunks affected by the explosion
            /*List<VerticalChunkPos> poses = new ArrayList<>();
            VerticalChunkPos.Mutable po = new VerticalChunkPos.Mutable();
            for (BlockPos pos : event.getExplosion().getAffectedBlockPositions()) {
                po.setPos(pos.getX() >> 4, pos.getY() >> 4, pos.getZ() >> 4);
                if (!poses.contains(po)) {
                    onBlockChange(event.getWorld(), pos);
                    poses.add(po.toImmutable());
                }
            }*/

            // Explosion effect
            Vector3f explosionPosition = new Vector3f((float) event.getExplosion().getPosition().x,
                    (float) event.getExplosion().getPosition().y, (float) event.getExplosion().getPosition().z);

            event.getAffectedEntities().forEach(entity -> {
                if (entity instanceof PhysicsEntity) {
                    PhysicsEntity<?> physicsEntity = (PhysicsEntity<?>) entity;
                    DynamXPhysicsHelper.createExplosion(physicsEntity, explosionPosition, 10.0D);
                }
            });
        }
    }

    /*@SubscribeEvent
    public void onChunkLoad(ChunkEvent.Load event) {
        ChunkAABB capability = event.getChunk().getCapability(CapaProvider.CHUNK_AABB_CAPABILITY, null);
        DynamXMain.proxy.scheduleTask(event.getWorld(), () -> {
            event.getChunk().getTileEntityMap().forEach((blockPos, tileEntity) -> {
                if(tileEntity instanceof TEDynamXBlock) {
                    DynamXContext.getNetwork().sendToClient(new MessageSetBlockAABB(blockPos, ((TEDynamXBlock) tileEntity).computeBoundingBox().offset(blockPos)), EnumPacketTarget.PLAYER);
                }
            });
        });
    }*/

    @SubscribeEvent
    public void onChunkUnload(ChunkEvent.Unload e) {
        if (e.getWorld().provider.getDimension() == 0 && DynamXMain.proxy.shouldUseBulletSimulation(e.getWorld())) {
            DynamXContext.getPhysicsWorld().schedule(() -> DynamXContext.getPhysicsWorld().getTerrainManager().onChunkUnload(e));
        }
    }

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload e) {
        try {
            if (DynamXContext.getPhysicsWorld() != null && DynamXContext.getPhysicsWorld().ownsWorld(e.getWorld())) {
                DynamXContext.getPhysicsWorld().clearAll();
                DynamXContext.getPlayerToCollision().clear();
            }
        } catch (Exception ex) {
            DynamXMain.log.fatal("Error while unloading the physics world", ex);
        }
    }

    /* Interaction events */

    @SubscribeEvent
    public void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getEntity() instanceof EntityPlayer) {
            if (event.getItemStack().getItem() instanceof ItemSlopes) {
                ItemSlopes i = (ItemSlopes) event.getItemStack().getItem();
                if (!event.getEntity().isSneaking()) {
                    Vec3d post = event.getHitVec();
                    Vector3fPool.openPool();
                    i.clickedWith(event.getWorld(), event.getEntityPlayer(), event.getHand(), ItemSlopes.fixPos(event.getWorld(), post));
                    Vector3fPool.closePool();
                } else {
                    //i.clearMemory(event.getWorld(), event.getEntityPlayer(), event.getItemStack());
                }
            }
        }
    }

    @SubscribeEvent
    public void onRightClick(PlayerInteractEvent.RightClickItem event) {
        if (event.getEntity() instanceof EntityPlayer) {
            if (event.getEntity().isSneaking()) {
                if (event.getItemStack().getItem() instanceof ItemSlopes) {
                    ((ItemSlopes) event.getItemStack().getItem()).clearMemory(event.getWorld(), event.getEntityPlayer(), event.getItemStack());
                }
            }
        }
    }

    /* Player collisions and walking players */
    @SubscribeEvent
    public void onTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            /* Set floatingTickCount & vehicleFloatingTickCount to 0 to prevent being kicked because of the "Flying is not enable on this server"*/
            DynamXContext.getWalkingPlayers().forEach((player, physicsEntity) -> {
                if (player instanceof EntityPlayerMP) {
                    ((EntityPlayerMP) player).connection.floatingTickCount = 0;
                    ((EntityPlayerMP) player).connection.vehicleFloatingTickCount = 0;
                }
            });
        }
    }

    //Walking players :


    @SubscribeEvent
    public void onPlayerUpdate(TickEvent.PlayerTickEvent e) {
        if (!(e.player.getRidingEntity() instanceof BaseVehicleEntity) && DynamXContext.getPhysicsWorld() != null && !e.player.isDead) {
            if(!DynamXContext.getPlayerToCollision().containsKey(e.player) && DynamXPhysicsWorldBlacklistApi.isBlacklisted(e.player)) return;
            Vector3fPool.openPool();
            QuaternionPool.openPool();
            if (!DynamXContext.getPlayerToCollision().containsKey(e.player)) {
                PlayerPhysicsHandler playerPhysicsHandler = new PlayerPhysicsHandler(e.player);
                DynamXContext.getPlayerToCollision().put(e.player, playerPhysicsHandler);
                playerPhysicsHandler.addToWorld();
            }
            DynamXContext.getPlayerToCollision().get(e.player).update(DynamXContext.getPhysicsWorld());
            Vector3fPool.closePool();
            QuaternionPool.closePool();
        }
    }

    @SubscribeEvent
    public void onPlayerLoggedOut(net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedOutEvent event) {
        if (DynamXContext.getPlayerToCollision().containsKey(event.player))
            DynamXContext.getPlayerToCollision().get(event.player).removeFromWorld(true);
    }

    @SubscribeEvent
    public void onVehicleMount(VehicleEntityEvent.MountVehicleEntityEvent e) {
        if (DynamXContext.getPlayerToCollision().containsKey(e.getPlayer())) {
            DynamXContext.getPlayerToCollision().get(e.getPlayer()).removeFromWorld(false);
        }
    }

    @SubscribeEvent
    public void onVehicleDismount(VehicleEntityEvent.DismountVehicleEntityEvent e) {
        if (DynamXContext.getPlayerToCollision().containsKey(e.getPlayer())) {
            DynamXContext.getPlayerToCollision().get(e.getPlayer()).addToWorld();
        }
    }

    /* Registry */

    @Mod.EventBusSubscriber(modid = DynamXConstants.ID)
    public static class RegisterObjects {
        @SubscribeEvent
        public static void registerItems(RegistryEvent.Register<Item> event) {
            DynamXItemRegistry.injectItems(event);
        }

        @SubscribeEvent
        public static void registerBlocks(RegistryEvent.Register<Block> event) {
            IForgeRegistry<Block> blocks = event.getRegistry();
            for (IInfoOwner<BlockObject<?>> block : DynamXObjectLoaders.BLOCKS.owners) {
                blocks.register((Block) block);

                if (FMLCommonHandler.instance().getSide().isClient()) {
                    if (block.getInfo().isObj()) {
                        ContentPackUtils.registerBlockWithNoModel((Block) block);
                    } else {
                        ContentPackUtils.registerDynamXBlockStateMapper(block);
                        if (((DynamXBlock<?>) block).createJson()) {
                            ContentPackUtils.createBlockJson((IResourcesOwner) block, block.getInfo(), DynamXMain.resDir);
                        }
                    }
                }
            }
        }
    }
}
