package fr.dynamx.common;

import fr.aym.acslib.ACsLib;
import fr.aym.acslib.api.services.ErrorTrackingService;
import fr.dynamx.api.network.IDnxNetworkSystem;
import fr.dynamx.api.obj.IObjModelRegistry;
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
    private static IPhysicsWorld physicsWorld;
    private static final IRotatedCollisionHandler collisionHandler = new RotatedCollisionHandlerImpl();
    private static final IDnxNetworkSystem network;

    private static final ErrorTrackingService errorTrackingService = ACsLib.getPlatform().provideService(ErrorTrackingService.class);
    @SideOnly(Side.CLIENT)
    private static IObjModelRegistry objModelRegistry;

    private static final Map<EntityPlayer, PlayerPhysicsHandler> playerToCollision = new HashMap<>();
    private static final ConcurrentHashMap<EntityPlayer, PhysicsEntity<?>> walkingPlayers = new ConcurrentHashMap<>(0, 0.75f, 2);
    private static Map<Integer, Integer> playerPickingObjects = new HashMap<>();

    private static final IPhysicsSimulationMode[] physicsSimulationModes = new IPhysicsSimulationMode[]{new PhysicsSimulationModes.FullPhysics(), new PhysicsSimulationModes.FullPhysics()};

    private static final Map<ResourceLocation, ObjModelData> OBJ_MODEL_DATA_CACHE = new HashMap<>();


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
    public static IPhysicsWorld getPhysicsWorld() {
        return physicsWorld;
    }

    /**
     * @return The collision handler for collisions between players and physics entities
     */
    public static IRotatedCollisionHandler getCollisionHandler() {
        return collisionHandler;
    }

    /**
     * @return The obj model loader
     */
    @SideOnly(Side.CLIENT)
    public static IObjModelRegistry getObjModelRegistry() {
        return objModelRegistry;
    }

    /**
     * @return The rigid bodies of all players
     */
    public static Map<EntityPlayer, PlayerPhysicsHandler> getPlayerToCollision() {
        return playerToCollision;
    }

    /**
     * @return The players walking on the top of entities
     */
    public static ConcurrentHashMap<EntityPlayer, PhysicsEntity<?>> getWalkingPlayers() {
        return walkingPlayers;
    }

    /**
     * @return A map linking player ids with the entities they are holding
     */
    public static Map<Integer, Integer> getPlayerPickingObjects() {
        return playerPickingObjects;
    }

    /**
     * The current {@link ErrorTrackingService} instance
     */
    public static ErrorTrackingService getErrorTracker() {
        return errorTrackingService;
    }

    /**
     * @return The current {@link IDnxNetworkSystem} for DynamX packets
     */
    public static IDnxNetworkSystem getNetwork() {
        return network;
    }

    public static void setPhysicsWorld(IPhysicsWorld physicsWorld) {
        DynamXContext.physicsWorld = physicsWorld;
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

    public static Map<ResourceLocation, ObjModelData> getObjModelDataCache(){
        return OBJ_MODEL_DATA_CACHE;
    }

    static {
        network = DynamXNetwork.init(FMLCommonHandler.instance().getSide());
        if (FMLCommonHandler.instance().getSide().isClient())
            objModelRegistry = new DynamXModelRegistry();
    }
}