package fr.dynamx.client.network;

import com.google.common.collect.Queues;
import com.jme3.math.Vector3f;
import fr.dynamx.api.entities.IModuleContainer;
import fr.dynamx.api.entities.modules.IPhysicsModule;
import fr.dynamx.api.entities.modules.IVehicleController;
import fr.dynamx.api.network.sync.*;
import fr.dynamx.client.handlers.ClientDebugSystem;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.DynamXMain;
import fr.dynamx.common.entities.BaseVehicleEntity;
import fr.dynamx.common.entities.PhysicsEntity;
import fr.dynamx.common.network.packets.MessageForcePlayerPos;
import fr.dynamx.common.network.packets.MessageJoints;
import fr.dynamx.common.network.packets.MessageWalkingPlayer;
import fr.dynamx.common.network.packets.PhysicsEntityMessage;
import fr.dynamx.common.network.sync.MessagePhysicsEntitySync;
import fr.dynamx.common.network.sync.MessageSeatsSync;
import fr.dynamx.common.network.sync.vars.EntityPhysicsState;
import fr.dynamx.common.physics.joints.EntityJoint;
import fr.dynamx.common.physics.joints.EntityJointsHandler;
import fr.dynamx.common.physics.player.WalkingOnPlayerController;
import fr.dynamx.utils.debug.Profiler;
import fr.dynamx.utils.optimization.HashMapPool;
import fr.dynamx.utils.optimization.PooledHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.fml.relauncher.Side;

import java.util.*;

import static fr.dynamx.api.network.sync.SynchronizedVariablesRegistry.retainSyncVars;
import static fr.dynamx.common.DynamXMain.log;

public class UdpClientPhysicsEntityNetHandler<T extends PhysicsEntity<?>> extends PhysicsEntityNetHandler<T> implements ClientEntityNetHandler {
    /**
     * Ordered sync waiting packet queue, we need this because when network lags, we receive all packets at the same tick
     */
    private final Queue<MessagePhysicsEntitySync<T>> queuedPackets = Queues.newArrayDeque();
    private int ticksBeforeNextSync, skippedPacketsCount;

    private final Map<Integer, EntityPhysicsState> states = new HashMap<>();

    private final List<IVehicleController> controllers = new ArrayList<>();

    private boolean usePhysicsThisTick;

    public UdpClientPhysicsEntityNetHandler(T entity) {
        super(entity);
    }

    @Override
    public void onPrePhysicsTick(Profiler profiler) {
        controllers.forEach(IVehicleController::update);
        if (getSimulationHolder().ownsPhysics(Side.CLIENT)) {
            if (!queuedPackets.isEmpty()) {
                getInputSyncVars().clear(); //temporary thinking
                while (queuedPackets.size() > 2)
                    replaceInputSyncVars(queuedPackets.remove().varsToSync);
                MessagePhysicsEntitySync<T> pck = queuedPackets.remove();
                replaceInputSyncVars(pck.varsToSync);
                getInputSyncVars().forEach((i, v) -> v.interpolate(entity, this, profiler, pck, 1));
                //readPacket();
                //Map<Integer, CleanSynchronizedVariable<T>> map = queuedPackets.remove().varsToSync;
                //POS = (PosSynchronizedVariable) map.get(SynchronizedVariablesRegistry.POS);
                //TO FIX POS.isDesynchronized(entity);
                //syncVars.forEach((i,v) -> v.interpolate(entity, 1));

                //Here for debug only
            }
            usePhysicsThisTick = true;

            //entity.updatePos();
        } else if (!getSimulationHolder().ownsPhysics(Side.CLIENT)) {
            ticksBeforeNextSync--; //Interpolation var
            if (skippedPacketsCount > 0)
                skippedPacketsCount--;
            if (ticksBeforeNextSync <= 0) //We finished the interpolation so take a new packet !
            {
                if (!queuedPackets.isEmpty()) {
                    getInputSyncVars().clear(); //temporary thinking
                    while (queuedPackets.size() > 2)
                        replaceInputSyncVars(queuedPackets.remove().varsToSync);
                    MessagePhysicsEntitySync<T> pck = queuedPackets.remove();
                    replaceInputSyncVars(pck.varsToSync);
                    ticksBeforeNextSync = entity.getSyncTickRate();
                    getInputSyncVars().forEach((i, v) -> v.interpolate(entity, this, profiler, pck, ticksBeforeNextSync));

                    usePhysicsThisTick = DynamXMain.proxy.ownsSimulation(entity);
                } else //We have no packets so use prediction
                {
                    usePhysicsThisTick = true;
                    //entity.updatePos();
                }
            } else if (ticksBeforeNextSync > 0) {//We have a new pos, so interpolate
                getInputSyncVars().forEach((i, v) -> v.interpolate(entity, this, profiler, null, ticksBeforeNextSync));
                usePhysicsThisTick = DynamXMain.proxy.ownsSimulation(entity);
            }
        }
        entity.prePhysicsUpdateWrapper(profiler, usePhysicsThisTick);

        //TODO THIS IS BAD
        //if (entity instanceof IModuleContainer.IEngineContainer && entity instanceof IModuleContainer.ISeatsContainer) {
        if (getSimulationHolder().ownsPhysics(Side.CLIENT)) {
            PooledHashMap<Integer, ? extends SynchronizedVariable<? extends PhysicsEntity<?>>> syncData = retainSyncVars(entity.getNetwork().getOutputSyncVars(), entity.getNetwork().getDirtyVars(HashMapPool.get(), Side.SERVER, ClientPhysicsSyncManager.simulationTime), SyncTarget.SERVER);
            //System.out.println("Send sync "+syncData+" "+ClientPhysicsSyncManager.simulationTime);
            if (!syncData.isEmpty()) {
                DynamXContext.getNetwork().sendToServer(new MessagePhysicsEntitySync(entity, ClientPhysicsSyncManager.simulationTime, syncData, MessagePhysicsEntitySync.SyncType.UDP_SYNC));
            } else {
                syncData.release();
            }
        }
        if (/*entity.getControllingPassenger() instanceof EntityPlayer &&*/ Minecraft.getMinecraft().player.getRidingEntity() instanceof BaseVehicleEntity && ClientDebugSystem.MOVE_DEBUG > 0) {
            if (Math.abs(entity.motionX) > 0.05f || Math.abs(entity.motionY) > 0.05f || Math.abs(entity.motionZ) > 0.05f) {
                log.warn("Entity " + entity.getEntityId() + " is moving motion " + entity.motionX + " " + entity.motionY + " " + entity.motionZ + " cli time " + ClientPhysicsSyncManager.simulationTime + " ticks exist " + entity.ticksExisted);
            }
        }
        // }
        if (DynamXMain.proxy.ownsSimulation(entity)) {
            states.put(ClientPhysicsSyncManager.simulationTime, entity.createStateSnapshot());
        }
    }

    @Override
    public void onPostPhysicsTick(Profiler profiler) {
        entity.postUpdatePhysicsWrapper(profiler, usePhysicsThisTick);
    }

    @Override
    public void onPlayerStartControlling(EntityPlayer player, boolean addControllers) {
        if (entity.physicsHandler != null) {
            entity.physicsHandler.setForceActivation(true);
        }
        if (player.isUser()) {
            if (addControllers && entity instanceof BaseVehicleEntity) {
                for (Object module : ((BaseVehicleEntity) entity).getModules()) {
                    IVehicleController c = ((IPhysicsModule) module).createNewController();
                    if (c != null)
                        controllers.add(c);
                }
            }
            setSimulationHolder(SimulationHolder.DRIVER);
            ClientPhysicsSyncManager.simulationTime = 0;
            states.clear();
        } else {
            setSimulationHolder(SimulationHolder.OTHER_CLIENT);
        }
    }

    @Override
    public void onPlayerStopControlling(EntityPlayer player, boolean removeControllers) {
        if (entity.physicsHandler != null) {
            entity.physicsHandler.setForceActivation(false);
        }
        setSimulationHolder(getDefaultSimulationHolder());
        if (removeControllers && player.isUser()) {
            controllers.clear();
        }
        states.clear();
    }

    @Override
    public void onWalkingPlayerChange(int playerId, Vector3f offset, byte face) {
        super.onWalkingPlayerChange(playerId, offset, face);
        DynamXContext.getNetwork().sendToServer(new MessageWalkingPlayer(entity, playerId, offset, face));
    }

    @Override
    public void processPacket(PhysicsEntityMessage<?> message) {
        if (message.getMessageId() == 1) //PosSync
            receivePosSyncPacket((MessagePhysicsEntitySync) message);
        else if (message.getMessageId() == 3) //Seats
        {
            if (entity instanceof IModuleContainer.ISeatsContainer)
                ((IModuleContainer.ISeatsContainer) entity).getSeats().updateSeats((MessageSeatsSync) message, this);
            else
                log.fatal("Received seats packet for an entity that have no seats !");
        } else if (message.getMessageId() == 4) //Walking player
        {
            MessageWalkingPlayer p = (MessageWalkingPlayer) message;
            Entity e = entity.world.getEntityByID(p.playerId);
            if (e instanceof EntityPlayer && e != Minecraft.getMinecraft().player) {
                if (p.face == -1) {
                    entity.walkingOnPlayers.remove(e);
                    DynamXContext.getWalkingPlayers().remove(e);
                } else {
                    entity.walkingOnPlayers.put((EntityPlayer) e, new WalkingOnPlayerController((EntityPlayer) e, entity, EnumFacing.byIndex(p.face), p.offset));
                    DynamXContext.getWalkingPlayers().put((EntityPlayer) e, entity);
                }
            }
        } else if (message.getMessageId() == 5) //RESYNC
        {
            MessageForcePlayerPos p = (MessageForcePlayerPos) message;
            if (entity.physicsHandler != null) {
                entity.physicsPosition.set(p.rightPos);
                //entity.physicEntity.getPosition().set(p.rightPos);
                entity.physicsRotation.set(p.rotation);
                //entity.physicEntity.getRotation().set(p.rotation);
                entity.physicsHandler.updatePhysicsState(p.rightPos, p.rotation, p.linearVel, p.rotationalVel);
                log.info("Entity " + entity + " has been resynced");

                Minecraft.getMinecraft().ingameGUI.setOverlayMessage("Resynchronisation...", true);
            } else
                log.fatal("Cannot resync entity " + entity + " : not physics found !");
        } else if (message.getMessageId() == 6) //Joints
        {
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
        }
    }

    private void receivePosSyncPacket(MessagePhysicsEntitySync msg) {
        //while (queuedPackets.size() > 2)
        //    queuedPackets.poll();
        queuedPackets.add(msg);
    }

    @Override
    public Map<Integer, EntityPhysicsState> getOldStates() {
        return states;
    }

    @Override
    public List<IVehicleController> getControllers() {
        return controllers;
    }
}
