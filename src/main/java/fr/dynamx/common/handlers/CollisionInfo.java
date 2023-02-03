package fr.dynamx.common.handlers;

import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import fr.dynamx.client.handlers.ClientDebugSystem;
import fr.dynamx.utils.debug.renderer.VehicleDebugRenderer;
import fr.dynamx.utils.optimization.MutableBoundingBox;
import fr.dynamx.utils.optimization.Vector3fPool;
import lombok.Getter;
import net.minecraft.entity.Entity;
import net.minecraft.util.EnumFacing;

import java.util.ArrayList;
import java.util.List;

public class CollisionInfo {
    /**
     * The collision boxes composing this entity, with no rotation applied, but at the objet position <br>
     * Used for collisions with players and other entities <br>
     * The list is not modified by callers of the function
     */
    @Getter
    private final List<MutableBoundingBox> collisionBoxes;
    /**
     *  The position of the collision boxes
     */
    @Getter
    private final Vector3f position;
    /**
     * The rotation of the collidable object
     */
    @Getter
    private final Quaternion rotation;
    private Quaternion inversedRotation;

    public CollisionInfo(List<MutableBoundingBox> collisionBoxes, Vector3f position, Quaternion rotation) {
        this.collisionBoxes = collisionBoxes;
        this.position = position;
        this.rotation = rotation;
        this.inversedRotation = rotation.inverse();
        if (inversedRotation == null)
            inversedRotation = Quaternion.IDENTITY;
    }

    public void update(Vector3f position, Quaternion rotation) {
        this.position.set(position);
        this.rotation.set(rotation);
        this.inversedRotation = rotation.inverse(); //todo use poule
        if (inversedRotation == null)
            inversedRotation = Quaternion.IDENTITY;
    }

    public float collideY(RotatedCollisionHandlerImpl handler, Entity entity, MutableBoundingBox entityBox, float motionY) {
        int k = 0;
        for (int l = collisionBoxes.size(); k < l; ++k) {
            //TODO STORE COMPUTED BOXES
            MutableBoundingBox rotated = handler.rotateBB(position, new MutableBoundingBox(collisionBoxes.get(k)), rotation);
            rotated.grow(0f, Math.abs(motionY), 0f);
            if (rotated.intersects(entityBox)) {
                Vector3f motion = doRotatedCollision(handler, entity, Vector3fPool.get(0, motionY, 0));
                motionY = motion.y;
                break;
            }
        }
        return motionY;
    }

    public float collideX(RotatedCollisionHandlerImpl handler, Entity entity, MutableBoundingBox entityBox, float motionX) {
        int k = 0;
        for (int l = collisionBoxes.size(); k < l; ++k) {
            MutableBoundingBox rotated = handler.rotateBB(position, new MutableBoundingBox(collisionBoxes.get(k)), rotation);
            rotated.grow(Math.abs(motionX), 0f, 0f);
            if (rotated.intersects(entityBox)) {
                Vector3f motion = doRotatedCollision(handler, entity, Vector3fPool.get(motionX, 0, 0));
                motionX = motion.x;
                break;
            }
        }
        return motionX;
    }

    public float collideZ(RotatedCollisionHandlerImpl handler, Entity entity, MutableBoundingBox entityBox, float motionZ) {
        int k = 0;
        for (int l = collisionBoxes.size(); k < l; ++k) {
            MutableBoundingBox rotated = handler.rotateBB(position, new MutableBoundingBox(collisionBoxes.get(k)), rotation);
            rotated.grow(0f, 0f, Math.abs(motionZ));
            if (rotated.intersects(entityBox)) {
                Vector3f motion = doRotatedCollision(handler, entity, Vector3fPool.get(0, 0, motionZ));
                motionZ = motion.z;
                break;
            }
        }
        return motionZ;
    }

    private Vector3f doRotatedCollision(RotatedCollisionHandlerImpl handler, Entity entity, Vector3f motion) {
        if (entity.world.isRemote && ClientDebugSystem.enableDebugDrawing)
            VehicleDebugRenderer.PlayerCollisionsDebug.pos = entity.getPositionVector();
        float oldx = motion.x, oldy = motion.y, oldz = motion.z;

        if (entity.world.isRemote && ClientDebugSystem.enableDebugDrawing)
            VehicleDebugRenderer.PlayerCollisionsDebug.motion = Vector3fPool.getPermanentVector(motion);
        motion = handler.rotate(motion, inversedRotation);
        Vector3f origin = Vector3fPool.get(motion);

        List<EnumFacing> collisionFaces = new ArrayList<>();
        MutableBoundingBox tempBB = handler.rotateBB(position, Vector3fPool.get((float) entity.posX, (float) entity.posY, (float) entity.posZ), entity.getEntityBoundingBox(), inversedRotation);
        if (entity.world.isRemote && ClientDebugSystem.enableDebugDrawing) {
            VehicleDebugRenderer.PlayerCollisionsDebug.lastTemp = tempBB.toBB();
            VehicleDebugRenderer.PlayerCollisionsDebug.rotatedmotion = Vector3fPool.getPermanentVector(motion);
        }
        if (Math.abs(entity.getEntityBoundingBox().minY - tempBB.minY) < 0.05)
            tempBB.minY = entity.getEntityBoundingBox().minY + 0.01f;
        if (motion.y != 0) {
            int k = 0;
            for (int l = collisionBoxes.size(); k < l; ++k) {
                float ny = RotatedCollisionHandlerImpl.calculateYOffset(collisionBoxes.get(k), tempBB, motion.y);
                if (ny < motion.y) {
                    collisionFaces.add(EnumFacing.DOWN);
                    motion.y = ny;
                } else if (ny > motion.y) {
                    collisionFaces.add(EnumFacing.UP);
                    motion.y = ny;
                }
            }
            if (motion.y != 0)
                tempBB = tempBB.offset(0, motion.y, 0);
        }
        if (motion.x != 0) {
            int j5 = 0;
            for (int l5 = collisionBoxes.size(); j5 < l5; ++j5) {
                float nx = RotatedCollisionHandlerImpl.calculateXOffset(collisionBoxes.get(j5), tempBB, motion.x);
                if (nx < motion.x) {
                    collisionFaces.add(EnumFacing.WEST);
                    motion.x = nx;
                } else if (nx > motion.x) {
                    collisionFaces.add(EnumFacing.EAST);
                    motion.x = nx;
                }
            }
            if (motion.x != 0)
                tempBB = tempBB.offset(motion.x, 0, 0);
        }
        if (motion.z != 0) {
            int k5 = 0;
            for (int i6 = collisionBoxes.size(); k5 < i6; ++k5) {
                float nz = RotatedCollisionHandlerImpl.calculateZOffset(collisionBoxes.get(k5), tempBB, motion.z);
                if (nz < motion.z) {
                    collisionFaces.add(EnumFacing.NORTH);
                    motion.z = nz;
                } else if (nz > motion.z) {
                    collisionFaces.add(EnumFacing.SOUTH);
                    motion.z = nz;
                }
            }
        }
        if (entity.world.isRemote && ClientDebugSystem.enableDebugDrawing)
            VehicleDebugRenderer.PlayerCollisionsDebug.realmotionrot = Vector3fPool.getPermanentVector(motion);

        if (!motion.equals(origin)) {
            motion = handler.rotate(motion, rotation);
            float eps = 0.1f;
            if (Math.abs(motion.x - oldx) < eps / 5)
                motion.x = oldx;
            if (Math.abs(motion.y - oldy) < eps / 5)
                motion.y = oldy;
            if (Math.abs(motion.z - oldz) < eps / 5)
                motion.z = oldz;
        } else
            motion = Vector3fPool.get(oldx, oldy, oldz);
        if (entity.world.isRemote && ClientDebugSystem.enableDebugDrawing)
            VehicleDebugRenderer.PlayerCollisionsDebug.realmotion = Vector3fPool.getPermanentVector(motion);
        return motion;
    }
}
