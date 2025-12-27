package fr.dynamx.core.client;

import fr.dynamx.api.physics.IPhysicsWorld;
import fr.dynamx.core.client.handlers.ClientEventHandler;
import fr.dynamx.core.client.network.ClientPhysicsEntitySynchronizer;
import fr.dynamx.core.client.sound.DynamXSoundHandler;
import fr.dynamx.core.common.CommonProxy;
import fr.dynamx.core.common.DynamXContext;
import fr.dynamx.core.common.DynamXMain;
import fr.dynamx.core.common.entities.PhysicsEntity;
import fr.dynamx.core.common.network.sync.PhysicsEntitySynchronizer;
import fr.dynamx.core.common.network.sync.SPPhysicsEntitySynchronizer;
import fr.dynamx.core.common.physics.entities.AbstractEntityPhysicsHandler;
import fr.dynamx.core.common.physics.world.BuiltinThreadedPhysicsWorld;
import fr.hermes.api.mc.entities.HmClientPlayerEntity;
import fr.hermes.api.mc.world.HmClientWorld;
import fr.hermes.api.mc.world.HmServerWorld;
import fr.hermes.api.mc.world.HmWorld;
import fr.hermes.api.mod.HermesPlatform;
import fr.hermes.api.mod.HermesUtils;
import fr.hermes.api.mod.HermesUtilsClient;

public class ClientProxy extends CommonProxy {
    public static DynamXSoundHandler SOUND_HANDLER = new DynamXSoundHandler();

    public ClientProxy() {
        DynamXContext.initObjModelRegistry();
    }

    @Override
    public boolean isDedicatedServer() {
        return false;
    }

    @Override
    public void scheduleTask(HmWorld mcWorld, Runnable task) {
        if (mcWorld.hm$isClient()) {
            ((HmClientWorld) mcWorld).hm$getMinecraftClient().hm$addScheduledTask(task);
        } else {
            ((HmServerWorld) mcWorld).hm$getServer().hm$addScheduledTask(task);
        }
    }

    @Override
    public void preInit() {
        super.preInit();

        DynamXContext.getDxModelRegistry().onPackInfosReloaded();
        ((HermesUtilsClient) DynamXMain.getInstance().getMod().getUtils()).registerMinecraftRenderingHandlers();
    }

    @Override
    public void completeInit() {
        super.completeInit();
        // TODO idk if needed SplashProgress.pause();
        //try {
        DynamXContext.getDxModelRegistry().uploadVAOs();
        //} finally {
        // SplashProgress.resume();
        //}
    }

    @Override
    public HmClientWorld getClientWorld() {
        return HermesPlatform.getInstance().getClient().hm$getWorld();
    }

    @Override
    public boolean shouldUseBulletSimulation(HmWorld world) {
        return super.shouldUseBulletSimulation(world) && world.hm$isClient();
    }

    @Override
    public <T extends AbstractEntityPhysicsHandler<?, ?>> PhysicsEntitySynchronizer<? extends PhysicsEntity<T>> getNetHandlerForEntity(PhysicsEntity<T> tPhysicsEntity) {
        if (tPhysicsEntity.getHmWorld().hm$isClient()) {
            if (HermesPlatform.getInstance().getServer() != null)
                return new SPPhysicsEntitySynchronizer<>(tPhysicsEntity, true);
            else
                return new ClientPhysicsEntitySynchronizer<>(tPhysicsEntity);
        }
        return super.getNetHandlerForEntity(tPhysicsEntity);
    }

    @Override
    public int getTickTime() {
        return HermesPlatform.getInstance().getClient().hm$getPlayer().getTicksExisted();
    }

    @Override
    public boolean ownsSimulation(PhysicsEntity<?> entity) {
        //TODO NEW SYNC CLEAN THIS
        if (entity.getSynchronizer().getSimulationHolder().ownsPhysics(entity.getHmWorld().hm$isClient())) {
            return true;
        }
        HmClientPlayerEntity player = ClientEventHandler.MC.hm$getPlayer();
        if (entity.getHmWorld().hm$isClient() && player.getRidingEntity() instanceof PhysicsEntity
                && ((PhysicsEntity<?>) player.getRidingEntity()).getSynchronizer().getSimulationHolder().ownsPhysics(true)) {
            return true;
        }
        return player != null && DynamXContext.getPlayerPickingObjects().containsKey(player.getEntityId()) &&
                DynamXContext.getPlayerPickingObjects().get(player.getEntityId()) == entity.getEntityId();
        //on client side : true if the player is driving a vehicle (in any entity)
    }

    @Override
    public void initPhysicsWorld(HmWorld world) {
        if (DynamXContext.getPhysicsWorldPerDimensionMap().containsKey(world.hm$getDimension())) {
            // connecting to another server (e.g. with bungeecoord) : unload the previous world
            DynamXMain.log.info("Duplicate world load detected. Are using BungeeCoord ? Unloading old world.");
            IPhysicsWorld physicsWorld = DynamXContext.getPhysicsWorld(world);
            if (physicsWorld != null) {
                DynamXMain.log.debug("Clearing current physics world...");
                physicsWorld.clearAll();
                DynamXContext.getPlayerToCollision().clear();
            } else {
                throw new IllegalStateException("Physics world loaded but not found. Dim: " + world.hm$getDimension() + " World: " + world);
            }
        }
        DynamXContext.getPhysicsWorldPerDimensionMap().put(world.hm$getDimension(), new BuiltinThreadedPhysicsWorld(world, !ClientEventHandler.MC.hm$isSingleplayer()));
    }

    @Override
    public void schedulePacksInit(HermesUtils utils) {
        ((HermesUtilsClient) utils).initializeDynamXPacks();
    }
}
