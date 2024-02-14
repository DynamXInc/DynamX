package fr.dynamx.common.physics.entities;

import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.math.Quaternion;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import fr.dynamx.api.physics.BulletShapeType;
import fr.dynamx.api.physics.EnumBulletShapeType;
import fr.dynamx.common.DynamXMain;
import fr.dynamx.common.contentpack.type.objects.PropObject;
import fr.dynamx.common.entities.PropsEntity;
import fr.dynamx.utils.maths.DynamXGeometry;
import fr.dynamx.utils.optimization.QuaternionPool;
import jme3utilities.math.MyQuaternion;

public class PropPhysicsHandler<T extends PropsEntity<?>> extends PackEntityPhysicsHandler<PropObject<?>, T> {

    public PropPhysicsHandler(T entity) {
        super(entity);
    }

    @Override
    public PhysicsRigidBody createShape(Vector3f position, Quaternion rotation, float spawnRotation) {
        if (MyQuaternion.isZero(rotation)) {
            DynamXMain.log.warn("Resetting physics rotation of entity " + handledEntity);
            rotation = DynamXGeometry.rotationYawToQuaternion(spawnRotation);
        }
        Transform transform = new Transform(position, QuaternionPool.get(rotation));

        //Don't use this.getPackInfo() : it isn't initialized yet
        PropObject<?> packInfo = getHandledEntity().getPackInfo();
        PhysicsRigidBody rigidBody = new PhysicsRigidBody(packInfo.getCollisionsHelper().getPhysicsCollisionShape(), packInfo.getEmptyMass());
        rigidBody.setPhysicsTransform(transform);
        rigidBody.setUserObject(new BulletShapeType<>(EnumBulletShapeType.BULLET_ENTITY, getHandledEntity()));
        rigidBody.setFriction(packInfo.getFriction());
        rigidBody.setSleepingThresholds(0.2f, 1);
        rigidBody.setDamping(packInfo.getLinearDamping(), packInfo.getAngularDamping());
        rigidBody.setRestitution(packInfo.getRestitutionFactor());
        if (packInfo.isCCDEnabled()) {
            rigidBody.setCcdMotionThreshold(0.1f);
            rigidBody.setCcdSweptSphereRadius(0.1f);
        }
        return rigidBody;
    }

    @Override
    public void update() {
        super.update();
    }
}
