package fr.dynamx.common;

import fr.dynamx.api.network.sync.PhysicsEntityNetHandler;
import fr.dynamx.api.physics.IPhysicsWorld;
import fr.dynamx.common.blocks.TEDynamXBlock;
import fr.dynamx.common.entities.PhysicsEntity;
import fr.dynamx.common.handlers.CommonEventHandler;
import fr.dynamx.common.network.SPPhysicsEntityNetHandler;
import fr.dynamx.common.physics.PhysicsTickHandler;
import fr.dynamx.common.physics.entities.AbstractEntityPhysicsHandler;
import fr.dynamx.common.physics.world.BuiltinPhysicsWorld;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.server.FMLServerHandler;

import static fr.dynamx.utils.DynamXConstants.ID;

public abstract class CommonProxy {
    public void preInit() {
        GameRegistry.registerTileEntity(TEDynamXBlock.class, new ResourceLocation(ID + ":dynamxblock"));
    }

    public void init() {
        MinecraftForge.EVENT_BUS.register(new PhysicsTickHandler());
        MinecraftForge.EVENT_BUS.register(new CommonEventHandler());
    }

    /**
     * @return The client world, if loader
     */
    public World getClientWorld() {
        return null;
    }

    /**
     * @return The server world, if loader
     */
    public World getServerWorld() {
        return FMLServerHandler.instance().getServer().getEntityWorld();
    }

    /**
     * @return True if the bullet physics engine should be used for the world. Always true except for client single player worlds
     */
    public boolean shouldUseBulletSimulation(World world) {
        return world.provider.getDimension() == 0;
    }

    /**
     * @return The {@link AbstractEntityPhysicsHandler} for the given entity, according to the side and game type (solo or multi)
     */
    public <T extends AbstractEntityPhysicsHandler<?, ?>> PhysicsEntityNetHandler<? extends PhysicsEntity<T>> getNetHandlerForEntity(PhysicsEntity<T> tPhysicsEntity) {
        return new SPPhysicsEntityNetHandler<>(tPhysicsEntity, Side.SERVER); //Does not work at all on dedicated servers or in lan games
    }

    /**
     * @return The minecraft server's tick counter
     */
    public int getTickTime() {
        return FMLCommonHandler.instance().getMinecraftServerInstance().getTickCounter();
    }

    /**
     * @param entity The entity to test
     * @return True if the current side is playing a simulation of this entity
     */
    public abstract boolean ownsSimulation(PhysicsEntity<?> entity);

    /**
     * Schedules the given task in the client or server threads, according to the given world's side
     */
    public abstract void scheduleTask(World mcWorld, Runnable task);

    /**
     * Creates the client physics world
     */
    public IPhysicsWorld provideClientPhysicsWorld(World world) {
        return null;
    }

    /**
     * Creates the server physics world
     */
    public IPhysicsWorld provideServerPhysicsWorld(World world) {
        return new BuiltinPhysicsWorld(world, false);
    }

    public abstract void schedulePacksInit();
}