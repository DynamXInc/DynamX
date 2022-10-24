package fr.dynamx.common.physics.entities;

import com.jme3.bullet.collision.Activation;
import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import fr.dynamx.common.DynamXMain;
import fr.dynamx.common.entities.PhysicsEntity;
import fr.dynamx.utils.maths.DynamXMath;
import fr.dynamx.utils.optimization.Vector3fPool;
import fr.dynamx.utils.physics.DynamXPhysicsHelper;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

/**
 * Physics handler of {@link PhysicsEntity} using rigid bodies <br>
 * The physics handler is the bridge between the minecraft entity and the physics engine
 *
 * @param <T> The entity type
 */
public abstract class EntityPhysicsHandler<T extends PhysicsEntity<?>> extends AbstractEntityPhysicsHandler<T, PhysicsRigidBody> {
    private final Vector3f linearVel = new Vector3f();
    private final Vector3f rotationalVel = new Vector3f();
    private boolean appliedBuoy;

    public EntityPhysicsHandler(T entity) {
        super(entity);
        collisionObject.setPhysicsRotation(entity.physicsRotation);
    }

    @Override
    public void update() {
        super.update();
        /* todo more tests to correctly fix this
        if(collisionObject.getActivationState() == Activation.error){
            handledEntity.setDead();
            DynamXMain.log.error("Fatal error on " + handledEntity.getName());
            return;
        }*/
        getCollisionObject().getLinearVelocity(linearVel);
        getCollisionObject().getAngularVelocity(rotationalVel);

        //Buoyancy effect W.I.P
        if (collisionObject.isInWorld()) {
            Vector3f bodyPos = getPosition();
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
        }
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
        this.physicsPosition.set(position);
        collisionObject.setPhysicsLocation(physicsPosition);
    }

    @Override
    public void setPhysicsRotation(Quaternion rotation) {
        this.physicsRotation.set(rotation);
        collisionObject.setPhysicsRotation(physicsRotation);
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
        //TODO helicopter if(getLinearVelocity().length() > 0.12f)
        getCollisionObject().applyImpulse(force, at);
    }

    @Override
    public void applyTorque(Vector3f force) {
        //TODO helicopter if(getLinearVelocity().length() > 0.12f)
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
}