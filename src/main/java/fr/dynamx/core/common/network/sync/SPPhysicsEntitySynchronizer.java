package fr.dynamx.core.common.network.sync;

import fr.dynamx.api.entities.modules.IPhysicsModule;
import fr.dynamx.api.entities.modules.IVehicleController;
import fr.dynamx.api.network.sync.ClientEntityNetHandler;
import fr.dynamx.api.network.sync.EntityVariable;
import fr.dynamx.api.network.sync.SimulationHolder;
import fr.dynamx.api.network.sync.SyncTarget;
import fr.dynamx.core.common.DynamXMain;
import fr.dynamx.core.common.entities.BaseVehicleEntity;
import fr.dynamx.core.common.entities.PhysicsEntity;
import fr.dynamx.core.common.network.sync.variables.SynchronizedEntityVariableSnapshot;
import fr.dynamx.core.utils.debug.Profiler;
import fr.dynamx.core.utils.optimization.PooledHashMap;
import fr.hermes.api.HmEntityLogicMatcher;
import fr.hermes.api.mc.entities.HmEntity;
import fr.hermes.api.mc.entities.HmPlayerEntity;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Simplified network handler (there are no packets sent) for single player games
 */
public class SPPhysicsEntitySynchronizer<T extends PhysicsEntity<?>> extends PhysicsEntitySynchronizer<T> implements ClientEntityNetHandler {
    /**
     * The side of this single player entity synchronizer
     */
    private final boolean isClient;
    /**
     * The list of {@link IVehicleController}s loaded on this entity
     */
    private final List<IVehicleController> controllers = new ArrayList<>();

    /**
     * @param entityIn The synchronized entity
     * @param isClient     The side of this single player entity synchronizer
     */
    public SPPhysicsEntitySynchronizer(T entityIn, boolean isClient) {
        super(entityIn);
        this.isClient = isClient;
    }

    /**
     * @return The entity matching this entity, but on the other side (if called on client side, it will return the server side entity, and vice versa)
     */
    public PhysicsEntity<?> getOtherSideEntity() {
        HmEntity other;
        if (!isClient) {
            other = DynamXMain.getProxy().getClientWorld() == null ? null : DynamXMain.getProxy().getClientWorld().hm$getEntityByID(entity.getEntityId());
        } else {
            other = DynamXMain.getProxy().getServerWorld().hm$getEntityByID(entity.getEntityId());
        }
        return HmEntityLogicMatcher.cast(other, PhysicsEntity.class);
    }

    /**
     * Send all changed synchronized variables to the given SyncTarget
     *
     * @param other The synchronized of the matching entity on the other side, matching the sync target
     * @param to    The sync target
     */
    private void sendMyVars(SPPhysicsEntitySynchronizer<T> other, SyncTarget to) {
        PooledHashMap<Integer, EntityVariable<?>> varsToSync = getVarsToSync(isClient, to);
        ByteBuf buf = Unpooled.buffer();
        for (Map.Entry<Integer, EntityVariable<?>> entry : varsToSync.entrySet()) {
            Integer varId = entry.getKey();
            EntityVariable<?> sourceVar = entry.getValue();
            sourceVar.writeValue(buf, false);
            sourceVar.setChanged(false);
            SynchronizedEntityVariableSnapshot<?> targetVar = other.getReceivedVariables().get(varId);
            if (targetVar != null) {
                targetVar.read(buf);
            } else if (other.getEntity().getTicksExisted() > 20) {
                DynamXMain.log.error("Synchronized variable not found " + varId + " " + sourceVar + " " + other.getReceivedVariables() + " on " + entity);
            }
            buf.clear();
        }
        varsToSync.release();
    }

    @Override
    public void onPlayerStartControlling(HmPlayerEntity player, boolean addControllers) {
        if (entity.physicsHandler != null) {
            entity.physicsHandler.setForceActivation(true);
        }
        setSimulationHolder(SimulationHolder.DRIVER_SP, player);
        if (!player.hm$getWorld().hm$isClient() || !player.hm$isLocalPlayer() || !(entity instanceof BaseVehicleEntity)) {
            return;
        }
        for (IPhysicsModule<?> module : ((BaseVehicleEntity<?>) entity).getModules()) {
            IVehicleController c = module.createNewController();
            if (c != null) {
                controllers.add(c);
            }
        }
    }

    @Override
    public void onPlayerStopControlling(HmPlayerEntity player, boolean removeControllers) {
        if (entity.physicsHandler != null) {
            entity.physicsHandler.setForceActivation(false);
        }
        setSimulationHolder(getDefaultSimulationHolder(), null);
        if (player.hm$getWorld().hm$isClient() && player.hm$isLocalPlayer()) {
            controllers.clear();
        }
    }

    @Override
    public void onPrePhysicsTick(Profiler profiler) {
        if (entity.getWorld().hm$isClient() && entity.initialized == PhysicsEntity.EnumEntityInitState.ALL &&
                entity.getControllingPassenger() instanceof HmPlayerEntity && ((HmPlayerEntity) entity.getControllingPassenger()).hm$isLocalPlayer()) {
            controllers.forEach(IVehicleController::update);
        }
        PhysicsEntity<?> other = getOtherSideEntity();
        if (other != null) {
            getReceivedVariables().forEach((key, value) -> ((SynchronizedEntityVariableSnapshot<Object>) value).updateVariable(tryGetVariable(key)));
        }
        entity.prePhysicsUpdateWrapper(profiler, entity.usesPhysicsWorld());
    }

    @Override
    public void onPostPhysicsTick(Profiler profiler) {
        entity.postUpdatePhysicsWrapper(profiler, entity.usesPhysicsWorld());
        PhysicsEntity<?> other = getOtherSideEntity();
        if (other != null && other.initialized == PhysicsEntity.EnumEntityInitState.ALL) {
            if (isClient) {
                sendMyVars((SPPhysicsEntitySynchronizer<T>) ((T) other).getSynchronizer(), SyncTarget.SERVER);
            } else {
                profiler.start(Profiler.Profiles.PKTSEND2);
                sendMyVars((SPPhysicsEntitySynchronizer<T>) ((T) other).getSynchronizer(), SyncTarget.SPECTATORS);
                profiler.end(Profiler.Profiles.PKTSEND2);
            }
        }
    }

    @Override
    public boolean doesOtherSideUsesPhysics() {
        return !entity.hm$getWorld().hm$isClient();
    }

    @Override
    public SimulationHolder getDefaultSimulationHolder() {
        return SimulationHolder.SERVER_SP;
    }

    @Override
    public List<IVehicleController> getControllers() {
        return controllers;
    }
}
