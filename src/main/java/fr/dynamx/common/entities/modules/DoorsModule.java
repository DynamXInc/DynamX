package fr.dynamx.common.entities.modules;

import com.jme3.bullet.RotationOrder;
import com.jme3.bullet.collision.shapes.BoxCollisionShape;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.joints.Constraint;
import com.jme3.bullet.joints.New6Dof;
import com.jme3.bullet.joints.motors.MotorParam;
import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import fr.dynamx.api.entities.IModuleContainer;
import fr.dynamx.api.entities.modules.AttachModule;
import fr.dynamx.api.entities.modules.IPhysicsModule;
import fr.dynamx.api.network.sync.SimulationHolder;
import fr.dynamx.api.network.sync.v3.SynchronizationRules;
import fr.dynamx.api.network.sync.v3.SynchronizedEntityVariable;
import fr.dynamx.api.network.sync.v3.SynchronizedVariableSerializer;
import fr.dynamx.api.physics.BulletShapeType;
import fr.dynamx.api.physics.EnumBulletShapeType;
import fr.dynamx.client.ClientProxy;
import fr.dynamx.client.renders.RenderPhysicsEntity;
import fr.dynamx.client.renders.model.ObjModelClient;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.DynamXMain;
import fr.dynamx.common.contentpack.parts.PartDoor;
import fr.dynamx.common.entities.BaseVehicleEntity;
import fr.dynamx.common.entities.PhysicsEntity;
import fr.dynamx.common.handlers.TaskScheduler;
import fr.dynamx.common.network.packets.MessageChangeDoorState;
import fr.dynamx.common.network.sync.v3.DynamXSynchronizedVariables;
import fr.dynamx.common.network.sync.vars.AttachedBodySynchronizedVariable;
import fr.dynamx.common.physics.entities.AbstractEntityPhysicsHandler;
import fr.dynamx.common.physics.joints.EntityJoint;
import fr.dynamx.common.physics.joints.JointHandler;
import fr.dynamx.common.physics.joints.JointHandlerRegistry;
import fr.dynamx.common.physics.utils.RigidBodyTransform;
import fr.dynamx.common.physics.utils.SynchronizedRigidBodyTransform;
import fr.dynamx.utils.DynamXConstants;
import fr.dynamx.utils.client.ClientDynamXUtils;
import fr.dynamx.utils.maths.DynamXGeometry;
import fr.dynamx.utils.maths.DynamXMath;
import fr.dynamx.utils.optimization.QuaternionPool;
import fr.dynamx.utils.optimization.Vector3fPool;
import fr.dynamx.utils.physics.DynamXPhysicsHelper;
import io.netty.buffer.ByteBuf;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DoorsModule implements IPhysicsModule<AbstractEntityPhysicsHandler<?, ?>>, AttachModule.AttachToSelfModule,
        IPhysicsModule.IEntityPosUpdateListener, IPhysicsModule.IPhysicsUpdateListener,
        AttachedBodySynchronizedVariable.AttachedBodySynchronizer, IPhysicsModule.IDrawableModule<BaseVehicleEntity<?>> {
    public static final ResourceLocation JOINT_NAME = new ResourceLocation(DynamXConstants.ID, "door_module");

    static {
        JointHandlerRegistry.register(new JointHandler(JOINT_NAME, BaseVehicleEntity.class, BaseVehicleEntity.class, DoorsModule.class));
    }

    public final BaseVehicleEntity<?> vehicleEntity;
    private final Map<Byte, DoorVarContainer> attachedDoors = new HashMap<>();
    private final Map<Byte, DoorVarContainer> cachedAttachedDoors = new HashMap<>();
    private final HashMap<Byte, SynchronizedRigidBodyTransform> attachedBodiesTransform = new HashMap<>();
    @Getter
    protected final SynchronizedEntityVariable<Map<Byte, DoorState>> doorsState;

    public DoorsModule(BaseVehicleEntity<?> vehicleEntity) {
        this.vehicleEntity = vehicleEntity;

        doorsState = new SynchronizedEntityVariable<>((variable, doorsState) -> {
            Map<Byte, DoorsModule.DoorState> target = variable.get();
            //TODO NEW SYNC DEBUG THIS
            if (true /*|| network.getSimulationHolder() != SimulationHolder.SERVER_SP*/) {
                doorsState.forEach((i, b) -> {
                    if (b == DoorsModule.DoorState.OPEN && (!target.containsKey(i) || target.get(i) == DoorsModule.DoorState.CLOSE)) {
                        ((IModuleContainer.IDoorContainer) vehicleEntity).getDoors().setDoorState(i, DoorsModule.DoorState.OPEN);
                    } else if (b == DoorsModule.DoorState.CLOSE && (target.containsKey(i) && target.get(i) == DoorsModule.DoorState.OPEN)) {
                        ((IModuleContainer.IDoorContainer) vehicleEntity).getDoors().setDoorState(i, DoorsModule.DoorState.CLOSE);
                    }
                });
            } else {
                doorsState.forEach((i, b) -> {
                    if (b != target.get(i)) {
                        target.put(i, b);
                        DoorsModule doors = ((IModuleContainer.IDoorContainer) vehicleEntity).getDoors();
                        doors.playDoorSound(b == DoorsModule.DoorState.OPEN ? DoorsModule.DoorState.CLOSE : DoorsModule.DoorState.OPEN);
                    }
                });
            }
        }, SynchronizationRules.SERVER_TO_CLIENTS, DynamXSynchronizedVariables.doorsStatesSerializer, new HashMap<>(), "door_states");
    }

    @Override
    public boolean canCreateJoint(PhysicsEntity<?> withEntity, byte jointId) {
        return !attachedDoors.containsKey(jointId);
    }

    @Override
    public void onJointDestroyed(EntityJoint<?> joint) {
        //TODO SPAWN DOOR ENTITY IF BROKEN
        //((IModuleContainer.IDoorContainer)entity).getDoors().detachDoor(doorID);
        //System.out.println("RM DOOR " + joint);
        attachedBodiesTransform.remove(joint.getJointId());
        attachedDoors.remove(joint.getJointId());
        if (vehicleEntity.physicsHandler != null) {
            vehicleEntity.physicsHandler.getCollisionObject().removeFromIgnoreList(joint.getJoint().getBodyB());
        }
    }

    @Override
    public Constraint createJoint(byte jointId) {
        PartDoor partDoor = getPartDoor(jointId);
        Vector3f p1 = Vector3fPool.get(partDoor.getCarAttachPoint());
        Vector3f p2 = Vector3fPool.get(partDoor.getDoorAttachPoint());

        DoorVarContainer localVarContainer = new DoorVarContainer();
        localVarContainer.setModule(this);
        localVarContainer.setDoorID(jointId);

        Quaternion doorRotation = QuaternionPool.get(vehicleEntity.physicsRotation);
        Vector3f doorPos = DynamXGeometry.rotateVectorByQuaternion(p1.subtract(p2).addLocal(vehicleEntity.getPackInfo().getCenterOfMass()), doorRotation).addLocal(vehicleEntity.physicsPosition);
        CollisionShape doorShape = new BoxCollisionShape(partDoor.getScale());
        if(partDoor.getPhysicsCollisionShape() != null)
            doorShape = partDoor.getPhysicsCollisionShape();
        PhysicsRigidBody doorBody = DynamXPhysicsHelper.fastCreateRigidBody(vehicleEntity, 40, doorShape, doorPos, vehicleEntity.rotationYaw);
        localVarContainer.setDoorBody(doorBody);
        doorBody.setUserObject(new BulletShapeType<>(EnumBulletShapeType.BULLET_ENTITY, localVarContainer));
        DynamXContext.getPhysicsWorld().addCollisionObject(doorBody);

        attachedDoors.forEach((doorId, doorVarContainer) -> {
            doorBody.addToIgnoreList(doorVarContainer.doorBody);
            doorVarContainer.doorBody.addToIgnoreList(doorBody);
        });

        New6Dof new6Dof = new New6Dof(
                vehicleEntity.physicsHandler.getCollisionObject(), doorBody,
                p1.addLocal(vehicleEntity.getPackInfo().getCenterOfMass()), p2,
                Quaternion.IDENTITY.toRotationMatrix(), Quaternion.IDENTITY.toRotationMatrix(),
                RotationOrder.XYZ);
        localVarContainer.setJoint(new6Dof);
        localVarContainer.setJointLimit(DynamXPhysicsHelper.X_ROTATION_DOF, 0, 0);
        localVarContainer.setJointLimit(DynamXPhysicsHelper.Y_ROTATION_DOF, 0, 0);
        localVarContainer.setJointLimit(DynamXPhysicsHelper.Z_ROTATION_DOF, 0, 0);
        new6Dof.setCollisionBetweenLinkedBodies(false);

        attachedBodiesTransform.put(jointId, new SynchronizedRigidBodyTransform(new RigidBodyTransform(doorBody)));
        attachedDoors.put(jointId, localVarContainer);
        cachedAttachedDoors.put(jointId, localVarContainer);
        return new6Dof;
    }

    @Override
    public void initEntityProperties() {
        for (PartDoor partDoor : vehicleEntity.getPackInfo().getPartsByType(PartDoor.class)) {
            doorsState.get().put(partDoor.getId(), DoorState.CLOSE);
        }
        doorsState.setChanged(true);
    }

    @Override
    public void addSynchronizedVariables(Side side, SimulationHolder simulationHolder) {
        if (simulationHolder.isPhysicsAuthority(side)) {
            //TODO SYNC variables.add(AttachedDoorsSynchronizedVariable.NAME);
        }
        vehicleEntity.getSynchronizer().registerVariable(DynamXSynchronizedVariables.DOORS_STATES, doorsState);
    }

    @Override
    public void initPhysicsEntity(AbstractEntityPhysicsHandler<?, ?> handler) {
        if (!vehicleEntity.world.isRemote) {
            vehicleEntity.getPackInfo().getPartsByType(PartDoor.class).forEach(this::spawnDoor);
        }
    }


    public boolean isDoorAttached(byte doorID) {
        for (EntityJoint<?> entityJoint : vehicleEntity.getJointsHandler().getJoints()) {
            if (entityJoint.getType() == JOINT_NAME && entityJoint.getJointId() == doorID) {
                return true;
            }
        }
        return false;//attachedDoors.containsKey(doorID);
    }


    public void spawnDoor(PartDoor door) {
        if (door.isEnabled()) {
            DynamXMain.proxy.scheduleTask(vehicleEntity.world, () -> JointHandlerRegistry.createJointWithSelf(JOINT_NAME, vehicleEntity, door.getId()));
        }
    }

    @Override
    public void preUpdatePhysics(boolean simulatingPhysics) {
        if (simulatingPhysics) {
            for (byte doorID : attachedDoors.keySet()) {
                DoorVarContainer varContainer = attachedDoors.get(doorID);
                PartDoor door = getPartDoor(doorID);
                if (getCurrentState(doorID) == DoorState.OPENING && isDoorJointOpened(door, varContainer)) {
                    DynamXContext.getNetwork().sendToServer(new MessageChangeDoorState(vehicleEntity, DoorState.OPEN, doorID));
                    doorsState.get().put(doorID, DoorState.OPEN);
                    doorsState.setChanged(true);
                    varContainer.setJointMotorState(door.getAxisToUse(), false);
                }
                if ((isDoorOpened(doorID) || getCurrentState(doorID) == DoorState.CLOSING) && isDoorJointClosed(varContainer)) {
                    DynamXContext.getNetwork().sendToServer(new MessageChangeDoorState(vehicleEntity, DoorState.CLOSE, doorID));
                    doorsState.get().put(doorID, DoorState.CLOSE);
                    doorsState.setChanged(true);
                    playDoorSound(DoorState.CLOSE);
                    varContainer.setJointMotorState(door.getAxisToUse(), false);
                    varContainer.setJointLimit(door.getAxisToUse(), door.getCloseLimit().x, door.getCloseLimit().y);
                }
                //System.out.println(doorID +" " +getCurrentState(doorID) + " " + varContainer.getJointAngle().y);
            }
        }
    }

    @Override
    public void postUpdatePhysics(boolean simulatingPhysics) {
        if (simulatingPhysics) {
            for (byte door : attachedDoors.keySet()) {
                PhysicsRigidBody body = attachedDoors.get(door).doorBody;
                attachedBodiesTransform.get(door).getPhysicTransform().set(body);
                //attachedBodiesTransform.get(door).updatePos();
            }
        } else {
            /*for (byte door : attachedDoors.keySet()) {
                attachedBodiesTransform.get(door).updatePos();
            }*/
        }
    }


    public boolean isDoorOpened(byte doorID) {
        if (doorsState == null)
            return true;
        return !doorsState.get().containsKey(doorID) || doorsState.get().get(doorID) == DoorState.OPEN;
    }


    public void switchDoorState(byte doorId) {
        setDoorState(doorId, getInverseCurrentState(doorId));
    }

    public void setDoorState(byte doorId, DoorState doorState) {
        DoorVarContainer doorVarContainer = attachedDoors.get(doorId);
        PartDoor door = getPartDoor(doorId);
        if (doorVarContainer != null) {
            vehicleEntity.forcePhysicsActivation();
            if (doorState == DoorState.OPEN) {
                doorsState.get().put(doorId, DoorState.OPENING);
                playDoorSound(DoorState.OPEN);
                doorVarContainer.setJointLimit(door.getAxisToUse(), door.getOpenLimit().x, door.getOpenLimit().y);
                doorVarContainer.setJointRotationMotorVelocity(door.getAxisToUse(), door.getOpenMotor().x, door.getOpenMotor().y);
            } else {
                doorVarContainer.setJointRotationMotorVelocity(door.getAxisToUse(), door.getCloseMotor().x, door.getCloseMotor().y);
                // if (vehicleEntity.getNetwork().getSimulationHolder() != SimulationHolder.SERVER_SP) //TODO SERVER_SP SPLITTED IN SERVER_SP AND DRIVE_SP
                doorsState.get().put(doorId, DoorState.CLOSING); //Closes the door (animation) and the plays the closing sound

            }
        } else {
            if (doorState == DoorState.OPEN) {
                doorsState.get().put(doorId, DoorState.OPEN);
                playDoorSound(DoorState.OPEN);
            } else {
                //if (entity.getNetwork().getSimulationHolder() != SimulationHolder.SERVER_SP)
                doorsState.get().put(doorId, DoorState.CLOSE); //Closes the door (animation) and the plays the closing sound
                TaskScheduler.schedule(new TaskScheduler.ScheduledTask(door.getDoorCloseTime()) {
                    @Override
                    public void run() {
                        //if (entity.getNetwork().getSimulationHolder() == SimulationHolder.SERVER_SP)
                        //  doorsStatus.put(doorId, false); //Only plays the closing sound
                        playDoorSound(DoorState.CLOSE);
                    }
                });
            }
        }
        doorsState.setChanged(true);
    }


    private boolean isDoorJointClosed(DoorVarContainer doorVarContainer) {
        float roundedCurrentAngle = DynamXMath.roundFloat(doorVarContainer.getJointAngle(), 100f);
        return FastMath.approximateEquals(roundedCurrentAngle, 0.0f);
    }

    private boolean isDoorJointOpened(PartDoor door, DoorVarContainer doorVarContainer) {
        float extreme = doorVarContainer.getJointExtremeLimit(door.getOpenLimit().x, door.getOpenLimit().y);
        float roundedExtreme = DynamXMath.roundFloat(extreme, 100f);
        float roundedCurrentAngle = DynamXMath.roundFloat(doorVarContainer.getJointAngle(), 100f);
        return roundedExtreme >= 0 ? roundedCurrentAngle >= roundedExtreme : roundedCurrentAngle <= roundedExtreme;
    }


    public void playDoorSound(DoorState doorState) {
        if (vehicleEntity.world.isRemote) {
            ClientProxy.SOUND_HANDLER.playSingleSound(vehicleEntity.physicsPosition, doorState == DoorState.CLOSE ? "door_close" : "door_open", 1, 1);
        }
    }

    @Override
    public void onRemovedFromWorld() {
        if (DynamXMain.proxy.shouldUseBulletSimulation(vehicleEntity.world)) {
            for (DoorVarContainer body : cachedAttachedDoors.values())
                DynamXContext.getPhysicsWorld().removeCollisionObject(body.doorBody);
            cachedAttachedDoors.clear();
        }
    }

    @Override
    public void setPhysicsTransform(byte jointId, RigidBodyTransform transform) {
        if (attachedDoors.containsKey(jointId)) {
            attachedDoors.get(jointId).doorBody.setPhysicsLocation(transform.getPosition());
            attachedDoors.get(jointId).doorBody.setPhysicsRotation(transform.getRotation());
        }
    }

    @Override
    public void updateEntityPos() {
        attachedBodiesTransform.values().forEach(SynchronizedRigidBodyTransform::updatePos);
    }

    @Override
    public void drawParts(RenderPhysicsEntity<?> render, float partialTicks, BaseVehicleEntity<?> carEntity) {
        List<PartDoor> doors = carEntity.getPackInfo().getPartsByType(PartDoor.class);
        for (byte id = 0; id < doors.size(); id++) {
            PartDoor door = doors.get(id);
            GlStateManager.pushMatrix();
            if (!door.isEnabled()) {
                Vector3f pos = Vector3fPool.get().addLocal(door.getCarAttachPoint());
                pos.subtract(door.getDoorAttachPoint(), pos);

                GlStateManager.translate(pos.x, pos.y, pos.z);
            } else if (getTransforms().containsKey(id)) {
                SynchronizedRigidBodyTransform sync = getTransforms().get(id);
                RigidBodyTransform transform = sync.getTransform();
                RigidBodyTransform prev = sync.getPrevTransform();

                Vector3f pos = Vector3fPool.get(prev.getPosition()).addLocal(transform.getPosition().subtract(prev.getPosition(), Vector3fPool.get()).multLocal(partialTicks));

                GlStateManager.rotate(ClientDynamXUtils.computeInterpolatedGlQuaternion(carEntity.prevRenderRotation, carEntity.renderRotation, partialTicks, true));
                GlStateManager.translate(
                        (float) -(carEntity.prevPosX + (carEntity.posX - carEntity.prevPosX) * partialTicks),
                        (float) -(carEntity.prevPosY + (carEntity.posY - carEntity.prevPosY) * partialTicks),
                        (float) -(carEntity.prevPosZ + (carEntity.posZ - carEntity.prevPosZ) * partialTicks));
                GlStateManager.translate(pos.x, pos.y, pos.z);
                GlStateManager.rotate(ClientDynamXUtils.computeInterpolatedGlQuaternion(prev.getRotation(), transform.getRotation(), partialTicks));

            }

            ObjModelClient vehicleModel = DynamXContext.getObjModelRegistry().getModel(carEntity.getPackInfo().getModel());
            GlStateManager.scale(carEntity.getPackInfo().getScaleModifier().x, carEntity.getPackInfo().getScaleModifier().y, carEntity.getPackInfo().getScaleModifier().z);
            render.renderModelGroup(vehicleModel, door.getPartName(), carEntity, carEntity.getEntityTextureID());
            GlStateManager.scale(1 / carEntity.getPackInfo().getScaleModifier().x, 1 / carEntity.getPackInfo().getScaleModifier().y, 1 / carEntity.getPackInfo().getScaleModifier().z);

            GlStateManager.popMatrix();
        }
    }

    public PartDoor getPartDoor(byte doorID) {
        return vehicleEntity.getPackInfo().getPartByTypeAndId(PartDoor.class, doorID);
    }

    public DoorVarContainer getDoorVarContainer(byte doorId) {
        return attachedDoors.get(doorId);
    }

    public DoorState getCurrentState(byte doorId) {
        return doorsState.get().get(doorId);
    }

    public DoorState getInverseCurrentState(byte doorId) {
        return getCurrentState(doorId) == DoorState.OPEN ? DoorState.CLOSE : DoorState.OPEN;
    }

    @Override
    public Map<Byte, SynchronizedRigidBodyTransform> getTransforms() {
        return attachedBodiesTransform;
    }

    public enum DoorState {
        OPEN, CLOSE, OPENING, CLOSING
    }

    @Getter
    @Setter
    public static class DoorVarContainer {
        private DoorsModule module;
        private byte doorID;
        private PhysicsRigidBody doorBody;
        private New6Dof joint;

        public float getJointAngle() {
            return joint.getAngles(null).get(module.getPartDoor(doorID).getAxisToUse()-3);
        }

        public void setJointRotationMotorVelocity(int axis, float targetVelocity, float maxForce) {
            setJointMotorState(axis, true);
            joint.set(MotorParam.TargetVelocity, axis, targetVelocity);
            joint.set(MotorParam.MaxMotorForce, axis, maxForce);
        }

        public void setJointLimit(int axis, float lowerLimit, float upperLimit) {
            joint.set(MotorParam.LowerLimit, axis, lowerLimit);
            joint.set(MotorParam.UpperLimit, axis, upperLimit);
        }

        private float getJointExtremeLimit(float lowerLimit, float upperLimit) {
            if (lowerLimit >= 0 && upperLimit >= 0) {
                return Math.max(lowerLimit, upperLimit);
            } else {
                return Math.min(lowerLimit, upperLimit);
            }
        }

        public void setJointMotorState(int axis, boolean state) {
            if (axis < 3) {
                joint.getTranslationMotor().setMotorEnabled(axis, state);
            } else {
                joint.getRotationMotor(axis - 3).setMotorEnabled(state);
            }
        }
    }
}
