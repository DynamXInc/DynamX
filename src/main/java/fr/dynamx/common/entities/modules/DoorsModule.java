package fr.dynamx.common.entities.modules;

import com.jme3.bullet.RotationOrder;
import com.jme3.bullet.collision.shapes.BoxCollisionShape;
import com.jme3.bullet.joints.Constraint;
import com.jme3.bullet.joints.New6Dof;
import com.jme3.bullet.joints.motors.MotorParam;
import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import fr.dynamx.api.entities.modules.AttachModule;
import fr.dynamx.api.entities.modules.IPhysicsModule;
import fr.dynamx.api.network.sync.SimulationHolder;
import fr.dynamx.api.physics.BulletShapeType;
import fr.dynamx.api.physics.EnumBulletShapeType;
import fr.dynamx.client.ClientProxy;
import fr.dynamx.client.renders.RenderPhysicsEntity;
import fr.dynamx.client.renders.model.ObjModelRenderer;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.DynamXMain;
import fr.dynamx.common.contentpack.parts.PartDoor;
import fr.dynamx.common.entities.BaseVehicleEntity;
import fr.dynamx.common.entities.PhysicsEntity;
import fr.dynamx.common.handlers.TaskScheduler;
import fr.dynamx.common.network.packets.MessageChangeDoorState;
import fr.dynamx.common.network.sync.vars.AttachedBodySynchronizedVariable;
import fr.dynamx.common.network.sync.vars.AttachedDoorsSynchronizedVariable;
import fr.dynamx.common.network.sync.vars.VehicleSynchronizedVariables;
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
    protected Map<Byte, DoorState> doorsState = new HashMap<>();

    public DoorsModule(BaseVehicleEntity<?> vehicleEntity) {
        this.vehicleEntity = vehicleEntity;
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
        PartDoor door1 = getPartDoor(jointId);
        Vector3f p1 = Vector3fPool.get(door1.getCarAttachPoint());
        Vector3f p2 = Vector3fPool.get(door1.getDoorAttachPoint());

        DoorVarContainer localVarContainer = new DoorVarContainer();
        localVarContainer.setModule(this);
        localVarContainer.setDoorID(jointId);

        Quaternion doorRotation = QuaternionPool.get(vehicleEntity.getPhysicsHandler().getRotation());
        Vector3f doorPos = DynamXGeometry.rotateVectorByQuaternion(p1.subtract(p2).addLocal(vehicleEntity.getPackInfo().getCenterOfMass()), doorRotation).addLocal(vehicleEntity.physicsPosition);
        PhysicsRigidBody doorBody = DynamXPhysicsHelper.fastCreateRigidBody(vehicleEntity, 40, new BoxCollisionShape(door1.getScale()), doorPos, vehicleEntity.getPhysicsHandler().getSpawnRotationAngle());
        localVarContainer.setDoorBody(doorBody);
        doorBody.setUserObject(new BulletShapeType<>(EnumBulletShapeType.BULLET_ENTITY, localVarContainer));
        DynamXContext.getPhysicsWorld().addCollisionObject(doorBody);
        attachedDoors.forEach((doorId, doorVarContainer) -> {
            doorBody.addToIgnoreList(doorVarContainer.doorBody);
            doorVarContainer.doorBody.addToIgnoreList(doorBody);
        });

        New6Dof new6Dof = new New6Dof(vehicleEntity.physicsHandler.getCollisionObject()
                , doorBody, p1.addLocal(vehicleEntity.getPackInfo().getCenterOfMass()), p2, Quaternion.IDENTITY.toRotationMatrix(), Quaternion.IDENTITY.toRotationMatrix(),
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
            doorsState.put(partDoor.getId(), DoorState.CLOSE);
        }
    }

    @Override
    public void addSynchronizedVariables(Side side, SimulationHolder simulationHolder, List<ResourceLocation> variables) {
        if (simulationHolder.isPhysicsAuthority(side)) {
            variables.add(AttachedDoorsSynchronizedVariable.NAME);
        }
        if (side.isServer()) {
            variables.add(VehicleSynchronizedVariables.DoorsStatus.NAME);
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
        if (simulatingPhysics) {
            for (byte doorID : attachedDoors.keySet()) {
                DoorVarContainer varContainer = attachedDoors.get(doorID);
                PartDoor door = getPartDoor(doorID);
                if (getCurrentState(doorID) == DoorState.OPENING && isDoorJointOpened(door, varContainer)) {
                    DynamXContext.getNetwork().sendToServer(new MessageChangeDoorState(vehicleEntity, DoorState.OPEN, doorID));
                    doorsState.put(doorID, DoorState.OPEN);
                    varContainer.setJointMotorState(DynamXPhysicsHelper.Y_ROTATION_DOF, false);
                }
                if ((isDoorOpened(doorID) || getCurrentState(doorID) == DoorState.CLOSING) && isDoorJointClosed(varContainer)) {
                    DynamXContext.getNetwork().sendToServer(new MessageChangeDoorState(vehicleEntity, DoorState.CLOSE, doorID));
                    doorsState.put(doorID, DoorState.CLOSE);
                    playDoorSound(DoorState.CLOSE);
                    varContainer.setJointMotorState(DynamXPhysicsHelper.Y_ROTATION_DOF, false);
                    varContainer.setJointLimit(DynamXPhysicsHelper.Y_ROTATION_DOF, door.getCloseLimit().x, door.getCloseLimit().y);
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
        return !doorsState.containsKey(doorID) || doorsState.get(doorID) == DoorState.OPEN;
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
                doorsState.put(doorId, DoorState.OPENING);
                playDoorSound(DoorState.OPEN);
                doorVarContainer.setJointLimit(DynamXPhysicsHelper.Y_ROTATION_DOF,
                        door.getOpenLimit().x, door.getOpenLimit().y);
                doorVarContainer.setJointRotationMotorVelocity(DynamXPhysicsHelper.Y_ROTATION_DOF,
                        door.getOpenMotor().x, door.getOpenMotor().y);
            } else {
                doorVarContainer.setJointRotationMotorVelocity(DynamXPhysicsHelper.Y_ROTATION_DOF,
                        door.getCloseMotor().x, door.getCloseMotor().y);
                // if (vehicleEntity.getNetwork().getSimulationHolder() != SimulationHolder.SERVER_SP) //TODO SERVER_SP SPLITTED IN SERVER_SP AND DRIVE_SP
                doorsState.put(doorId, DoorState.CLOSING); //Closes the door (animation) and the plays the closing sound

            }
        } else {
            if (doorState == DoorState.OPEN) {
                doorsState.put(doorId, DoorState.OPEN);
                playDoorSound(DoorState.OPEN);
            } else {
                //if (entity.getNetwork().getSimulationHolder() != SimulationHolder.SERVER_SP)
                doorsState.put(doorId, DoorState.CLOSE); //Closes the door (animation) and the plays the closing sound
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
    }


    private boolean isDoorJointClosed(DoorVarContainer doorVarContainer) {
        float roundedCurrentAngle = DynamXMath.roundFloat(doorVarContainer.getJointAngle().y, 100f);
        return FastMath.approximateEquals(roundedCurrentAngle, 0.0f);
    }

    private boolean isDoorJointOpened(PartDoor door, DoorVarContainer doorVarContainer) {
        float extreme = doorVarContainer.getJointExtremeLimit(door.getOpenLimit().x, door.getOpenLimit().y);
        float roundedExtreme = DynamXMath.roundFloat(extreme, 100f);
        float roundedCurrentAngle = DynamXMath.roundFloat(doorVarContainer.getJointAngle().y, 100f);
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
        DoorsModule doorsModule = this;
        List<PartDoor> doors = carEntity.getPackInfo().getPartsByType(PartDoor.class);
        for (byte id = 0; id < doors.size(); id++) {
            PartDoor door = doors.get(id);
            if (!door.isEnabled()) {
                GlStateManager.pushMatrix();
                Vector3f pos = Vector3fPool.get().addLocal(door.getCarAttachPoint());
                pos.subtract(door.getDoorAttachPoint(), pos);

                /*GlStateManager.rotate(ClientDynamXUtils.computeInterpolatedGlQuaternion(carEntity.prevRenderRotation, carEntity.renderRotation, partialTicks, true));
                GlStateManager.translate(
                        (float) -(carEntity.prevPosX + (carEntity.posX - carEntity.prevPosX) * partialTicks),
                        (float) -(carEntity.prevPosY + (carEntity.posY - carEntity.prevPosY) * partialTicks),
                        (float) -(carEntity.prevPosZ + (carEntity.posZ - carEntity.prevPosZ) * partialTicks));*/
                GlStateManager.translate(pos.x, pos.y, pos.z);

                ObjModelRenderer vehicleModel = DynamXContext.getObjModelRegistry().getModel(carEntity.getPackInfo().getModel());
                GlStateManager.scale(carEntity.getPackInfo().getScaleModifier().x, carEntity.getPackInfo().getScaleModifier().y, carEntity.getPackInfo().getScaleModifier().z);
                render.renderModelGroup(vehicleModel, door.getPartName(), carEntity, carEntity.getEntityTextureID());
                GlStateManager.scale(1 / carEntity.getPackInfo().getScaleModifier().x, 1 / carEntity.getPackInfo().getScaleModifier().y, 1 / carEntity.getPackInfo().getScaleModifier().z);

                GlStateManager.popMatrix();
            } else if (doorsModule.getTransforms().containsKey(id)) {
                SynchronizedRigidBodyTransform sync = doorsModule.getTransforms().get(id);
                RigidBodyTransform transform = sync.getTransform();
                RigidBodyTransform prev = sync.getPrevTransform();

                GlStateManager.pushMatrix();
                Vector3f pos = Vector3fPool.get(prev.getPosition()).addLocal(transform.getPosition().subtract(prev.getPosition(), Vector3fPool.get()).multLocal(partialTicks));

                GlStateManager.rotate(ClientDynamXUtils.computeInterpolatedGlQuaternion(carEntity.prevRenderRotation, carEntity.renderRotation, partialTicks, true));
                GlStateManager.translate(
                        (float) -(carEntity.prevPosX + (carEntity.posX - carEntity.prevPosX) * partialTicks),
                        (float) -(carEntity.prevPosY + (carEntity.posY - carEntity.prevPosY) * partialTicks),
                        (float) -(carEntity.prevPosZ + (carEntity.posZ - carEntity.prevPosZ) * partialTicks));
                GlStateManager.translate(pos.x, pos.y, pos.z);
                GlStateManager.rotate(ClientDynamXUtils.computeInterpolatedGlQuaternion(prev.getRotation(), transform.getRotation(), partialTicks));

                ObjModelRenderer vehicleModel = DynamXContext.getObjModelRegistry().getModel(carEntity.getPackInfo().getModel());
                GlStateManager.scale(carEntity.getPackInfo().getScaleModifier().x, carEntity.getPackInfo().getScaleModifier().y, carEntity.getPackInfo().getScaleModifier().z);
                render.renderModelGroup(vehicleModel, door.getPartName(), carEntity, carEntity.getEntityTextureID());
                GlStateManager.scale(1 / carEntity.getPackInfo().getScaleModifier().x, 1 / carEntity.getPackInfo().getScaleModifier().y, 1 / carEntity.getPackInfo().getScaleModifier().z);

                GlStateManager.popMatrix();
            }
        }
    }

    public PartDoor getPartDoor(byte doorID) {
        return vehicleEntity.getPackInfo().getPartByTypeAndId(PartDoor.class, doorID);
    }

    public DoorVarContainer getDoorVarContainer(byte doorId) {
        return attachedDoors.get(doorId);
    }

    public DoorState getCurrentState(byte doorId) {
        return doorsState.get(doorId);
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

        public Vector3f getJointAngle() {
            return joint.getAngles(null);
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
            if (axis <= 3) {
                joint.getTranslationMotor().setMotorEnabled(axis, state);
            } else {
                joint.getRotationMotor(axis - 3).setMotorEnabled(state);
            }
        }
    }
}
