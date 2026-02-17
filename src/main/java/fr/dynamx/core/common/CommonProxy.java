package fr.dynamx.core.common;

import fr.dynamx.core.common.entities.PhysicsEntity;
import fr.dynamx.core.common.handlers.CommonEventHandler;
import fr.dynamx.core.common.network.sync.PhysicsEntitySynchronizer;
import fr.dynamx.core.common.network.sync.SPPhysicsEntitySynchronizer;
import fr.dynamx.core.common.physics.PhysicsTickHandler;
import fr.dynamx.core.common.physics.entities.AbstractEntityPhysicsHandler;
import fr.dynamx.core.common.physics.world.BuiltinPhysicsWorld;
import fr.hermes.api.mc.world.HmClientWorld;
import fr.hermes.api.mc.world.HmServerWorld;
import fr.hermes.api.mc.world.HmWorld;
import fr.hermes.api.platform.HermesPlatform;
import fr.hermes.api.mod.HermesUtils;

public abstract class CommonProxy {
    public void preInit() {
        DynamXMain.getInstance().getMod().getUtils().registerMcObjects();
    }

    public void init() {
        PhysicsTickHandler.register();
        CommonEventHandler.register();
    }

    public void completeInit(){}

    /**
     * @return The client world, if loader
     */
    public HmClientWorld getClientWorld() {
        return null;
    }

    /**
     * @return The server world, if loader
     */
    public HmServerWorld getServerWorld() {
        return HermesPlatform.getInstance().getServer().hm$getWorld();
    }

    /**
     * @return True if the bullet physics engine should be used for the world. Always true except for client single player worlds
     */
    public boolean shouldUseBulletSimulation(HmWorld world) {
        return DynamXContext.getPhysicsWorldPerDimensionMap().containsKey(world.hm$getDimension());
    }

    /**
     * @return The {@link AbstractEntityPhysicsHandler} for the given entity, according to the side and game type (solo or multi)
     */
    public <T extends AbstractEntityPhysicsHandler<?, ?>> PhysicsEntitySynchronizer<? extends PhysicsEntity<T>> getNetHandlerForEntity(PhysicsEntity<T> tPhysicsEntity) {
        return new SPPhysicsEntitySynchronizer<>(tPhysicsEntity, false); //Does not work at all on dedicated servers or in lan games
    }

    /**
     * @return The minecraft server's tick counter
     */
    public int getTickTime() {
        return HermesPlatform.getInstance().getServer().hm$getTickCounter();
    }

    /**
     * @param entity The entity to test
     * @return True if the current side is playing a simulation of this entity
     */
    public abstract boolean ownsSimulation(PhysicsEntity<?> entity);

    /**
     * Schedules the given task in the client or server threads, according to the given world's side
     */
    public abstract void scheduleTask(HmWorld mcWorld, Runnable task);

    /**
     * Creates the physics world
     */
    public void initPhysicsWorld(HmWorld world) {
        if (DynamXContext.getPhysicsWorldPerDimensionMap().containsKey(world.hm$getDimension())) {
            DynamXMain.log.warn("Physics world of {} is already loaded ! Keeping the previously loaded world.", world);
            return;
        }
        DynamXContext.getPhysicsWorldPerDimensionMap().put(world.hm$getDimension(), new BuiltinPhysicsWorld(world, false));
    }

    public abstract void schedulePacksInit(HermesUtils utils);

    public abstract boolean isDedicatedServer();
}