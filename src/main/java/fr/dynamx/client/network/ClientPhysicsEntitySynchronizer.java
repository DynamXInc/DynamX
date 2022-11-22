package fr.dynamx.client.network;

import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import fr.dynamx.api.entities.modules.IPhysicsModule;
import fr.dynamx.api.entities.modules.IVehicleController;
import fr.dynamx.api.network.sync.*;
import fr.dynamx.api.network.sync.v3.*;
import fr.dynamx.client.handlers.ClientDebugSystem;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.DynamXMain;
import fr.dynamx.common.entities.BaseVehicleEntity;
import fr.dynamx.common.entities.PhysicsEntity;
import fr.dynamx.common.network.packets.MessageWalkingPlayer;
import fr.dynamx.common.network.sync.MessagePhysicsEntitySync;
import fr.dynamx.common.network.sync.vars.EntityPhysicsState;
import fr.dynamx.utils.debug.Profiler;
import fr.dynamx.utils.optimization.HashMapPool;
import fr.dynamx.utils.optimization.PooledHashMap;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.relauncher.Side;

import java.util.*;

import static fr.dynamx.common.DynamXMain.log;

public class ClientPhysicsEntitySynchronizer<T extends PhysicsEntity<?>> extends MultiplayerPhysicsEntitySynchronizer<T> implements ClientEntityNetHandler {
    private int ticksBeforeNextSync, skippedPacketsCount;

    private final Map<Integer, EntityPhysicsState> states = new HashMap<>();

    private final List<IVehicleController> controllers = new ArrayList<>();

    private boolean usePhysicsThisTick;

    @Getter
    @Setter
    private Vector3f serverPos;
    @Getter
    @Setter
    private Quaternion serverRotation;

    public ClientPhysicsEntitySynchronizer(T entity) {
        super(entity);
    }

    @Override
    public void setSimulationTimeClient(int simulationTimeClient) {

    }

    @Override
    public void onPrePhysicsTick(Profiler profiler) {
        controllers.forEach(IVehicleController::update);
        if (getSimulationHolder().ownsPhysics(Side.CLIENT) || true) {
            //System.out.println("Read " + entity.ticksExisted);
            readReceivedPackets();
            usePhysicsThisTick = true;
        } else if (!getSimulationHolder().ownsPhysics(Side.CLIENT)) {
            ticksBeforeNextSync--; //Interpolation var
            if (skippedPacketsCount > 0)
                skippedPacketsCount--;
            //System.out.println("Read " + entity.ticksExisted);
            if (ticksBeforeNextSync <= 0) //We finished the interpolation so take a new packet !
            {
                if (!getReceivedPackets().isEmpty()) {
                    readReceivedPackets();
                    ticksBeforeNextSync = entity.getSyncTickRate();
                    usePhysicsThisTick = DynamXMain.proxy.ownsSimulation(entity);
                } else //We have no packets so use prediction
                {
                    usePhysicsThisTick = true;
                    //entity.updatePos();
                }
            } else if (ticksBeforeNextSync > 0) {//We have a new pos, so interpolate
                //todo getInputSyncVars().forEach((i, v) -> v.interpolate(entity, this, profiler, null, ticksBeforeNextSync));
                usePhysicsThisTick = DynamXMain.proxy.ownsSimulation(entity);
            }
        }
        entity.prePhysicsUpdateWrapper(profiler, usePhysicsThisTick);

        if (getSimulationHolder().ownsPhysics(Side.CLIENT))
            sendVariables();
        if (DynamXMain.proxy.ownsSimulation(entity))
            states.put(ClientPhysicsSyncManager.simulationTime, entity.createStateSnapshot());
    }

    protected void sendVariables() {
        PooledHashMap<Integer, SynchronizedEntityVariable<?>> syncData = getVarsToSync(Side.CLIENT, SyncTarget.SERVER);
        //System.out.println("Send sync "+syncData+" "+ClientPhysicsSyncManager.simulationTime);
        NetworkActivityTracker.addSentVars(entity, syncData.values());
        if (!syncData.isEmpty()) {
            DynamXContext.getNetwork().sendToServer(new MessagePhysicsEntitySync(entity, ClientPhysicsSyncManager.simulationTime, syncData, false));
        } else {
            syncData.release();
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
    public List<IVehicleController> getControllers() {
        return controllers;
    }
}
