package fr.dynamx.common.network.sync;

import fr.dynamx.api.entities.IModuleContainer;
import fr.dynamx.api.entities.modules.IPhysicsModule;
import fr.dynamx.api.entities.modules.IVehicleController;
import fr.dynamx.api.network.sync.ClientEntityNetHandler;
import fr.dynamx.api.network.sync.SimulationHolder;
import fr.dynamx.api.network.sync.SyncTarget;
import fr.dynamx.api.network.sync.EntityVariable;
import fr.dynamx.common.network.sync.variables.SynchronizedEntityVariableSnapshot;
import fr.dynamx.common.DynamXMain;
import fr.dynamx.common.entities.BaseVehicleEntity;
import fr.dynamx.common.entities.PhysicsEntity;
import fr.dynamx.utils.debug.Profiler;
import fr.dynamx.utils.optimization.PooledHashMap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.*;

/**
 * Simplified networks handler (there are no packets sent) for singleplayer games
 */
public class SPPhysicsEntitySynchronizer<T extends PhysicsEntity<?>> extends PhysicsEntitySynchronizer<T> implements ClientEntityNetHandler {
    private final Side mySide;
    private final List<IVehicleController> controllers = new ArrayList<>();

    public SPPhysicsEntitySynchronizer(T entityIn, Side side) {
        super(entityIn);
        this.mySide = side;
    }

    public Entity getOtherSideEntity() {
        if (mySide.isServer()) {
            return Minecraft.getMinecraft().player == null ? null : DynamXMain.proxy.getClientWorld().getEntityByID(entity.getEntityId());
        }
        return DynamXMain.proxy.getServerWorld().getEntityByID(entity.getEntityId());
    }

    private <A> void sendMyVars(SPPhysicsEntitySynchronizer<T> other, SyncTarget to) {
        PooledHashMap<Integer, EntityVariable<?>> varsToSync = getVarsToSync(mySide, to);
        ByteBuf buf = Unpooled.buffer();
        for (Map.Entry<Integer, EntityVariable<?>> entry : varsToSync.entrySet()) {
            Integer varId = entry.getKey();
            EntityVariable<A> sourceVar = (EntityVariable<A>) entry.getValue();
            sourceVar.writeValue(buf, false);
            sourceVar.setChanged(false);
            SynchronizedEntityVariableSnapshot<A> targetVar = (SynchronizedEntityVariableSnapshot<A>) other.getReceivedVariables().get(varId);
            if(targetVar != null) {
                targetVar.read(buf);
            } else if(other.getEntity().ticksExisted > 20) {
                DynamXMain.log.error("Var not found " + varId + " " + sourceVar + " " + other.getReceivedVariables() + " on " + entity);
            }
            buf.clear();
        }
        varsToSync.release();
    }

    @Override
    public void onPlayerStartControlling(EntityPlayer player, boolean addControllers) {
        if (entity.physicsHandler != null) {
            entity.physicsHandler.setForceActivation(true);
        }
        setSimulationHolder(SimulationHolder.DRIVER_SP, player);
        if (player.world.isRemote && player.isUser()) {
            if (entity instanceof BaseVehicleEntity) {
                for (Object module : ((BaseVehicleEntity) entity).getModules()) {
                    IVehicleController c = ((IPhysicsModule) module).createNewController();
                    if (c != null)
                        controllers.add(c);
                }
            }
        }
    }

    @Override
    public void onPlayerStopControlling(EntityPlayer player, boolean removeControllers) {
        if (entity.physicsHandler != null) {
            entity.physicsHandler.setForceActivation(false);
        }
        setSimulationHolder(getDefaultSimulationHolder(), null);
        if (player.world.isRemote && player.isUser()) {
            controllers.clear();
        }
    }

    @Override
    public void onPrePhysicsTick(Profiler profiler) {
        if (entity.world.isRemote && entity.initialized == 2 && isLocalPlayerDriving()) {
            controllers.forEach(IVehicleController::update);
        }
        Entity other = getOtherSideEntity();
        if (other instanceof PhysicsEntity) {
            getReceivedVariables().forEach((key, value) -> ((SynchronizedEntityVariableSnapshot<Object>) value).updateVariable((EntityVariable<Object>) getSynchronizedVariables().get(key)));
        }
        entity.prePhysicsUpdateWrapper(profiler, entity.usesPhysicsWorld());
    }

    @Override
    public void onPostPhysicsTick(Profiler profiler) {
        entity.postUpdatePhysicsWrapper(profiler, entity.usesPhysicsWorld());

        Entity other = getOtherSideEntity();
        if (other instanceof PhysicsEntity && ((PhysicsEntity<?>) other).initialized != 0) {
            if (!mySide.isServer()) {
                sendMyVars((SPPhysicsEntitySynchronizer<T>) ((T) other).getSynchronizer(), SyncTarget.SERVER);
            } else {
                profiler.start(Profiler.Profiles.PKTSEND2);
                sendMyVars((SPPhysicsEntitySynchronizer<T>) ((T) other).getSynchronizer(), SyncTarget.SPECTATORS);
                profiler.end(Profiler.Profiles.PKTSEND2);
            }
        }
    }

    /**
     * @return True if the driving player is Minecraft.getMinecraft().player
     */
    @SideOnly(Side.CLIENT)
    public boolean isLocalPlayerDriving() {
        return entity.getControllingPassenger() == Minecraft.getMinecraft().player;
    }

    @Override
    public boolean doesOtherSideUsesPhysics() {
        return !entity.world.isRemote;
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
