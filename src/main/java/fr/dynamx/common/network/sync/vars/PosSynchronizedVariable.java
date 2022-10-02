package fr.dynamx.common.network.sync.vars;

import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import fr.dynamx.api.network.EnumPacketTarget;
import fr.dynamx.api.network.sync.PhysicsEntityNetHandler;
import fr.dynamx.api.network.sync.SyncTarget;
import fr.dynamx.api.network.sync.SynchronizedVariable;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.DynamXMain;
import fr.dynamx.common.entities.PhysicsEntity;
import fr.dynamx.common.network.packets.MessageForcePlayerPos;
import fr.dynamx.common.network.sync.MessagePhysicsEntitySync;
import fr.dynamx.common.physics.entities.AbstractEntityPhysicsHandler;
import fr.dynamx.server.network.ServerPhysicsSyncManager;
import fr.dynamx.utils.DynamXConstants;
import fr.dynamx.utils.DynamXUtils;
import fr.dynamx.utils.debug.Profiler;
import fr.dynamx.utils.debug.SyncTracker;
import fr.dynamx.utils.maths.DynamXMath;
import fr.dynamx.utils.optimization.Vector3fPool;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.fml.relauncher.Side;

/**
 * The {@link SynchronizedVariable} responsible to sync the pos and the rotation of the entity
 */
public class PosSynchronizedVariable implements SynchronizedVariable<PhysicsEntity<?>> {
    public static final ResourceLocation NAME = new ResourceLocation(DynamXConstants.ID, "pos");

    private boolean bodyActive;
    private float posX, posY, posZ;
    private Quaternion rotation = new Quaternion();
    private final Vector3f linearVel = new Vector3f();
    private final Vector3f rotationalVel = new Vector3f();
    private final Vector3f gravity = new Vector3f();

    private EntityPhysicsState interpolatingState;

    @Override
    public SyncTarget getValueFrom(PhysicsEntity<?> entity, PhysicsEntityNetHandler<PhysicsEntity<?>> network, Side side, int syncTick) {
        boolean changed = syncTick % (bodyActive ? 13 : 20) == 0; //Keep low-rate sync while not moving
        AbstractEntityPhysicsHandler<?, ?> physicsHandler = entity.physicsHandler;
        //Detect changes
        /*PhysicsCollisionObject rigidBody = physicsHandler.getRigidBody();
        if (rigidBody instanceof PhysicsRigidBody) {
            Vector3f gravity1 = ((PhysicsRigidBody) rigidBody).getGravity(null);
            if (gravity.equals(gravity1)) {
                gravity = gravity1;
                changed = true;
            }
        }*/
        Vector3f pos = entity.physicsPosition;
        if (bodyActive != physicsHandler.isBodyActive()) {
            bodyActive = physicsHandler.isBodyActive();
            changed = true;
        } else if (SyncTracker.different(pos.x, posX) || SyncTracker.different(pos.y, posY) || SyncTracker.different(pos.z, posZ)) {
            changed = true;
        } else if (SyncTracker.different(entity.physicsRotation.getX(), rotation.getX()) || SyncTracker.different(entity.physicsRotation.getY(), rotation.getY()) ||
                SyncTracker.different(entity.physicsRotation.getZ(), rotation.getZ()) || SyncTracker.different(entity.physicsRotation.getW(), rotation.getW())) {
            changed = true;
        }
        if (changed) {
            posX = pos.x;
            posY = pos.y;
            posZ = pos.z;

            rotation.set(entity.physicsRotation);
            linearVel.set(physicsHandler.getLinearVelocity());
            rotationalVel.set(physicsHandler.getAngularVelocity());
        }
        return changed ? (side.isClient() ? SyncTarget.ALL_CLIENTS : SyncTarget.SERVER) : SyncTarget.NONE;
    }

    public int ignoreFor;

    public void onTeleported(PhysicsEntity<?> entity, Vector3f newPos) {
        ignoreFor = 22;
        DynamXContext.getNetwork().sendToClient(new MessageForcePlayerPos(entity, newPos, entity.physicsRotation, entity.physicsHandler.getLinearVelocity(), entity.physicsHandler.getAngularVelocity()), EnumPacketTarget.PLAYER, (EntityPlayerMP) entity.getControllingPassenger());
    }

    public static int CRITIC1 = 30, CRITIC1warn = 100, CRITIC2 = 400, CRITIC3 = 50;

    @Override
    public void setValueTo(PhysicsEntity<?> entity, PhysicsEntityNetHandler<PhysicsEntity<?>> network, MessagePhysicsEntitySync msg, Side side) {
        if (network.getSimulationHolder().isSinglePlayer()) {
            if (side.isServer()) //Solo mode
            {
                entity.motionX = posX - entity.physicsPosition.x;
                entity.motionY = posY - entity.physicsPosition.y;
                entity.motionZ = posZ - entity.physicsPosition.z;
                double x = entity.physicsPosition.x + entity.motionX;
                double y = entity.physicsPosition.y + entity.motionY;
                double z = entity.physicsPosition.z + entity.motionZ;
                entity.physicsPosition.set((float) x, (float) y, (float) z);

                entity.physicsRotation.set(rotation);

            } else {
                DynamXMain.log.error("Incorrect simulation holder in client set pos value : " + network.getSimulationHolder());
            }
        } else //Server side
        {
            if (ignoreFor <= 0) {
                Vector3f pos = Vector3fPool.get(posX, posY, posZ);

                float delta = entity.physicsPosition.subtract(pos).length();
                if (delta > CRITIC1) {
                    if (delta > CRITIC1warn)
                        DynamXMain.log.warn("Physics entity " + entity + " is moving too quickly (driven by " + entity.getControllingPassenger() + ") !");
                    if (delta > CRITIC2 && entity.getControllingPassenger() instanceof EntityPlayerMP) {
                        ((EntityPlayerMP) entity.getControllingPassenger()).connection.disconnect(new TextComponentString("Invalid physics entity move packet"));
                    } else if (entity.getControllingPassenger() instanceof EntityPlayerMP) {
                        if (delta > CRITIC3) {
                            //Resync
                            ignoreFor = 20;
                            DynamXContext.getNetwork().sendToClient(new MessageForcePlayerPos(entity, entity.physicsPosition, entity.physicsRotation, entity.physicsHandler.getLinearVelocity(), entity.physicsHandler.getAngularVelocity()), EnumPacketTarget.PLAYER, (EntityPlayerMP) entity.getControllingPassenger());
                        } else
                            entity.physicsHandler.updatePhysicsState(pos, rotation, linearVel, rotationalVel);
                    } else
                        DynamXMain.log.error(entity + " lost his player for sync. ");//Packet sent from "+(msg != null ? msg.getSender() : "solo"));
                } else {
                    //Update entity pos
                    entity.physicsHandler.updatePhysicsStateFromNet(pos, rotation, linearVel, rotationalVel);
                }
            } else {
                ignoreFor--;
            }

            //Update stored driver's simulation time
            if (entity.getControllingPassenger() instanceof EntityPlayer) {
                ServerPhysicsSyncManager.putTime((EntityPlayer) entity.getControllingPassenger(), msg.getSimulationTimeClient() - 1);
            }
        }
    }

    @Override
    public void interpolate(PhysicsEntity<?> entity, PhysicsEntityNetHandler<PhysicsEntity<?>> network, Profiler profiler, MessagePhysicsEntitySync msg, int step) {
        if (DynamXMain.proxy.ownsSimulation(entity)) //If we are simulating this entity
        {
            if (interpolatingState == null && msg != null) //If interpolation isn't started
            {
                //System.out.println("Packet received at "+ ClientPhysicsSyncManager.simulationTime+", time is "+ msg.simulationTimeClient +" for entity "+entity.getEntityId()+" rcv at ticks existed "+entity.ticksExisted);
                EntityPhysicsState state = entity.getNetwork().getStateAndClearOlders(msg.getSimulationTimeClient()); //Get the state corresponding to the tick of the data
                interpolatingState = state; //Interpolate over it

                //Debug
                //float[] rotations = DynamXUtils.quaternionToEuler(rotation, entity.rotationYaw, entity.rotationPitch, entity.rotationRoll); //Prev or not prev ?
                if (state != null) {
                    //Smoothly correct entity pos from server's data, only if we are not driving this entity
                    if (!network.getSimulationHolder().ownsPhysics(Side.CLIENT)) {
                        interpolatingState.interpolateDeltas(Vector3fPool.get(posX, posY, posZ), rotation, bodyActive, entity.getSyncTickRate(), 0);
                    }
                }
            } else if (interpolatingState != null && !network.getSimulationHolder().ownsPhysics(Side.CLIENT)) { //Second interpolation step
                interpolatingState.interpolateDeltas(Vector3fPool.get(posX, posY, posZ), rotation, bodyActive, entity.getSyncTickRate(), 1);
            }
        } else {
            //Else we just interpolate
            //Thanks to Flan's mod and Mojang for the idea of this code

            entity.motionX = DynamXMath.interpolateDoubleDelta(posX, entity.physicsPosition.x, step);
            entity.motionY = DynamXMath.interpolateDoubleDelta(posY, entity.physicsPosition.y, step);
            entity.motionZ = DynamXMath.interpolateDoubleDelta(posZ, entity.physicsPosition.z, step);
            if (!bodyActive && Math.abs(entity.motionX) < SyncTracker.EPS && Math.abs(entity.motionY) < SyncTracker.EPS && Math.abs(entity.motionZ) < SyncTracker.EPS) {
                entity.motionX = entity.motionY = entity.motionZ = 0;
            } else {
                double x = entity.physicsPosition.x + entity.motionX;
                double y = entity.physicsPosition.y + entity.motionY;
                double z = entity.physicsPosition.z + entity.motionZ;
                entity.physicsPosition.set((float) x, (float) y, (float) z);
                DynamXMath.slerp(1f / step, entity.physicsRotation, rotation, entity.physicsRotation);

                //When the interpolation is finished, we set the new rotation into bullet because it may be used by the prediction system
                if (step == 1 && DynamXMain.proxy.shouldUseBulletSimulation(entity.world)) {
                    entity.physicsHandler.updatePhysicsState(Vector3fPool.get(posX, posY, posZ), entity.physicsRotation, linearVel, rotationalVel);
                }
            }
        }
    }

    @Override
    public void write(ByteBuf buf, boolean compress) {
        buf.writeFloat(posX);
        buf.writeFloat(posY);
        buf.writeFloat(posZ);

        DynamXUtils.writeQuaternion(buf, rotation);

        buf.writeBoolean(bodyActive);
        if (bodyActive) {
            DynamXUtils.writeVector3f(buf, linearVel);
            DynamXUtils.writeVector3f(buf, rotationalVel);
        }
    }

    @Override
    public void writeEntityValues(PhysicsEntity<?> entity, ByteBuf buf) {
        DynamXUtils.writeVector3f(buf, entity.physicsPosition);
        DynamXUtils.writeQuaternion(buf, entity.physicsRotation);

        AbstractEntityPhysicsHandler<?, ?> physicsHandler = entity.physicsHandler;
        buf.writeBoolean(physicsHandler.isBodyActive());
        if (physicsHandler.isBodyActive()) {
            DynamXUtils.writeVector3f(buf, physicsHandler.getLinearVelocity());
            DynamXUtils.writeVector3f(buf, physicsHandler.getAngularVelocity());
        }
    }

    @Override
    public void read(ByteBuf buf) {
        posX = buf.readFloat();
        posY = buf.readFloat();
        posZ = buf.readFloat();

        rotation = DynamXUtils.readQuaternion(buf);

        bodyActive = buf.readBoolean();
        if (bodyActive) {
            linearVel.set(buf.readFloat(), buf.readFloat(), buf.readFloat());
            rotationalVel.set(buf.readFloat(), buf.readFloat(), buf.readFloat());
        } else {
            linearVel.set(0, 0, 0);
            rotationalVel.set(0, 0, 0);
        }
    }
}
