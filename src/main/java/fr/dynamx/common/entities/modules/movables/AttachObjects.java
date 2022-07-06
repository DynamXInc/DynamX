package fr.dynamx.common.entities.modules.movables;

import com.jme3.bullet.RotationOrder;
import com.jme3.bullet.joints.JointEnd;
import com.jme3.bullet.joints.New6Dof;
import com.jme3.bullet.joints.motors.MotorParam;
import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import fr.dynamx.common.entities.PhysicsEntity;
import fr.dynamx.common.entities.modules.MovableModule;
import fr.dynamx.common.physics.joints.EntityJoint;
import fr.dynamx.utils.physics.DynamXPhysicsHelper;

import static fr.dynamx.utils.physics.DynamXPhysicsHelper.*;

public class AttachObjects extends MovableModule {

    private Vector3f firstAttachPoint, secondAttachPoint;
    private Vector3f firstAttachPointInWorld, secondAttachPointInWorld;
    private PhysicsRigidBody firstRb, secondRb;
    private New6Dof joint;

    public AttachObjects(PhysicsEntity<?> entity) {
        super(entity);
    }

    public New6Dof createJointBetween2Objects(byte jointID) {
        if (jointID == 1) {
            if (isEndInitialized(JointEnd.A) && isEndInitialized(JointEnd.B)) {

                Vector3f dir = secondAttachPoint.subtract(firstAttachPoint);

                Vector3f middleVec = firstAttachPoint.add(dir.multLocal(5f));
                Vector3f middleVec2 = secondAttachPoint.subtract(dir);

                joint = new New6Dof(firstRb, secondRb, middleVec, middleVec2, Quaternion.IDENTITY.toRotationMatrix(),
                        Quaternion.IDENTITY.toRotationMatrix(), RotationOrder.XYZ);

            } else if (isEndInitialized(JointEnd.A)) {
                joint = new New6Dof(firstRb, firstAttachPoint, firstAttachPointInWorld, Quaternion.IDENTITY.toRotationMatrix(),
                        Quaternion.IDENTITY.toRotationMatrix(), RotationOrder.XYZ);
            }
        } else if (jointID == 2) {
            if (isEndInitialized(JointEnd.A) && isEndInitialized(JointEnd.B)) {
                if (!firstRb.equals(secondRb)) {
                    joint = new New6Dof(firstRb, secondRb, firstAttachPoint, secondAttachPoint, Quaternion.IDENTITY.toRotationMatrix(),
                            Quaternion.IDENTITY.toRotationMatrix(), RotationOrder.XYZ);
                    joint.set(MotorParam.LowerLimit, X_ROTATION_DOF, 0f);
                    joint.set(MotorParam.LowerLimit, Z_ROTATION_DOF, 0f);
                    joint.set(MotorParam.LowerLimit, Y_ROTATION_DOF, 0f);
                    joint.set(MotorParam.UpperLimit, Y_ROTATION_DOF, 0f);
                    joint.set(MotorParam.UpperLimit, X_ROTATION_DOF, 0f);
                    joint.set(MotorParam.UpperLimit, Z_ROTATION_DOF, 0f);
                }
            }
        }

        return joint;
    }

    public void initObject(PhysicsRigidBody rigidBody, Vector3f attachPoint, JointEnd end) {
        Vector3f localPickPos = DynamXPhysicsHelper.getBodyLocalPoint(rigidBody, attachPoint);
        if (end == JointEnd.A) {
            firstRb = rigidBody;
            firstAttachPoint = localPickPos;
            firstAttachPointInWorld = attachPoint;
        } else {
            secondRb = rigidBody;
            secondAttachPoint = localPickPos;
            secondAttachPointInWorld = attachPoint;
        }
    }

    public boolean isEndInitialized(JointEnd end) {
        return end == JointEnd.A ? (firstRb != null && firstAttachPoint != null) : (secondRb != null && secondAttachPoint != null);
    }

    @Override
    public boolean canCreateJoint(PhysicsEntity<?> withEntity, byte jointId) {
        return (jointId == 1 || jointId == 2) && joint == null;
    }

    @Override
    public void onJointDestroyed(EntityJoint<?> joint) {
        this.joint = null;
        firstRb = null;
        firstAttachPoint = null;
        firstAttachPointInWorld = null;
        secondRb = null;
        secondAttachPoint = null;
        secondAttachPointInWorld = null;
    }

    public Vector3f getFirstAttachPoint() {
        return firstAttachPoint;
    }

    public Vector3f getSecondAttachPoint() {
        return secondAttachPoint;
    }

    public PhysicsRigidBody getFirstRb() {
        return firstRb;
    }

    public PhysicsRigidBody getSecondRb() {
        return secondRb;
    }

}
