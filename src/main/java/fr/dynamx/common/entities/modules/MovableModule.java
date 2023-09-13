package fr.dynamx.common.entities.modules;

import com.jme3.bullet.joints.Constraint;
import fr.dynamx.api.entities.modules.AttachModule;
import fr.dynamx.api.entities.modules.IPhysicsModule;
import fr.dynamx.api.entities.modules.ModuleListBuilder;
import fr.dynamx.common.entities.PhysicsEntity;
import fr.dynamx.common.entities.modules.movables.AttachObjects;
import fr.dynamx.common.entities.modules.movables.MoveObjects;
import fr.dynamx.common.entities.modules.movables.PickObjects;
import fr.dynamx.common.physics.entities.AbstractEntityPhysicsHandler;
import fr.dynamx.common.physics.joints.EntityJoint;
import fr.dynamx.common.physics.joints.JointHandler;
import fr.dynamx.common.physics.joints.JointHandlerRegistry;
import fr.dynamx.utils.DynamXConstants;
import lombok.Getter;
import net.minecraft.util.ResourceLocation;

import java.util.Arrays;

public class MovableModule implements IPhysicsModule<AbstractEntityPhysicsHandler<?, ?>>, AttachModule.AttachToSelfModule, IPhysicsModule.IEntityUpdateListener, IPhysicsModule.IPhysicsUpdateListener {
    public static final ResourceLocation JOINT_NAME = new ResourceLocation(DynamXConstants.ID, "movable_module");

    static {
        JointHandlerRegistry.register(new JointHandler(JOINT_NAME, PhysicsEntity.class, PhysicsEntity.class, MovableModule.class));
    }

    public final PhysicsEntity<?> entity;
    public PickObjects pickObjects;
    public AttachObjects attachObjects;
    public MoveObjects moveObjects;
    public EnumAction usingAction;

    public MovableModule(PhysicsEntity<?> entity) {
        this.entity = entity;
    }

    /* Should not be called in the constructor */
    public void initSubModules(ModuleListBuilder modules, PhysicsEntity<?> entity) {
        //System.out.println("init modules ! " + this);
        modules.add(moveObjects = new MoveObjects(entity));
        modules.add(pickObjects = new PickObjects(entity));
        modules.add(attachObjects = new AttachObjects(entity));
    }

    @Override
    public Constraint createJoint(byte jointId) {
        if (jointId == 0) {
            return pickObjects.createWeldJoint();
        } else if (jointId == 1 || jointId == 2) {
            return attachObjects.createJointBetween2Objects(jointId);
        }
        return null;
    }

    @Override
    public boolean canCreateJoint(PhysicsEntity<?> withEntity, byte jointId) {
        if (jointId == 0) {
            return pickObjects.canCreateJoint(withEntity, jointId);
        } else if (jointId == 1 || jointId == 2) {
            return attachObjects.canCreateJoint(withEntity, jointId);
        }
        return false;
    }


    @Override
    public void onJointDestroyed(EntityJoint<?> joint) {
        if (joint.getJointId() == 0) {
            pickObjects.onJointDestroyed(joint);
        } else if (joint.getJointId() == 1 || joint.getJointId() == 2) {
            attachObjects.onJointDestroyed(joint);
        }
    }

    public enum EnumAction {
        TAKE, UNTAKE, PICK, UNPICK, LENGTH_CHANGE, FREEZE_OBJECT, ATTACH_OBJECTS, THROW
    }

    public static class Action {
        @Getter
        private Object[] info;
        private EnumAction enumAction;

        public Action() {
        }

        public Action(EnumAction action, Object... info) {
            this.enumAction = action;
            this.info = info;
        }

        public void setEnumAction(EnumAction enumAction) {
            this.enumAction = enumAction;
        }

        public void setInfo(Object[] info) {
            this.info = info;
        }

        public EnumAction getMovableAction() {
            return enumAction;
        }

        @Override
        public String toString() {
            return "Action{" +
                    "info=" + Arrays.toString(info) +
                    ", enumAction=" + enumAction +
                    '}';
        }
    }
}

