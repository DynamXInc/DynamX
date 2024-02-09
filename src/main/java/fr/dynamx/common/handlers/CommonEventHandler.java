package fr.dynamx.common.handlers;

import com.jme3.math.Vector3f;
import fr.dynamx.api.contentpack.object.IInfoOwner;
import fr.dynamx.api.contentpack.object.render.IResourcesOwner;
import fr.dynamx.api.entities.IModuleContainer;
import fr.dynamx.api.events.VehicleEntityEvent;
import fr.dynamx.api.network.EnumPacketTarget;
import fr.dynamx.api.physics.IPhysicsWorld;
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
import fr.dynamx.common.entities.modules.movables.PickingObjectHelper;
import fr.dynamx.common.items.DynamXItemRegistry;
import fr.dynamx.common.items.tools.ItemSlopes;
import fr.dynamx.common.network.packets.MessageHandleExplosion;
import fr.dynamx.common.network.packets.MessageSyncConfig;
import fr.dynamx.common.network.sync.MessageSeatsSync;
import fr.dynamx.common.physics.player.PlayerPhysicsHandler;
import fr.dynamx.server.network.ServerPhysicsSyncManager;
import fr.dynamx.utils.DynamXConfig;
import fr.dynamx.utils.DynamXConstants;
import fr.dynamx.utils.client.ContentPackUtils;
import fr.dynamx.utils.optimization.QuaternionPool;
import fr.dynamx.utils.optimization.Vector3fPool;
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
            DynamXContext.getNetwork().sendToClient(new MessageSyncConfig(false, DynamXConfig.mountedVehiclesSyncTickRate, ContentPackLoader.getBlocksGrip(), ContentPackLoader.slopes, ContentPackLoader.SLOPES_LENGTH, ContentPackLoader.PLACE_SLOPES, DynamXContext.getPhysicsSimulationMode(Side.CLIENT), event.player.getEntityId()), EnumPacketTarget.PLAYER, (EntityPlayerMP) event.player);
        }
    }

    @SubscribeEvent
    public void onDisconnect(net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedOutEvent event) {
        EntityPlayer player = event.player;
        if (FMLCommonHandler.instance().getSide().isServer()) {
            ServerPhysicsSyncManager.onDisconnect(player);
            DynamXContext.getWalkingPlayers().remove(player);
        }
        if (DynamXContext.getPlayerPickingObjects().containsKey(player.getEntityId()))
            PickingObjectHelper.handlePlayerDisconnection(player);
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

    /**
     * Marks the physics terrain dirty and schedule a new computation <br>
     * Don't abuse as it may create some lag
     *
     * @param world The world
     * @param pos   The modified position. The corresponding chunk will be reloaded
     */
    public static void onBlockChange(World world, BlockPos pos) {
        if ((!world.isRemote || FMLCommonHandler.instance().getMinecraftServerInstance() != null)) {
            IPhysicsWorld physicsWorld = DynamXContext.getPhysicsWorld(world);
            if (physicsWorld != null)
                physicsWorld.getTerrainManager().onBlockChange(world, pos);
        }
    }

    @SubscribeEvent
    public void onExplosion(ExplosionEvent.Detonate event) {
        // Explosion effect
        Vector3f explosionPosition = new Vector3f((float) event.getExplosion().getPosition().x,
                (float) event.getExplosion().getPosition().y, (float) event.getExplosion().getPosition().z);
        DynamXContext.getNetwork().sendToClient(new MessageHandleExplosion(explosionPosition, event.getAffectedEntities()), EnumPacketTarget.ALL);
    }

    @SubscribeEvent
    public void onWorldLoad(WorldEvent.Load event) {
        World world = event.getWorld();
        world.addEventListener(new DynamXWorldListener());
        if (event.getWorld().isRemote || FMLCommonHandler.instance().getMinecraftServerInstance().isDedicatedServer()) {
            DynamXMain.proxy.initPhysicsWorld(event.getWorld());
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
        if (DynamXMain.proxy.shouldUseBulletSimulation(e.getWorld())) {
            DynamXContext.getPhysicsWorld(e.getWorld()).schedule(() -> DynamXContext.getPhysicsWorld(e.getWorld()).getTerrainManager().onChunkUnload(e));
        }
    }

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload e) {
        try {
            IPhysicsWorld physicsWorld = DynamXContext.getPhysicsWorld(e.getWorld());
            if (physicsWorld != null && physicsWorld.ownsWorld(e.getWorld())) {
                physicsWorld.clearAll();
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
        if (!(e.player.getRidingEntity() instanceof BaseVehicleEntity) && DynamXContext.getPhysicsWorld(e.player.world) != null && !e.player.isDead) {
            if (!DynamXContext.getPlayerToCollision().containsKey(e.player) && DynamXPhysicsWorldBlacklistApi.isBlacklisted(e.player))
                return;
            Vector3fPool.openPool();
            QuaternionPool.openPool();
            if (!DynamXContext.getPlayerToCollision().containsKey(e.player)) {
                PlayerPhysicsHandler playerPhysicsHandler = new PlayerPhysicsHandler(e.player);
                DynamXContext.getPlayerToCollision().put(e.player, playerPhysicsHandler);
                playerPhysicsHandler.addToWorld();
            }
            DynamXContext.getPlayerToCollision().get(e.player).update(e.player.world);
            Vector3fPool.closePool();
            QuaternionPool.closePool();
        }
    }

    @SubscribeEvent
    public void onVehicleMount(VehicleEntityEvent.PlayerMount e) {
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
                            ContentPackUtils.createBlockJson((IResourcesOwner) block, block.getInfo(), DynamXMain.resourcesDirectory);
                        }
                    }
                }
            }
        }
    }
}
