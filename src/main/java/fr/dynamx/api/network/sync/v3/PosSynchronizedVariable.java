package fr.dynamx.api.network.sync.v3;

import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import fr.dynamx.api.network.EnumPacketTarget;
import fr.dynamx.client.network.ClientPhysicsEntitySynchronizer;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.DynamXMain;
import fr.dynamx.common.entities.PhysicsEntity;
import fr.dynamx.common.network.packets.MessageForcePlayerPos;
import fr.dynamx.common.network.sync.v3.DynamXSynchronizedVariables;
import fr.dynamx.common.physics.entities.AbstractEntityPhysicsHandler;
import fr.dynamx.server.network.ServerPhysicsSyncManager;
import fr.dynamx.utils.DynamXUtils;
import fr.dynamx.utils.debug.SyncTracker;
import fr.dynamx.utils.optimization.Vector3fPool;
import io.netty.buffer.ByteBuf;
import lombok.Getter;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.text.TextComponentString;

import java.util.concurrent.Callable;

import static fr.dynamx.common.network.sync.vars.PosSynchronizedVariable.*;

public class PosSynchronizedVariable extends ListeningSynchronizedEntityVariable<PosSynchronizedVariable.EntityPositionData>
{
    public PosSynchronizedVariable(PhysicsEntity<?> entity) {
        super(((entityPositionDataSynchronizedEntityVariable, entityPositionData) -> {
//TODO INTPERPOLATION ETC :c
            if(entity.getSynchronizer().getSimulationHolder().isSinglePlayer()) {
                if (!entity.world.isRemote) //Solo mode
                {
                    entity.motionX = entityPositionData.position.x - entity.physicsPosition.x;
                    entity.motionY = entityPositionData.position.y - entity.physicsPosition.y;
                    entity.motionZ = entityPositionData.position.z - entity.physicsPosition.z;
                    double x = entity.physicsPosition.x + entity.motionX;
                    double y = entity.physicsPosition.y + entity.motionY;
                    double z = entity.physicsPosition.z + entity.motionZ;
                    entity.physicsPosition.set((float) x, (float) y, (float) z);
                    entity.physicsRotation.set(entityPositionData.rotation);
                } else {
                    DynamXMain.log.error("Incorrect simulation holder in client set pos value : " + entity.getSynchronizer().getSimulationHolder());
                }
            } else {
                //System.out.println("s pos");
                /*int ignoreFor = 0;
                if (ignoreFor <= 0) {
                    Vector3f pos = entityPositionData.position;
                    float delta = entity.physicsPosition.subtract(pos).length();
                    if (delta > CRITIC1) {
                        if (delta > CRITIC1warn)
                            DynamXMain.log.warn("Physics entity " + entity + " is moving too quickly (driven by " + entity.getControllingPassenger() + ") !");
                        if (delta > CRITIC2 && entity.getControllingPassenger() instanceof EntityPlayerMP) {
                            ((EntityPlayerMP) entity.getControllingPassenger()).connection.disconnect(new TextComponentString("Invalid physics entity move packet"));
                        } else if (entity.getControllingPassenger() instanceof EntityPlayerMP) {
                            if (delta > CRITIC3) {
                                //Resync
                                DynamXMain.log.error(entity + " doing resync !!!");
                                ignoreFor = 20;
                                DynamXContext.getNetwork().sendToClient(new MessageForcePlayerPos(entity, entity.physicsPosition, entity.physicsRotation, entity.physicsHandler.getLinearVelocity(), entity.physicsHandler.getAngularVelocity()), EnumPacketTarget.PLAYER, (EntityPlayerMP) entity.getControllingPassenger());
                            } else
                                entity.physicsHandler.updatePhysicsState(pos, entityPositionData.rotation, entityPositionData.linearVel, entityPositionData.rotationalVel);
                        } else
                            DynamXMain.log.error(entity + " lost his player for sync. ");//Packet sent from "+(msg != null ? msg.getSender() : "solo"));
                    } else if(entityPositionData.isBodyActive()){
                        //Update entity pos
                        entity.physicsHandler.updatePhysicsStateFromNet(pos, entityPositionData.rotation, entityPositionData.linearVel, entityPositionData.rotationalVel);
                    }
                } else {
                    ignoreFor--;
                }*/
                ((ClientPhysicsEntitySynchronizer)entity.getSynchronizer()).setServerPos(entityPositionData.position);
                ((ClientPhysicsEntitySynchronizer)entity.getSynchronizer()).setServerRotation(entityPositionData.rotation);
            }
        }), SynchronizationRules.PHYSICS_TO_SPECTATORS, DynamXSynchronizedVariables.posSerializer, new Callable<fr.dynamx.api.network.sync.v3.PosSynchronizedVariable.EntityPositionData>() {
            private fr.dynamx.api.network.sync.v3.PosSynchronizedVariable.EntityPositionData positionData;

            @Override
            public fr.dynamx.api.network.sync.v3.PosSynchronizedVariable.EntityPositionData call() {
                AbstractEntityPhysicsHandler<?, ?> physicsHandler = entity.physicsHandler;
                boolean changed = entity.ticksExisted % (physicsHandler.isBodyActive() ? 13 : 20) == 0; //Keep low-rate sync while not moving
                //Detect changes
                Vector3f pos = entity.physicsPosition;
                //TODO CLEAN
                if (positionData == null || positionData.bodyActive != physicsHandler.isBodyActive()) {
                    changed = true;
                } else if (SyncTracker.different(pos.x, positionData.position.x) || SyncTracker.different(pos.y, positionData.position.y) || SyncTracker.different(pos.z, positionData.position.z)) {
                    changed = true;
                } else if (SyncTracker.different(entity.physicsRotation.getX(), positionData.rotation.getX()) || SyncTracker.different(entity.physicsRotation.getY(), positionData.rotation.getY()) ||
                        SyncTracker.different(entity.physicsRotation.getZ(), positionData.rotation.getZ()) || SyncTracker.different(entity.physicsRotation.getW(), positionData.rotation.getW())) {
                    changed = true;
                }
                //TODO PAS COOL NEW
                if (changed) {
                    positionData = new fr.dynamx.api.network.sync.v3.PosSynchronizedVariable.EntityPositionData(physicsHandler);
                    entity.synchronizedPosition.setChanged(true);
                }
                return positionData;
            }
        }, "pos");
    }

    public static class EntityPositionData {
        @Getter
        private final boolean bodyActive;
        @Getter
        private final Vector3f position = new Vector3f();
        @Getter
        private final Quaternion rotation = new Quaternion();
        @Getter
        private final Vector3f linearVel = new Vector3f();
        @Getter
        private final Vector3f rotationalVel = new Vector3f();

        private EntityPositionData(AbstractEntityPhysicsHandler<?, ?> physicsHandler) {
            bodyActive = physicsHandler.isBodyActive();
            position.set(physicsHandler.getHandledEntity().physicsPosition);
            rotation.set(physicsHandler.getHandledEntity().physicsRotation);
            linearVel.set(physicsHandler.getLinearVelocity());
            rotationalVel.set(physicsHandler.getAngularVelocity());
        }

        public EntityPositionData(boolean bodyActive, Vector3f position, Quaternion rotation) {
            this.bodyActive = bodyActive;
            this.position.set(position);
            this.rotation.set(rotation);
        }
    }

    //TODO USE
    public void onTeleported(PhysicsEntity<?> entity, Vector3f newPos) {
        //TODO DOIT IGNORER LES PROCHAINES UPDATES VENANT DU CLIENT ignoreFor = 22;
        DynamXContext.getNetwork().sendToClient(new MessageForcePlayerPos(entity, newPos, entity.physicsRotation, entity.physicsHandler.getLinearVelocity(), entity.physicsHandler.getAngularVelocity()), EnumPacketTarget.PLAYER, (EntityPlayerMP) entity.getControllingPassenger());
    }
}
