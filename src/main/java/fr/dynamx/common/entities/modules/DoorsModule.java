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
import fr.dynamx.api.entities.modules.AttachModule;
import fr.dynamx.api.entities.modules.IPhysicsModule;
import fr.dynamx.common.network.sync.variables.EntityMapVariable;
import fr.dynamx.common.network.sync.variables.EntityTransformsVariable;
import fr.dynamx.api.network.sync.SynchronizationRules;
import fr.dynamx.api.physics.BulletShapeType;
import fr.dynamx.api.physics.EnumBulletShapeType;
import fr.dynamx.client.ClientProxy;
import fr.dynamx.client.renders.RenderPhysicsEntity;
import fr.dynamx.client.renders.model.renderer.ObjModelRenderer;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.DynamXMain;
import fr.dynamx.common.contentpack.parts.PartDoor;
import fr.dynamx.common.entities.BaseVehicleEntity;
import fr.dynamx.common.entities.PhysicsEntity;
import fr.dynamx.common.network.packets.MessageChangeDoorState;
import fr.dynamx.api.network.sync.SynchronizedEntityVariable;
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
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SynchronizedEntityVariable.SynchronizedPhysicsModule()
public class DoorsModule implements IPhysicsModule<AbstractEntityPhysicsHandler<?, ?>>, AttachModule.AttachToSelfModule,
        IPhysicsModule.IEntityPosUpdateListener, IPhysicsModule.IPhysicsUpdateListener,
        AttachedBodySynchronizedVariable.AttachedBodySynchronizer, IPhysicsModule.IDrawableModule<BaseVehicleEntity<?>> {
    public static final ResourceLocation JOINT_NAME = new ResourceLocation(DynamXConstants.ID, "door_module");

    static {
        JointHandlerRegistry.register(new JointHandler(JOINT_NAME, BaseVehicleEntity.class, BaseVehicleEntity.class, DoorsModule.class));
    }

    public final BaseVehicleEntity<?> vehicleEntity;
    private final Map<Byte, DoorPhysics> attachedDoors = new HashMap<>();
    private final HashMap<Byte, SynchronizedRigidBodyTransform> attachedBodiesTransform = new HashMap<>();

    @SynchronizedEntityVariable(name = "door_states")
    private final EntityTransformsVariable synchronizedTransforms;
    @Getter
    @SynchronizedEntityVariable(name = "dparts_pos")
    protected final EntityMapVariable<Map<Byte, DoorsModule.DoorState>, Byte, DoorState> doorsState;

    public DoorsModule(BaseVehicleEntity<?> vehicleEntity) {
        this.vehicleEntity = vehicleEntity;

        doorsState = new EntityMapVariable<>((variable, doorsState) -> {
            Map<Byte, DoorsModule.DoorState> target = variable.get();
            doorsState.forEach((i, b) -> {
                if (!target.containsKey(i) || target.get(i) != b)
                    setDoorState(i, b);
            });
        }, SynchronizationRules.SERVER_TO_CLIENTS);
        synchronizedTransforms = new EntityTransformsVariable(vehicleEntity, this);
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

        DoorPhysics localVarContainer = new DoorPhysics();
        localVarContainer.setModule(this);
        localVarContainer.setDoorID(jointId);

        Quaternion doorRotation = QuaternionPool.get(vehicleEntity.physicsRotation);
        Vector3f doorPos = DynamXGeometry.rotateVectorByQuaternion(p1.subtract(p2).addLocal(vehicleEntity.getPackInfo().getCenterOfMass()), doorRotation).addLocal(vehicleEntity.physicsPosition);
        CollisionShape doorShape = new BoxCollisionShape(partDoor.getScale());
        if (partDoor.getPhysicsCollisionShape() != null)
            doorShape = partDoor.getPhysicsCollisionShape();
        PhysicsRigidBody doorBody = DynamXPhysicsHelper.fastCreateRigidBody(vehicleEntity, 40, doorShape, doorPos, vehicleEntity.rotationYaw);
        localVarContainer.setDoorBody(doorBody);
        doorBody.setUserObject(new BulletShapeType<>(EnumBulletShapeType.BULLET_ENTITY, localVarContainer));
        DynamXContext.getPhysicsWorld(vehicleEntity.world).addCollisionObject(doorBody);

        attachedDoors.forEach((doorId, doorPhysics) -> {
            doorBody.addToIgnoreList(doorPhysics.doorBody);
            doorPhysics.doorBody.addToIgnoreList(doorBody);
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
        return new6Dof;
    }

    @Override
    public void initEntityProperties() {
        for (PartDoor partDoor : vehicleEntity.getPackInfo().getPartsByType(PartDoor.class)) {
            doorsState.put(partDoor.getId(), DoorState.CLOSED);
        }
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
        synchronizedTransforms.setChanged(true);
        if (simulatingPhysics) {
            for (byte doorID : attachedDoors.keySet()) {
                DoorPhysics varContainer = attachedDoors.get(doorID);
                PartDoor door = getPartDoor(doorID);
                if (getCurrentState(doorID) == DoorState.OPENING && isDoorJointOpened(door, varContainer)) {
                    if (vehicleEntity.world.isRemote && vehicleEntity.getSynchronizer().getSimulationHolder().isPhysicsAuthority(Side.CLIENT))
                        DynamXContext.getNetwork().sendToServer(new MessageChangeDoorState(vehicleEntity, DoorState.OPENED, doorID));
                    setDoorState(doorID, DoorState.OPENED);
                } else if ((getCurrentState(doorID) == DoorState.OPENED || getCurrentState(doorID) == DoorState.CLOSING) && isDoorJointClosed(varContainer)) {
                    if (vehicleEntity.world.isRemote && vehicleEntity.getSynchronizer().getSimulationHolder().isPhysicsAuthority(Side.CLIENT))
                        DynamXContext.getNetwork().sendToServer(new MessageChangeDoorState(vehicleEntity, DoorState.CLOSED, doorID));
                    setDoorState(doorID, DoorState.CLOSED);
                }
            }
        }
    }

    @Override
    public void postUpdatePhysics(boolean simulatingPhysics) {
        if (simulatingPhysics) {
            for (byte door : attachedDoors.keySet()) {
                PhysicsRigidBody body = attachedDoors.get(door).doorBody;
                attachedBodiesTransform.get(door).getPhysicTransform().set(body);
            }
        }
    }


    public boolean isDoorOpened(byte doorID) {
        if (doorsState == null)
            return true;
        return !doorsState.get().containsKey(doorID) || doorsState.get().get(doorID) == DoorState.OPENED || doorsState.get().get(doorID) == DoorState.OPENING;
    }

    public void switchDoorState(byte doorId) {
        setDoorState(doorId, getInverseCurrentState(doorId));
    }

    public void setDoorState(byte doorId, DoorState doorState) {
        DoorPhysics doorPhysics = attachedDoors.get(doorId);
        PartDoor door = getPartDoor(doorId);
        boolean usePhysics = doorPhysics != null;
        if (usePhysics)
            vehicleEntity.forcePhysicsActivation();
        doorsState.put(doorId, doorState);
        switch (doorState) {
            case OPENING:
                playDoorSound(DoorState.OPENED);
                if (usePhysics) {
                    doorPhysics.setJointLimit(door.getAxisToUse(), door.getOpenLimit().x, door.getOpenLimit().y);
                    doorPhysics.setJointRotationMotorVelocity(door.getAxisToUse(), door.getOpenMotor().x, door.getOpenMotor().y);
                }
                break;
            case OPENED:
                if (usePhysics)
                    doorPhysics.setJointMotorState(door.getAxisToUse(), false);
                break;
            case CLOSING:
                if (usePhysics)
                    doorPhysics.setJointRotationMotorVelocity(door.getAxisToUse(), door.getCloseMotor().x, door.getCloseMotor().y);
                break;
            case CLOSED:
                playDoorSound(DoorState.CLOSED);
                if (usePhysics) {
                    doorPhysics.setJointMotorState(door.getAxisToUse(), false);
                    doorPhysics.setJointLimit(door.getAxisToUse(), door.getCloseLimit().x, door.getCloseLimit().y);
                }
                break;
        }
    }

    private boolean isDoorJointClosed(DoorPhysics doorPhysics) {
        float roundedCurrentAngle = DynamXMath.roundFloat(doorPhysics.getJointAngle(), 100f);
        return FastMath.approximateEquals(roundedCurrentAngle, 0.0f);
    }

    private boolean isDoorJointOpened(PartDoor door, DoorPhysics doorPhysics) {
        float extreme = doorPhysics.getJointExtremeLimit(door.getOpenLimit().x, door.getOpenLimit().y);
        float roundedExtreme = DynamXMath.roundFloat(extreme, 100f);
        float roundedCurrentAngle = DynamXMath.roundFloat(doorPhysics.getJointAngle(), 100f);
        return roundedExtreme >= 0 ? roundedCurrentAngle >= roundedExtreme : roundedCurrentAngle <= roundedExtreme;
    }

    public void playDoorSound(DoorState doorState) {
        if (vehicleEntity.world.isRemote) {
            ClientProxy.SOUND_HANDLER.playSingleSound(vehicleEntity.physicsPosition, doorState == DoorState.CLOSED ? "door_close" : "door_open", 1, 1);
        }
    }

    @Override
    public void onRemovedFromWorld() {
        if (DynamXMain.proxy.shouldUseBulletSimulation(vehicleEntity.world)) {
            for (DoorPhysics body : attachedDoors.values())
                DynamXContext.getPhysicsWorld(vehicleEntity.world).removeCollisionObject(body.doorBody);
            attachedDoors.clear();
        }
    }

    @Override
    public void setPhysicsTransform(byte jointId, RigidBodyTransform transform) {
        if (attachedDoors.containsKey(jointId)) {
            attachedBodiesTransform.get(jointId).getPhysicTransform().set(transform);
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

            ObjModelRenderer vehicleModel = DynamXContext.getObjModelRegistry().getModel(carEntity.getPackInfo().getModel());
            GlStateManager.scale(carEntity.getPackInfo().getScaleModifier().x, carEntity.getPackInfo().getScaleModifier().y, carEntity.getPackInfo().getScaleModifier().z);
            render.renderModelGroup(vehicleModel, door.getPartName(), carEntity, carEntity.getEntityTextureID());
            GlStateManager.scale(1 / carEntity.getPackInfo().getScaleModifier().x, 1 / carEntity.getPackInfo().getScaleModifier().y, 1 / carEntity.getPackInfo().getScaleModifier().z);

            GlStateManager.popMatrix();
        }
    }

    public PartDoor getPartDoor(byte doorID) {
        return vehicleEntity.getPackInfo().getPartByTypeAndId(PartDoor.class, doorID);
    }

    public DoorPhysics getDoorVarContainer(byte doorId) {
        return attachedDoors.get(doorId);
    }

    public DoorState getCurrentState(byte doorId) {
        return doorsState.get().get(doorId);
    }

    public DoorState getInverseCurrentState(byte doorId) {
        return isDoorOpened(doorId) ? DoorState.CLOSING : DoorState.OPENING;
    }

    @Override
    public Map<Byte, SynchronizedRigidBodyTransform> getTransforms() {
        return attachedBodiesTransform;
    }

    public enum DoorState {
        OPENED, CLOSED, OPENING, CLOSING
    }

    @Getter
    @Setter
    public static class DoorPhysics {
        private DoorsModule module;
        private byte doorID;
        private PhysicsRigidBody doorBody;
        private New6Dof joint;

        public float getJointAngle() {
            return joint.getAngles(null).get(module.getPartDoor(doorID).getAxisToUse() - 3);
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
