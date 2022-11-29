package fr.dynamx.api.network.sync.v3;

import com.jme3.math.Vector3f;
import fr.dynamx.api.entities.IModuleContainer;
import fr.dynamx.api.network.EnumPacketTarget;
import fr.dynamx.api.network.sync.SimulationHolder;
import fr.dynamx.api.network.sync.SyncTarget;
import fr.dynamx.api.network.sync.SynchronizedVariable;
import fr.dynamx.api.network.sync.SynchronizedVariablesRegistry;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.entities.ModularPhysicsEntity;
import fr.dynamx.common.entities.PhysicsEntity;
import fr.dynamx.common.network.sync.MessagePhysicsEntitySync;
import fr.dynamx.common.network.sync.MessageSeatsSync;
import fr.dynamx.common.physics.player.WalkingOnPlayerController;
import fr.dynamx.server.network.ServerPhysicsSyncManager;
import fr.dynamx.utils.debug.Profiler;
import fr.dynamx.utils.optimization.HashMapPool;
import fr.dynamx.utils.optimization.PooledHashMap;
import lombok.Getter;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * TODO UPDATE DOC
 * Base net handler (responsible to sync the different clients and the server) of a {@link PhysicsEntity}
 */
public abstract class PhysicsEntitySynchronizer<T extends PhysicsEntity<?>> {
    /**
     * All {@link SynchronizedVariable} to sync with the clients (or with the server if we are the driver)
     */
    private ConcurrentHashMap<Integer, SynchronizedEntityVariable<?>> synchronizedVariables = new ConcurrentHashMap<>(3, 0.75f, 2);
    /**
     * All {@link SynchronizedVariable} received from the network, and that need to be synced with the entity
     */
    private ConcurrentHashMap<Integer, SynchronizedEntityVariableSnapshot<?>> receivedVariables = new ConcurrentHashMap<>(3, 0.75f, 2);

    /**
     * The {@link SimulationHolder} of this entity
     */
    private SimulationHolder simulationHolder = getDefaultSimulationHolder();
    @Getter
    private EntityPlayer simulationPlayerHolder;

    /**
     * The entity that we sync
     */
    @Getter
    protected final T entity;

    public PhysicsEntitySynchronizer(T entity) {
        this.entity = entity;
    }

    public ConcurrentHashMap<Integer, SynchronizedEntityVariable<?>> getSynchronizedVariables() {
        return synchronizedVariables;
    }

    public ConcurrentHashMap<Integer, SynchronizedEntityVariableSnapshot<?>> getReceivedVariables() {
        return receivedVariables;
    }

    //TODO ANNOTATION SYSTEM
    public void registerVariable(ResourceLocation name, SynchronizedEntityVariable<?> variable) {
        Integer id = SynchronizedEntityVariableRegistry.getSyncVarRegistry().get(name);
        if (id == null) { //TODO CLEAN
            throw new IllegalArgumentException("not registered " + name + " " + variable);
        }
        if (synchronizedVariables.containsKey(id))
            return;
        synchronizedVariables.put(id, variable);
        receivedVariables.put(id, new SynchronizedEntityVariableSnapshot(variable.getSerializer(), variable.get()));
    }

    /**
     * Called before ticking the physics world (can be in an external thread)
     *
     * @param profiler The current profiler
     */
    public abstract void onPrePhysicsTick(Profiler profiler);

    /**
     * Called after ticking the physics world (can be in an external thread)
     *
     * @param profiler The current profiler
     */
    public abstract void onPostPhysicsTick(Profiler profiler);

    /**
     * Called when a driver mounts on this entity, useful to change the {@link SimulationHolder}
     */
    public abstract void onPlayerStartControlling(EntityPlayer player, boolean addControllers);

    /**
     * Called when a driver dismounts from this entity, useful to change the {@link SimulationHolder}
     */
    public abstract void onPlayerStopControlling(EntityPlayer player, boolean removeControllers);

    /**
     * Called when the state of a walking player changes
     *
     * @param playerId The player's entity id
     * @param offset   The pos of the player, relative to the vehicle with 0 rotation
     * @param face     The collided face id (see {@link net.minecraft.util.EnumFacing})
     * @see WalkingOnPlayerController
     */
    public void onWalkingPlayerChange(int playerId, Vector3f offset, byte face) {
    }

    /**
     * @return true If the world instance on the other side uses physic (false if you are in single-player)
     */
    public boolean doesOtherSideUsesPhysics() {
        return true;
    }

    /**
     * @return The {@link SimulationHolder} responsible of this entity
     */
    public SimulationHolder getSimulationHolder() {
        return simulationHolder;
    }

    /**
     * @return The default simulation holder for this entity (when there is no driver) on this type of network
     */
    public SimulationHolder getDefaultSimulationHolder() {
        return SimulationHolder.SERVER;
    }

    /**
     * Sets the {@link SimulationHolder} responsible of this entity, updates output sync vars depending on the holder and clears input sync vars
     *
     * @param simulationHolder The new simulation holder
     */
    public void setSimulationHolder(SimulationHolder simulationHolder, EntityPlayer simulationPlayerHolder) {
        setSimulationHolder(simulationHolder, simulationPlayerHolder, SimulationHolder.UpdateContext.NORMAL);
    }

    /**
     * Sets the {@link SimulationHolder} responsible of this entity, updates output sync vars depending on the holder and clears input sync vars
     *
     * @param simulationHolder The new simulation holder
     * @param changeContext    The simulation holder update context, changes the affected entities (linked entities, entities in props containers...)
     */
    public void setSimulationHolder(SimulationHolder simulationHolder, EntityPlayer simulationPlayerHolder, SimulationHolder.UpdateContext changeContext) {
        System.out.println("SET HOLD " + this.simulationHolder+" "+simulationHolder+" "+entity+" "+changeContext+" "+simulationPlayerHolder);
        this.simulationHolder = simulationHolder;
        this.simulationPlayerHolder = simulationPlayerHolder;
        if (changeContext != SimulationHolder.UpdateContext.ATTACHED_ENTITIES && entity.getJointsHandler() != null) {
            entity.getJointsHandler().setSimulationHolderOnJointedEntities(simulationHolder, simulationPlayerHolder);
        }
        if (entity instanceof ModularPhysicsEntity) {
            ((ModularPhysicsEntity<?>) entity).getModules().forEach(m -> m.onSetSimulationHolder(simulationHolder, simulationPlayerHolder, changeContext));
        }
        SynchronizedVariablesRegistry.setSyncVarsForContext(entity.world.isRemote ? Side.CLIENT : Side.SERVER, this);
    }

    public void resyncEntity(EntityPlayerMP target) {
        //Force tcp for first sync and resyncs
        DynamXContext.getNetwork().getVanillaNetwork().sendPacket(new MessagePhysicsEntitySync(entity, ServerPhysicsSyncManager.getTime(target), synchronizedVariables, false), EnumPacketTarget.PLAYER, target);
        if (entity instanceof IModuleContainer.ISeatsContainer)
            DynamXContext.getNetwork().sendToClient(new MessageSeatsSync((IModuleContainer.ISeatsContainer) entity), EnumPacketTarget.PLAYER, target);
        if (entity.getJointsHandler() != null)
            entity.getJointsHandler().sync(target);
    }

    public PooledHashMap<Integer, SynchronizedEntityVariable<?>> getVarsToSync(Side fromSide, SyncTarget target) {
        PooledHashMap<Integer, SynchronizedEntityVariable<?>> ret = HashMapPool.get();
        getSynchronizedVariables().forEach((i, s) -> {
            SyncTarget varTarget = s.getSyncTarget(simulationHolder, fromSide);
            if(target.isIncluded(varTarget))
                ret.put(i, s);
        });
        return ret;
    }
}
