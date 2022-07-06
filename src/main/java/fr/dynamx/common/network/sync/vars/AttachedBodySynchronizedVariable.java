package fr.dynamx.common.network.sync.vars;

import fr.dynamx.api.network.sync.PhysicsEntityNetHandler;
import fr.dynamx.api.network.sync.SimulationHolder;
import fr.dynamx.api.network.sync.SyncTarget;
import fr.dynamx.api.network.sync.SynchronizedVariable;
import fr.dynamx.common.DynamXMain;
import fr.dynamx.common.entities.PhysicsEntity;
import fr.dynamx.common.network.sync.MessagePhysicsEntitySync;
import fr.dynamx.common.physics.entities.EnumRagdollBodyPart;
import fr.dynamx.common.physics.utils.RigidBodyTransform;
import fr.dynamx.common.physics.utils.SynchronizedRigidBodyTransform;
import fr.dynamx.utils.debug.Profiler;
import fr.dynamx.utils.debug.SyncTracker;
import fr.dynamx.utils.maths.DynamXGeometry;
import fr.dynamx.utils.maths.DynamXMath;
import fr.dynamx.utils.optimization.Vector3fPool;
import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.relauncher.Side;

import java.util.HashMap;
import java.util.Map;

/**
 * The {@link SynchronizedVariable} responsible to sync the pos and the rotation of rigid bodies attached to the entity
 */
public abstract class AttachedBodySynchronizedVariable<T extends PhysicsEntity<?>> implements SynchronizedVariable<T>
{
    private final Map<Byte, RigidBodyTransform> transforms = new HashMap<>();
    
    public AttachedBodySynchronizedVariable() {}

    /**
     * @return The {@link AttachedBodySynchronizer} of the given entity
     */
    public abstract AttachedBodySynchronizer getSynchronizer(T on);

    @Override
    public SyncTarget getValueFrom(T entity, PhysicsEntityNetHandler<T> network, Side side, int syncTick) {
        boolean changed = !transforms.isEmpty() && syncTick%20==0; //Keep low-rate sync while not moving, if we have joints to sync
        if(syncTick%20==0)
            SyncTracker.addChange("r_pos", "keep_sync");

        AttachedBodySynchronizer synchronizer = getSynchronizer(entity);
        if(synchronizer != null) {
            if (!changed) {
                for (Map.Entry<Byte, SynchronizedRigidBodyTransform> transform : synchronizer.getTransforms().entrySet()) {
                    if (!transforms.containsKey(transform.getKey())) {
                        SyncTracker.addChange("r_pos", "add_j" + transform.getKey());
                        changed = true;
                        transforms.put(transform.getKey(), new RigidBodyTransform(transform.getValue().getPhysicTransform()));
                    } else {
                        RigidBodyTransform pos = transform.getValue().getPhysicTransform();
                        RigidBodyTransform ctransform = transforms.get(transform.getKey());
                        if (SyncTracker.different(pos.getPosition().x, ctransform.getPosition().x) || SyncTracker.different(pos.getPosition().y, ctransform.getPosition().y) || SyncTracker.different(pos.getPosition().z, ctransform.getPosition().z)) {
                            SyncTracker.addChange("r_pos", "pos_j" + transform.getKey());
                            changed = true;
                        } else if (SyncTracker.different(pos.getRotation().getX(), ctransform.getRotation().getX()) || SyncTracker.different(pos.getRotation().getY(), ctransform.getRotation().getY()) ||
                                SyncTracker.different(pos.getRotation().getZ(), ctransform.getRotation().getZ()) || SyncTracker.different(pos.getRotation().getW(), ctransform.getRotation().getW())) {
                            changed = true;
                            SyncTracker.addChange("r_pos", "rotation_j" + transform.getKey());
                        }
                    }
                }
                if (synchronizer.getTransforms().size() != transforms.size()) {
                    changed = true;
                    transforms.keySet().removeIf(tr -> {
                        if (!synchronizer.getTransforms().containsKey(tr)) {
                            SyncTracker.addChange("r_pos", "rm_j" + tr);
                            return true;
                        }
                        return false;
                    });
                }
            }
            if (changed) {
                for (Map.Entry<Byte, SynchronizedRigidBodyTransform> transform : synchronizer.getTransforms().entrySet()) {
                    transforms.get(transform.getKey()).set(transform.getValue().getPhysicTransform());
                }
            }
        }
        return changed ? SyncTarget.SPECTATORS_PEDESTRIANS : SyncTarget.NONE;
    }

    @Override
    public void setValueTo(T entity, PhysicsEntityNetHandler<T> network, MessagePhysicsEntitySync msg, Side side)
    {
        if(network.getSimulationHolder().isSinglePlayer())
        {
            if(side.isServer()) //Solo mode
            {
                if(getSynchronizer(entity) != null) {
                    Map<Byte, SynchronizedRigidBodyTransform> tar = getSynchronizer(entity).getTransforms();//entity.getModuleByType(DoorsModule.class).getTransforms();
                    transforms.forEach((b, t) -> {
                        if(!tar.containsKey(b))
                            tar.put(b, new SynchronizedRigidBodyTransform(t));
                        else
                            tar.get(b).getPhysicTransform().set(t);
                    });
                    if (tar.size() != transforms.size()) {
                        tar.keySet().removeIf(b -> !transforms.containsKey(b));
                    }
                }
            }
            else
                DynamXMain.log.error("Incorrect simulation holder in client set pos value : "+network.getSimulationHolder());
        }
        else //Physics "receiver" side
        {
            for(Map.Entry<Byte, RigidBodyTransform> transform : transforms.entrySet())
            {
                getSynchronizer(entity).setPhysicsTransform(transform.getKey(), transform.getValue());
            }
        }
    }

    @Override
    public void interpolate(T entity, PhysicsEntityNetHandler<T> network, Profiler profiler, MessagePhysicsEntitySync msg, int step) {
       // if(ClientDebugSystem.MOVE_DEBUG > 0)
            //System.out.println("Interpolate "+step+" "+msg+" "+DynamXMain.proxy.ownsSimulation(entity));
        /*if(DynamXMain.proxy.ownsSimulation(entity) && false) //If we are simulating this entity
        {
            if(!network.getSimulationHolder().isMe(Side.CLIENT)) {
                if (interpolatingState == null && msg != null) //If interpolation isn't started
                {
                    //System.out.println("Packet received at "+ ClientPhysicsSyncManager.simulationTime+", time is "+ msg.simulationTimeClient +" for entity "+entity.getEntityId()+" rcv at ticks existed "+entity.ticksExisted);
                    EntityPhysicsState state = entity.getNetwork().getStateAndClearOlders(msg.getSimulationTimeClient()); //Get the state corresponding to the tick of the data
                    interpolatingState = (AttachBodyPhysicsState) state; //Interpolate over it

                    //Debug
                    //float[] rotations = DynamXUtils.quaternionToEuler(rotation, entity.rotationYaw, entity.rotationPitch, entity.rotationRoll); //Prev or not prev ?
                    if (interpolatingState != null) {
                        for(Map.Entry<Byte, RigidBodyTransform> transform : transforms.entrySet())
                        {
                            //Smoothly correct entity pos from server's data, only if we are not driving this entity
                            interpolatingState.interpolateDeltas(part, transforms.get(part), entity.getSyncTickRate(), 0);
                        }
                    } else if (ClientDebugSystem.MOVE_DEBUG > 0)
                        System.err.println("State of " + msg.getSimulationTimeClient() + " not found " + entity.getNetwork().getOldStates());
                } else if (interpolatingState != null && !network.getSimulationHolder().isMe(Side.CLIENT)) //Second interpolation step
                    for(Map.Entry<Byte, RigidBodyTransform> transform : transforms.entrySet())
                    {
                        interpolatingState.interpolateDeltas(part, transforms.get(part), entity.getSyncTickRate(), 1);
                    }
            }
            //else ignore
        }
        else*/
       // if(!(entity instanceof BaseVehicleEntity) || !DynamXMain.proxy.ownsSimulation(entity))
        {
            //Else we just interpolate
            //Thanks to Flan's mod and Mojang for the idea of this code

            Map<Byte, SynchronizedRigidBodyTransform> tar = getSynchronizer(entity).getTransforms();//entity.getModuleByType(DoorsModule.class).getTransforms();
            for(Map.Entry<Byte, RigidBodyTransform> transform : transforms.entrySet())
            {
                if(getSynchronizer(entity).getTransforms().containsKey(transform.getKey())) { //Joint has been added
                    //tar.put(transform.getKey(), new RigidBodyTransform());
                    SynchronizedRigidBodyTransform var = tar.get(transform.getKey());
                    var.getPhysicTransform().setPosition(
                            Vector3fPool.get(DynamXMath.interpolateLinear(1f / step, var.getPhysicTransform().getPosition().x, transform.getValue().getPosition().x),
                                    DynamXMath.interpolateLinear(1f / step, var.getPhysicTransform().getPosition().y, transform.getValue().getPosition().y),
                                    DynamXMath.interpolateLinear(1f / step, var.getPhysicTransform().getPosition().z, transform.getValue().getPosition().z)));
                    //entity.onMove(entity.motionX, entity.motionY, entity.motionZ);
                    //entity.setPosition(x, y, z);
                    DynamXGeometry.slerp(var.getPhysicTransform().getRotation(), transform.getValue().getRotation(),
                            var.getPhysicTransform().getRotation(), 1f / step);

                    //When the interpolation is finished, we set the new rotation into bullet because it may be used by the prediction system
                    if (step == 1 && DynamXMain.proxy.shouldUseBulletSimulation(entity.world)) {
                        getSynchronizer(entity).setPhysicsTransform(transform.getKey(), transform.getValue());
                    }
                }
            }
        }
    }

    @Override
    public void write(ByteBuf buf, boolean compress) {
        if(compress) {
            buf.writeByte(1);
            RigidBodyTransform transform = transforms.get((byte) EnumRagdollBodyPart.CHEST.ordinal());
            buf.writeByte(EnumRagdollBodyPart.CHEST.ordinal());
            writeTransform(buf, transform);
        }
        else {
            buf.writeByte(transforms.size());
            for (byte tr : transforms.keySet()) {
                buf.writeByte(tr);
                RigidBodyTransform transform = transforms.get(tr);
                writeTransform(buf, transform);
            }
        }
    }

    @Override
    public void writeEntityValues(T entity, ByteBuf buf) {
        AttachedBodySynchronizer synchronizer = getSynchronizer(entity);
        buf.writeByte(synchronizer.getTransforms().size());
        for (byte tr : synchronizer.getTransforms().keySet()) {
            buf.writeByte(tr);
            RigidBodyTransform transform = synchronizer.getTransforms().get(tr).getTransform();
            writeTransform(buf, transform);
        }
    }

    private void writeTransform(ByteBuf buf, RigidBodyTransform transform) {
        buf.writeFloat(transform.getPosition().x);
        buf.writeFloat(transform.getPosition().y);
        buf.writeFloat(transform.getPosition().z);

        buf.writeFloat(transform.getRotation().getX());
        buf.writeFloat(transform.getRotation().getY());
        buf.writeFloat(transform.getRotation().getZ());
        buf.writeFloat(transform.getRotation().getW());
    }

    @Override
    public void read(ByteBuf buf) {
        transforms.clear();
        byte size = buf.readByte();
        for (byte i = 0; i < size; i++)
        {
            byte tr = buf.readByte();
            RigidBodyTransform transform = new RigidBodyTransform();

            transform.getPosition().set(buf.readFloat(), buf.readFloat(), buf.readFloat());
            transform.getRotation().set(buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat());

            transforms.put(tr, transform);
        }
    }

    /**
     * Handles sync of attached bodies, via the {@link AttachedBodySynchronizer}
     */
    public interface AttachedBodySynchronizer
    {
        /**
         * @return A map giving one {@link SynchronizedRigidBodyTransform} for each joint (the key is the jointId) <br>
         *     Keep this map in a field of this module
         */
        Map<Byte, SynchronizedRigidBodyTransform> getTransforms();

        /**
         * Alters the physics state on the object attached to the given joint <br>
         *     Fired when receiving a sync and physics are enabled on this side
         */
        void setPhysicsTransform(byte jointId, RigidBodyTransform transform);
    }
}
