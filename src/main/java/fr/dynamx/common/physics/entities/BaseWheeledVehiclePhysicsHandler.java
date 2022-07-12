package fr.dynamx.common.physics.entities;

import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.bullet.objects.PhysicsVehicle;
import com.jme3.math.Quaternion;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import fr.dynamx.api.physics.BulletShapeType;
import fr.dynamx.api.physics.EnumBulletShapeType;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.contentpack.type.vehicle.FrictionPoint;
import fr.dynamx.common.entities.BaseVehicleEntity;
import fr.dynamx.utils.optimization.QuaternionPool;
import fr.dynamx.utils.optimization.Vector3fPool;

/**
 * Physics handler of {@link BaseVehicleEntity}, for vehicles that have wheels (using the bullet's {@link PhysicsVehicle}) <br>
 *     The physics handler is the bridge between the minecraft entity and the physics engine
 * @param <T> The entity type
 */
public abstract class BaseWheeledVehiclePhysicsHandler<T extends BaseVehicleEntity<?>> extends BaseVehiclePhysicsHandler<T>
{
    private PhysicsVehicle physicsVehicle;

    public BaseWheeledVehiclePhysicsHandler(T entity) {
        super(entity);
    }

    @Override
    public PhysicsRigidBody createShape(Vector3f position, Quaternion rotation, float spawnRotation) {
        Vector3f trans = Vector3fPool.get(position);
        Transform localTransform = new Transform(trans, QuaternionPool.get(rotation));

        //Don't use this.getPackInfo() : it isn't initialized yet
        physicsVehicle = new PhysicsVehicle(getHandledEntity().getPackInfo().getPhysicsCollisionShape(), getHandledEntity().getPackInfo().getEmptyMass());
        physicsVehicle.setPhysicsTransform(localTransform);
        physicsVehicle.setUserObject(new BulletShapeType<>(EnumBulletShapeType.VEHICLE, getHandledEntity()));
        physicsVehicle.setSleepingThresholds(0.3f, 1);
        //physicsVehicle.setRollingFriction(10);
        return physicsVehicle;
    }

    @Override
    public void addToWorld() {
        if(DynamXContext.getPhysicsWorld() == null)  {
            throw new NullPointerException("Physics world is null, wtf "+handledEntity.getEntityWorld()+" "+ getCollisionObject());
        }
        DynamXContext.getPhysicsWorld().addVehicle((PhysicsVehicle) getCollisionObject());
    }

    @Override
    public void update() {
        super.update();
        if(!handledEntity.getPackInfo().getFrictionPoints().isEmpty() && isBodyActive()) {
            float horizSpeed = Vector3fPool.get(getLinearVelocity().x, 0, getLinearVelocity().z).length();
            for(FrictionPoint f : handledEntity.getPackInfo().getFrictionPoints())
            {
                Vector3f pushDown = new Vector3f(-getLinearVelocity().x, -horizSpeed, -getLinearVelocity().z);
                pushDown.multLocal(f.getIntensity());
                applyForce(f.getPosition(), pushDown);
            }
        }

        //Quaternion q = getRotation();
        /*float roll  = (float) atan2(2.0 * (q.getZ() * q.getY() + q.getW() * q.getX()) , 1.0 - 2.0 * (q.getX() * q.getX() + q.getY() * q.getY()));
        float pitch = (float) asin(2.0 * (q.getY() * q.getW() - q.getZ() * q.getX()));
        float yaw   = (float) atan2(2.0 * (q.getZ() * q.getW() + q.getX() * q.getY()) , - 1.0 + 2.0 * (q.getW() * q.getW() + q.getX() * q.getX()));
        System.out.println("RPY is "+roll+" "+pitch+" "+yaw);*/
        /*Vector3f proj = Vector3fPool.get(1, 0, 0);
        proj = Trigonometry.rotateVectorByQuaternion(proj, getRotation());
        float y = proj.y;
        double roll = y * PI/2;
        System.out.println("Get Y "+y+" and roll "+roll);
        float pas = 0.08f;
        if(roll > 0.05)
        {
            System.out.println("Apply +");
            //if(y < 1.2)
              //  y = 0.8f;
            getPhysicsVehicle().applyTorque(Vector3fPool.get(1000*y, 0, 0));
        }
        else if(roll < -0.05)
        {
            System.out.println("Apply -");
            //if(y > -1.2)
              //  y = -0.8f;
            getPhysicsVehicle().applyTorque(Vector3fPool.get(1000*y, 0, 0));
        }*/
    }

    public float getSpeed(SpeedUnit speedUnit) {
        switch (speedUnit) {
            case KMH:
                return this.physicsVehicle.getCurrentVehicleSpeedKmHour();
            case MPH:
                return this.physicsVehicle.getCurrentVehicleSpeedKmHour() * KMH_TO_MPH;
            default:
                return -1;
        }
    }

    public PhysicsVehicle getPhysicsVehicle() {
        return physicsVehicle;
    }

    @Override
    public void removePhysicsEntity() {
        if (physicsVehicle != null) {
            DynamXContext.getPhysicsWorld().removeVehicle(physicsVehicle);
        }
    }
}
