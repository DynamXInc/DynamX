package fr.dynamx.server.network;

import com.google.common.collect.Queues;
import fr.dynamx.api.network.EnumPacketTarget;
import fr.dynamx.api.network.sync.PhysicsEntityNetHandler;
import fr.dynamx.api.network.sync.SimulationHolder;
import fr.dynamx.api.network.sync.SyncTarget;
import fr.dynamx.api.network.sync.SynchronizedVariable;
import fr.dynamx.api.network.sync.v3.PhysicsEntitySynchronizer;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.DynamXMain;
import fr.dynamx.common.entities.PhysicsEntity;
import fr.dynamx.common.network.packets.MessageWalkingPlayer;
import fr.dynamx.common.network.packets.PhysicsEntityMessage;
import fr.dynamx.common.network.sync.MessagePhysicsEntitySync;
import fr.dynamx.common.physics.player.WalkingOnPlayerController;
import fr.dynamx.server.command.CmdNetworkConfig;
import fr.dynamx.utils.debug.Profiler;
import fr.dynamx.utils.optimization.PooledHashMap;
import fr.dynamx.utils.optimization.Vector3fPool;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import static fr.dynamx.api.network.sync.SynchronizedVariablesRegistry.retainSyncVars;

@SideOnly(Side.SERVER)
public class ServerPhysicsEntitySynchronizer<T extends PhysicsEntity<?>> extends PhysicsEntitySynchronizer<T> {
    /**
     * Ordered sync waiting packet queue, we need this because when network lags, we receive all packets at the same tick
     */
    private final Queue<MessagePhysicsEntitySync<T>> queuedPackets = Queues.newArrayDeque();

    private final Map<Integer, SyncTarget> varsToSync = new HashMap<>();
    private int updateCount = 0;

    public ServerPhysicsEntitySynchronizer(T entityIn) {
        super(entityIn);
    }

    @Override
    public void onPlayerStartControlling(EntityPlayer player, boolean addControllers) {
        if (entity.physicsHandler != null)
            entity.physicsHandler.setForceActivation(true);
        ServerPhysicsSyncManager.putTime(player, 0);
        setSimulationHolder(SimulationHolder.DRIVER);
    }

    @Override
    public void onPlayerStopControlling(EntityPlayer player, boolean removeControllers) {
        if (entity.physicsHandler != null)
            entity.physicsHandler.setForceActivation(false);
        setSimulationHolder(getDefaultSimulationHolder());
    }

    @Override
    public void onPrePhysicsTick(Profiler profiler) {
        if (!queuedPackets.isEmpty()) {



            //HERE WILL BE THE TRICKY CODE TO FIND DIFFS
            getInputSyncVars().clear(); //temporary thinking
            while (queuedPackets.size() > 2)
                replaceInputSyncVars(queuedPackets.remove().varsToSync);
            MessagePhysicsEntitySync pck = queuedPackets.remove();
            replaceInputSyncVars(pck.varsToSync);
            getInputSyncVars().forEach((i, v) -> v.setValueTo(entity, this, pck, Side.SERVER));
        }
        profiler.start(Profiler.Profiles.PHY1);
        Vector3fPool.openPool();
        entity.prePhysicsUpdateWrapper(profiler, true);
        Vector3fPool.closePool();
        profiler.end(Profiler.Profiles.PHY1);
        //entity.updatePos();

        if (entity.ticksExisted % entity.getSyncTickRate() == 0) //Don't send a packet each tick
        {
            varsToSync.clear();
            profiler.start(Profiler.Profiles.PKTSEND1);
            getDirtyVars(varsToSync, Side.CLIENT, updateCount);
            varsToSync.forEach((i, t) -> getOutputSyncVars().get(i).validate(entity, 1));
            profiler.end(Profiler.Profiles.PKTSEND1);

            profiler.start(Profiler.Profiles.PKTSEND2);
            Set<? extends EntityPlayer> l = ((WorldServer) entity.world).getEntityTracker().getTrackingPlayers(entity);
            l.forEach(p -> sendSyncTo(p, retainSyncVars(getOutputSyncVars(), varsToSync, p == entity.getControllingPassenger() ? SyncTarget.DRIVER : SyncTarget.SPECTATORS)));
            profiler.end(Profiler.Profiles.PKTSEND2);
            updateCount++;
        }

        //if(entity.getControllingPassenger() instanceof EntityPlayer)// && DynamXCommands.SERVER_NET_DEBUG)
        {
            if ((Math.abs(entity.motionX) > 0.05f || Math.abs(entity.motionY) > 0.05f || Math.abs(entity.motionZ) > 0.05f) && CmdNetworkConfig.SERVER_NET_DEBUG > 0) {
                DynamXMain.log.info("Entity " + entity.getEntityId() + " is moving motion " + entity.motionX + " " + entity.motionY + " " + entity.motionZ + " cli time " + ServerPhysicsSyncManager.toDebugString() + " ticks exist " + entity.ticksExisted);
            }
        }
    }

    @Override
    public void onPostPhysicsTick(Profiler profiler) {
        entity.postUpdatePhysicsWrapper(profiler, true);
    }

    private void sendSyncTo(EntityPlayer p, PooledHashMap<Integer, SynchronizedVariable<T>> varsToSync) {
        //if(!ServerPhysicsSyncManager.wasSent(p, ServerPhysicsSyncManager.getTime(p))) {
        if (!varsToSync.isEmpty()) {
            ServerPhysicsSyncManager.addEntitySync(p, entity, varsToSync);
        }
        //}
    }

    @Override
    public void processPacket(PhysicsEntityMessage<?> message) {
        if (message.getMessageId() == 1) //Sync
        {
            //System.out.println("Add pck with time cli "+((SynchronizedVariable.Pos)((MessageBulletEntitySync)message).varsToSync.get(SynchronizedVariablesRegistry.POS)).simulationTimeClient
            //      +" srv "+((SynchronizedVariable.Pos)((MessageBulletEntitySync)message).varsToSync.get(SynchronizedVariablesRegistry.POS)).simulationTimeServer+" here is srv "+serverSimTime);
            //System.out.println("QUEUING "+(((MessageBulletEntitySync) message).getSimulationTimeClient()));
            queuedPackets.add((MessagePhysicsEntitySync) message);
            /*if (queuedPackets.size() > 2) {
                queuedPackets.poll();
            }*/
        } else if (message.getMessageId() == 4) {
            MessageWalkingPlayer p = (MessageWalkingPlayer) message;
            Entity e = entity.world.getEntityByID(p.playerId);
            if (e instanceof EntityPlayer) {
                if (p.face == -1) {
                    entity.walkingOnPlayers.remove(e);
                    DynamXContext.getWalkingPlayers().remove(e);
                } else {
                    entity.walkingOnPlayers.put((EntityPlayer) e, new WalkingOnPlayerController((EntityPlayer) e, entity, EnumFacing.byIndex(p.face), p.offset));
                    DynamXContext.getWalkingPlayers().put((EntityPlayer) e, entity);
                }
            }
            DynamXContext.getNetwork().sendToClientFromOtherThread(new MessageWalkingPlayer(entity, p.playerId, p.offset, p.face), EnumPacketTarget.ALL_TRACKING_ENTITY, entity);
        }
    }
}