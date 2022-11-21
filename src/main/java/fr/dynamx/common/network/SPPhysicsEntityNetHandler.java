package fr.dynamx.common.network;

import com.google.common.collect.Queues;
import fr.dynamx.api.entities.IModuleContainer;
import fr.dynamx.api.entities.modules.IPhysicsModule;
import fr.dynamx.api.entities.modules.IVehicleController;
import fr.dynamx.api.network.sync.*;
import fr.dynamx.common.DynamXMain;
import fr.dynamx.common.entities.BaseVehicleEntity;
import fr.dynamx.common.entities.PhysicsEntity;
import fr.dynamx.common.network.packets.MessageJoints;
import fr.dynamx.common.network.packets.PhysicsEntityMessage;
import fr.dynamx.common.network.sync.MessageSeatsSync;
import fr.dynamx.common.network.sync.vars.EntityPhysicsState;
import fr.dynamx.common.physics.joints.EntityJoint;
import fr.dynamx.common.physics.joints.EntityJointsHandler;
import fr.dynamx.utils.debug.Profiler;
import fr.dynamx.utils.optimization.HashMapPool;
import fr.dynamx.utils.optimization.PooledHashMap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.*;

import static fr.dynamx.common.DynamXMain.log;

/**
 * Simplified networks handler (there are no packets sent) for singleplayer games
 */
public class SPPhysicsEntityNetHandler<T extends PhysicsEntity<?>> extends PhysicsEntityNetHandler<T> implements ClientEntityNetHandler {
    private final Side mySide;
    private final List<IVehicleController> controllers = new ArrayList<>();
    private final Map<Integer, SyncTarget> varsToSync = new HashMap<>();
    private int updateCount = 0;

    private final Queue<PooledHashMap<Integer, SynchronizedVariable<T>>> updateQueue = Queues.newConcurrentLinkedQueue();

    public SPPhysicsEntityNetHandler(T entityIn, Side side) {
        super(entityIn);
        this.mySide = side;
    }

    public Entity getOtherSideEntity() {
        if (mySide.isServer()) {
            return Minecraft.getMinecraft().player == null ? null : DynamXMain.proxy.getClientWorld().getEntityByID(entity.getEntityId());
        }
        return DynamXMain.proxy.getServerWorld().getEntityByID(entity.getEntityId());
    }

    private void sendMyVars(SPPhysicsEntityNetHandler<T> other, SyncTarget to) {
        PooledHashMap<Integer, SynchronizedVariable<T>> varsToSync = SynchronizedVariablesRegistry.retainSyncVars(getOutputSyncVars(), this.varsToSync, to);
        PooledHashMap<Integer, SynchronizedVariable<T>> varsToRead = HashMapPool.get();
        ByteBuf buf = Unpooled.buffer();
        for (Map.Entry<Integer, SynchronizedVariable<T>> entry : varsToSync.entrySet()) {
            Integer i = entry.getKey();
            SynchronizedVariable<T> v = entry.getValue();
            v.write(buf, false);
            SynchronizedVariable<T> v2 = (SynchronizedVariable<T>) SynchronizedVariablesRegistry.instantiate(i);
            v2.read(buf);
            buf.clear();
            varsToRead.put(i, v2);
        }
        other.updateQueue.add(varsToRead);
        varsToSync.release();
    }

    @Override
    public void onPlayerStartControlling(EntityPlayer player, boolean addControllers) {
        if (entity.physicsHandler != null) {
            entity.physicsHandler.setForceActivation(true);
        }
        setSimulationHolder(SimulationHolder.DRIVER_SP);
        if (player.world.isRemote && player.isUser()) {
            if (entity instanceof BaseVehicleEntity) {
                for (Object module : ((BaseVehicleEntity) entity).getModules()) {
                    IVehicleController c = ((IPhysicsModule) module).createNewController();
                    if (c != null)
                        controllers.add(c);
                }
            }
        }

        entity.getSynchronizer().onPlayerStartControlling(player, addControllers);
    }

    @Override
    public void onPlayerStopControlling(EntityPlayer player, boolean removeControllers) {
        if (entity.physicsHandler != null) {
            entity.physicsHandler.setForceActivation(false);
        }
        setSimulationHolder(getDefaultSimulationHolder());
        if (player.world.isRemote && player.isUser()) {
            controllers.clear();
        }

        entity.getSynchronizer().onPlayerStopControlling(player, removeControllers);
    }

    @Override
    public void onPrePhysicsTick(Profiler profiler) {
        if (entity.world.isRemote && entity.initialized == 2 && isLocalPlayerDriving() && entity instanceof IModuleContainer.IEngineContainer) {
            controllers.forEach(IVehicleController::update);
        }

        Entity other = getOtherSideEntity();
        if (other instanceof PhysicsEntity) {
            getInputSyncVars().clear();
            if (!updateQueue.isEmpty()) {
                while (updateQueue.size() > 1)
                    putInputSyncVars(updateQueue.poll());
            }
            getInputSyncVars().forEach((i, v) -> v.setValueTo(entity, this, null, mySide));
        }
        //entity.prePhysicsUpdateWrapper(profiler, entity.usesPhysicsWorld());
    }

    @Override
    public void onPostPhysicsTick(Profiler profiler) {
        entity.postUpdatePhysicsWrapper(profiler, entity.usesPhysicsWorld());

        Entity other = getOtherSideEntity();
        if (other instanceof PhysicsEntity) {
            if (!mySide.isServer()) {
                varsToSync.clear();
                getDirtyVars(varsToSync, Side.SERVER, updateCount);
                getOutputSyncVars().forEach((i, s) -> s.getValueFrom(entity, this, Side.SERVER, entity.ticksExisted));
                //sendMyVars((SPPhysicsEntityNetHandler<T>) ((T) other).getNetwork(), SyncTarget.SERVER);
            } else {
                varsToSync.clear();
                //profiler.start(Profiler.Profiles.PKTSEND1);
                getDirtyVars(varsToSync, Side.CLIENT, updateCount);
                //profiler.end(Profiler.Profiles.PKTSEND1);

                profiler.start(Profiler.Profiles.PKTSEND2);
                //sendMyVars((SPPhysicsEntityNetHandler<T>) ((T) other).getNetwork(), SyncTarget.SPECTATORS);
                profiler.end(Profiler.Profiles.PKTSEND2);
            }
            updateCount++;
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
    public void processPacket(PhysicsEntityMessage<?> message) {
        /*if (message.getMessageId() == 3) {//Seats
            /*if (entity instanceof IModuleContainer.ISeatsContainer)
                ((IModuleContainer.ISeatsContainer) entity).getSeats().updateSeats((MessageSeatsSync) message, this);
            else
                log.fatal("Received seats packet for an entity that have no seats !");*//*
        } else if (message.getMessageId() == 6) {//Joints
            if (entity.getJointsHandler() != null) {
                List<EntityJoint.CachedJoint> joints = ((MessageJoints) message).getJointList();
                EntityJointsHandler handler = ((EntityJointsHandler) entity.getJointsHandler());
                Collection<EntityJoint<?>> curJoints = handler.getJoints();
                curJoints.removeIf(j -> { //done in client thread
                    EntityJoint.CachedJoint found = null;
                    for (EntityJoint.CachedJoint g : joints) {
                        if (g.getId().equals(j.getOtherEntity(entity).getPersistentID())) {
                            found = g;
                            break;
                        }
                    }
                    if (found != null) {
                        joints.remove(found); //keep it
                        return false;
                    } else {
                        handler.onRemoveJoint(j);
                        return true;
                    }
                });
                for (EntityJoint.CachedJoint g : joints) {
                    if (g.isJointOwner()) //Only allow the owner to re-create the joint on client side
                        handler.syncJoint(g);
                }
            } else
                log.error("Cannot sync joints of " + entity + " : joint handler is null !");
        } else {
            throw new UnsupportedOperationException("Packets unavailable in single player");
        }*/
    }

    @Override
    public Map<Integer, EntityPhysicsState> getOldStates() {
        throw new IllegalStateException("Solo network");
    }

    @Override
    public List<IVehicleController> getControllers() {
        return controllers;
    }
}
