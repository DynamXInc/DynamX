package fr.dynamx.common;

import fr.dynamx.api.network.IDnxNetworkSystem;
import fr.dynamx.api.obj.ObjModelPath;
import fr.dynamx.api.physics.IPhysicsSimulationMode;
import fr.dynamx.api.physics.IPhysicsWorld;
import fr.dynamx.api.physics.IRotatedCollisionHandler;
import fr.dynamx.client.DynamXModelRegistry;
import fr.dynamx.common.entities.PhysicsEntity;
import fr.dynamx.common.handlers.RotatedCollisionHandlerImpl;
import fr.dynamx.common.network.DynamXNetwork;
import fr.dynamx.common.objloader.data.ObjModelData;
import fr.dynamx.common.physics.player.PlayerPhysicsHandler;
import fr.dynamx.common.physics.world.PhysicsSimulationModes;
import fr.dynamx.utils.DynamXUtils;
import lombok.Getter;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Common DynamX variables
 */
public class DynamXContext {
    /**
     * -- GETTER --
     *
     * @return The collision handler for collisions between players and physics entities
     */
    @Getter
    private static final IRotatedCollisionHandler collisionHandler = new RotatedCollisionHandlerImpl();
    /**
     * -- GETTER --
     *
     * @return The current {@link IDnxNetworkSystem} for DynamX packets
     */
    @Getter
    private static IDnxNetworkSystem network;
    @SideOnly(Side.CLIENT)
    private static DynamXModelRegistry objModelRegistry;

    /**
     * -- GETTER --
     *
     * @return The rigid bodies of all players
     */
    @Getter
    private static final Map<EntityPlayer, PlayerPhysicsHandler> playerToCollision = new HashMap<>();
    /**
     * -- GETTER --
     *
     * @return The players walking on the top of entities
     */
    @Getter
    private static final ConcurrentHashMap<EntityPlayer, PhysicsEntity<?>> walkingPlayers = new ConcurrentHashMap<>(0, 0.75f, 2);
    /**
     * -- GETTER --
     *
     * @return A map linking player ids with the entities they are holding
     */
    @Getter
    private static Map<Integer, Integer> playerPickingObjects = new HashMap<>();

    private static final IPhysicsSimulationMode[] physicsSimulationModes = new IPhysicsSimulationMode[]{new PhysicsSimulationModes.FullPhysics(), new PhysicsSimulationModes.FullPhysics()};

    private static final Map<ResourceLocation, ObjModelData> OBJ_MODEL_DATA_CACHE = new HashMap<>();

    private static final Map<Integer, IPhysicsWorld> PHYSICS_WORLD_PER_DIMENSION = new HashMap<>();

    protected static void initNetwork() {
        network = DynamXNetwork.init(FMLCommonHandler.instance().getSide());
    }

    @SideOnly(Side.CLIENT)
    public static void initObjModelRegistry() {
        objModelRegistry = new DynamXModelRegistry();
    }

    /**
     * Use this to avoid manipulating physics on invalid sides
     *
     * @param world The World to test
     * @return True is a {@link IPhysicsWorld} exists for this {@link World} (depends on the side of the world) <br>
     * Always true except for client single player worlds
     */
    public static boolean usesPhysicsWorld(World world) {
        return DynamXMain.proxy.shouldUseBulletSimulation(world);
    }

    /**
     * @return The local physics world
     */
    public static IPhysicsWorld getPhysicsWorld(World world) {
        return getPhysicsWorldPerDimensionMap().get(world.provider.getDimension());
    }

    /**
     * @return The obj model loader
     */
    @SideOnly(Side.CLIENT)
    public static DynamXModelRegistry getObjModelRegistry() {
        return objModelRegistry;
    }

    public static Map<Integer, IPhysicsWorld> getPhysicsWorldPerDimensionMap() {
        return PHYSICS_WORLD_PER_DIMENSION;
    }

    public static void setPlayerPickingObjects(Map<Integer, Integer> playerPickingObjects) {
        DynamXContext.playerPickingObjects = playerPickingObjects;
    }

    /**
     * @param side The side, server for the local physics simulation, client for the remote simulation world <br>
     *             Side.CLIENT is <strong>only</strong> used on dedicated server for the client physics worlds
     * @return The {@link IPhysicsSimulationMode} on the given side
     */
    public static IPhysicsSimulationMode getPhysicsSimulationMode(Side side) {
        return physicsSimulationModes[side.ordinal()];
    }

    /**
     * @param side                  The side, server for the local physics simulation, client for the remote simulation world <br>
     *                              Side.CLIENT is <strong>only</strong> used on dedicated server for the client physics worlds
     * @param physicsSimulationMode The {@link IPhysicsSimulationMode} to set on the given side
     */
    public static void setPhysicsSimulationMode(Side side, IPhysicsSimulationMode physicsSimulationMode) {
        DynamXContext.physicsSimulationModes[side.ordinal()] = physicsSimulationMode;
    }

    public static ObjModelData getObjModelDataFromCache(ObjModelPath objModelPath) {
        if (OBJ_MODEL_DATA_CACHE.containsKey(objModelPath.getModelPath())) {
            return OBJ_MODEL_DATA_CACHE.get(objModelPath.getModelPath());
        } else {
            ObjModelData objModelData = new ObjModelData(DynamXUtils.getModelPath(objModelPath.getPackName(), objModelPath.getModelPath()));
            OBJ_MODEL_DATA_CACHE.put(objModelPath.getModelPath(), objModelData);
            return objModelData;
        }
    }

    public static Map<ResourceLocation, ObjModelData> getObjModelDataCache() {
        return OBJ_MODEL_DATA_CACHE;
    }
}