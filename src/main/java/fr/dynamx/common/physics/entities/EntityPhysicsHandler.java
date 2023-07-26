package fr.dynamx.common.physics.entities;

import com.jme3.bullet.collision.Activation;
import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import fr.dynamx.common.DynamXMain;
import fr.dynamx.common.entities.PhysicsEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;

/**
 * Physics handler of {@link PhysicsEntity} using rigid bodies <br>
 * The physics handler is the bridge between the minecraft entity and the physics engine
 *
 * @param <T> The entity type
 */
public abstract class EntityPhysicsHandler<T extends PhysicsEntity<?>> extends AbstractEntityPhysicsHandler<T, PhysicsRigidBody> {
    private final Vector3f linearVel = new Vector3f();
    private final Vector3f rotationalVel = new Vector3f();

    public EntityPhysicsHandler(T entity) {
        super(entity);
    }

    @Override
    public void update() {
        super.update();
        if(collisionObject.getActivationState() == Activation.error){
            DynamXMain.log.error("Fatal error on " + handledEntity.getName());
            return;
        }
        getCollisionObject().getLinearVelocity(linearVel);
        getCollisionObject().getAngularVelocity(rotationalVel);

        //Buoyancy effect W.I.P
        /*if (collisionObject.isInWorld()) {
            Vector3f bodyPos = handledEntity.physicsPosition;
            BlockPos pos = new BlockPos(bodyPos.x, bodyPos.y, bodyPos.z);
            IBlockState blockState = handledEntity.world.getBlockState(pos);
            if (blockState.getBlock() instanceof BlockLiquid) { //TODO IMPROVE
                float liquidHeight = BlockLiquid.getBlockLiquidHeight(blockState, handledEntity.world, pos);
                if (liquidHeight > bodyPos.y % 1.0) {
                    appliedBuoy = true;
                    float normalizedMass = DynamXMath.normalizeBetween(collisionObject.getMass(), 0, 1500, 1, 2);
                    collisionObject.setGravity(Vector3fPool.get(0.0f, 2 - normalizedMass, 0.0f));
                    Vector3f waterVelocity = Vector3fPool.get(blockState.getBlock().modifyAcceleration(handledEntity.world, pos, null, new Vec3d(0, 0, 0)));
                    Vector3f bodyVelocity = linearVel;
                    Vector3f angularVelocity = rotationalVel;
                    float damping = 0.95f;
                    float flowStrength = 0.1f;
                    collisionObject.setLinearVelocity(Vector3fPool.get(
                            bodyVelocity.x * damping + waterVelocity.x * flowStrength,
                            bodyVelocity.y * damping + waterVelocity.y * flowStrength,
                            bodyVelocity.z * damping + waterVelocity.z * flowStrength));
                    collisionObject.setAngularVelocity(Vector3fPool.get(angularVelocity.x * damping, angularVelocity.y * damping, angularVelocity.z * damping));
                } else {
                    if (appliedBuoy) {
                        collisionObject.setGravity(DynamXPhysicsHelper.GRAVITY);
                        appliedBuoy = false;
                    }
                }
            } else {
                if (appliedBuoy) {
                    collisionObject.setGravity(DynamXPhysicsHelper.GRAVITY);
                    appliedBuoy = false;
                }
            }
        }*/
    }

    @Override
    public Vector3f getLinearVelocity() {
        return linearVel;
    }

    @Override
    public Vector3f getAngularVelocity() {
        return rotationalVel;
    }

    @Override
    public void setPhysicsPosition(Vector3f position) {
        handledEntity.physicsPosition.set(position);
        collisionObject.setPhysicsLocation(position);
    }

    @Override
    public void setPhysicsRotation(Quaternion rotation) {
        handledEntity.physicsRotation.set(rotation);
        collisionObject.setPhysicsRotation(rotation);
    }

    @Override
    public void setLinearVelocity(Vector3f velocity) {
        this.linearVel.set(velocity);
        collisionObject.setLinearVelocity(velocity);
    }

    @Override
    public void setAngularVelocity(Vector3f velocity) {
        this.rotationalVel.set(velocity);
        collisionObject.setAngularVelocity(velocity);
    }

    @Override
    public void applyForce(Vector3f at, Vector3f force) {
        //if(getLinearVelocity().length() > 0.005f)
            getCollisionObject().applyImpulse(force, at);
    }

    @Override
    public void applyTorque(Vector3f force) {
        //if(getLinearVelocity().length() > 0.005f)
            getCollisionObject().applyTorque(force);
    }

    @Override
    public void applyImpulse(Vector3f at, Vector3f force) {
        getCollisionObject().applyImpulse(force, at);
    }

    @Override
    public void applyTorqueImpulse(Vector3f force) {
        getCollisionObject().applyTorqueImpulse(force);
    }

    @Override
    public void setFreezePhysics(boolean freeze) {
        getCollisionObject().setKinematic(freeze);
    }

    /**
     * @return The water level at the entity's position, or {@link Float#MIN_VALUE} if the entity is not in water
     */
    public float getWaterLevel() {
        PhysicsEntity<?> entity = getHandledEntity();
        // search water downwards, two blocks from the entity
        for (int offset = 2; offset > -2; offset--) {
            BlockPos blockPos = new BlockPos(entity.physicsPosition.x, entity.physicsPosition.y + offset, entity.physicsPosition.z);
            if (entity.getEntityWorld().getBlockState(blockPos).getMaterial().isLiquid()) {
                AxisAlignedBB boundingBox = entity.getEntityWorld().getBlockState(blockPos).getBoundingBox(entity.getEntityWorld(), blockPos);
                return (float) boundingBox.offset(blockPos).maxY - 0.125F + 0.5f;
            }
        }
        return Float.MIN_VALUE;
    }
}