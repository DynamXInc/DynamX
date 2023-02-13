package fr.dynamx.common.handlers;

import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import fr.dynamx.client.handlers.ClientDebugSystem;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.utils.debug.renderer.VehicleDebugRenderer;
import fr.dynamx.utils.maths.DynamXGeometry;
import fr.dynamx.utils.maths.DynamXMath;
import fr.dynamx.utils.optimization.MutableBoundingBox;
import fr.dynamx.utils.optimization.Vector3fPool;
import lombok.Getter;
import net.minecraft.entity.Entity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class CollisionInfo {
    /**
     * The collision boxes composing this entity, with no rotation applied, but at the objet position <br>
     * Used for collisions with players and other entities <br>
     * The list is not modified by callers of the function
     */
    private final List<AxisAlignedBB> collisionBoxes;
    @Getter
    private List<AxisAlignedBB> rotatedBoxes;
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

    public CollisionInfo(List<AxisAlignedBB> collisionBoxes, Vector3f position, Quaternion rotation) {
        this.collisionBoxes = collisionBoxes;
        this.position = position;
        this.rotation = rotation;
        this.inversedRotation = rotation.inverse();
        if (inversedRotation == null)
            inversedRotation = Quaternion.IDENTITY;
        this.rotatedBoxes = this.collisionBoxes.stream().map(b -> rotateBB(b, rotation)).collect(Collectors.toList());
    }

    public void update(Vector3f position, Quaternion rotation) {
        this.position.set(position);
        this.rotation.set(rotation);
        this.inversedRotation = rotation.inverse(); //todo use poule
        if (inversedRotation == null)
            inversedRotation = Quaternion.IDENTITY;
        this.rotatedBoxes = this.collisionBoxes.stream().map(b -> rotateBB(b, rotation)).collect(Collectors.toList());
    }

    public AxisAlignedBB rotateBB(AxisAlignedBB from, Quaternion rotation) {
        Vector3f v1 = DynamXGeometry.rotateVectorByQuaternion(Vector3fPool.get((float) from.minX, 0, 0), rotation);
        Vector3f v2 = DynamXGeometry.rotateVectorByQuaternion(Vector3fPool.get(0, (float) from.minY, 0), rotation);
        Vector3f v3 = DynamXGeometry.rotateVectorByQuaternion(Vector3fPool.get(0, 0, (float) from.minZ), rotation);
        Vector3f v4 = DynamXGeometry.rotateVectorByQuaternion(Vector3fPool.get((float) from.maxX, 0, 0), rotation);
        Vector3f v5 = DynamXGeometry.rotateVectorByQuaternion(Vector3fPool.get(0, (float) from.maxY, 0), rotation);
        Vector3f v6 = DynamXGeometry.rotateVectorByQuaternion(Vector3fPool.get(0, 0, (float) from.maxZ), rotation);
        AxisAlignedBB n = new AxisAlignedBB(DynamXMath.getMin(v1.x, v2.x, v3.x, v4.x, v5.x, v6.x), DynamXMath.getMin(v1.y, v2.y, v3.y, v4.y, v5.y, v6.y), DynamXMath.getMin(v1.z, v2.z, v3.z, v4.z, v5.z, v6.z),
                DynamXMath.getMax(v1.x, v2.x, v3.x, v4.x, v5.x, v6.x), DynamXMath.getMax(v1.y, v2.y, v3.y, v4.y, v5.y, v6.y), DynamXMath.getMax(v1.z, v2.z, v3.z, v4.z, v5.z, v6.z));
        mult = 3;
        n = n.grow(mult);
        return n;
    }

    public static int mult = 5;

    public Vector3f collideAll(RotatedCollisionHandlerImpl handler, Entity entity, MutableBoundingBox entityBox, Vector3f motion) {
        int k = 0;
        entityBox.offset(Vector3fPool.get(position).multLocal(-1));
        System.out.println("==> INSET " + motion+ " " + entityBox);
        for (int l = collisionBoxes.size(); k < l; ++k) {
            if (entityBox.intersects(rotatedBoxes.get(k))) {
                motion = doRotatedCollision(handler, entity, entityBox, motion, 0);
                //System.out.println("InitMot " + motionX +" to " + motion.x + " eb " + entityBox +" rt " +rotated);
                break;
            }
        }
        entityBox.offset(position);
        return motion;
    }

    public float collideY(RotatedCollisionHandlerImpl handler, Entity entity, MutableBoundingBox entityBox, float motionY) {
        int k = 0;
        entityBox.offset(Vector3fPool.get(position).multLocal(-1));
        for (int l = collisionBoxes.size(); k < l; ++k) {
           // System.out.println("Y TEST VS " + entityBox+" "+rotatedBoxes.get(k));
            if (entityBox.intersects(rotatedBoxes.get(k))) {
                Vector3f motion = doRotatedCollision(handler, entity, entityBox, Vector3fPool.get(0, motionY, 0), 1);
                motionY = motion.y;
                break;
            }
        }
        entityBox.offset(position);
        return motionY;
    }

    public float collideX(RotatedCollisionHandlerImpl handler, Entity entity, MutableBoundingBox entityBox, float motionX) {
        int k = 0;
        entityBox.offset(Vector3fPool.get(position).multLocal(-1));
        for (int l = collisionBoxes.size(); k < l; ++k) {
            if (entityBox.intersects(rotatedBoxes.get(k))) {
                Vector3f motion = doRotatedCollision(handler, entity, entityBox, Vector3fPool.get(motionX, 0, 0), 0);
                //System.out.println("InitMot " + motionX +" to " + motion.x + " eb " + entityBox +" rt " +rotated);
                motionX = motion.x;
                break;
            }
        }
        entityBox.offset(position);
        return motionX;
    }

    public float collideZ(RotatedCollisionHandlerImpl handler, Entity entity, MutableBoundingBox entityBox, float motionZ) {
        //System.out.println("========");
        int k = 0;
        entityBox.offset(Vector3fPool.get(position).multLocal(-1));
        for (int l = collisionBoxes.size(); k < l; ++k) {
            if (entityBox.intersects(rotatedBoxes.get(k))) {
                Vector3f motion = doRotatedCollision(handler, entity, entityBox, Vector3fPool.get(0, 0, motionZ), 2);
                //System.out.println("InitMot " + motionZ +" to " + motion.z + " eb " + entityBox +" rt " +rotatedBoxes.get(k));
                motionZ = motion.z;
                break;
            }
        }
        entityBox.offset(position);
        return motionZ;
    }

    public static MutableBoundingBox[] rotatedPlayer = new MutableBoundingBox[3];
    public static MutableBoundingBox[] rotatedPlayerWithMotion = new MutableBoundingBox[3];
    public static MutableBoundingBox[] rotatedPlayerAfter = new MutableBoundingBox[3];
    public static AxisAlignedBB[] collisionBoxed = new AxisAlignedBB[3];
    public static Vector3f positionS;
    public static Quaternion rotationS;
    public static Vector3f[] motioned = new Vector3f[3];
    public static Vector3f[] motionedAfter = new Vector3f[3];

    private Vector3f doRotatedCollision(RotatedCollisionHandlerImpl handler, Entity entity, MutableBoundingBox entityBB, Vector3f motion, int mode) {
        positionS = position;
        rotationS = rotation;
        //System.out.println("Handeling");
        if (entity.world.isRemote && ClientDebugSystem.enableDebugDrawing)
            VehicleDebugRenderer.PlayerCollisionsDebug.pos = entity.getPositionVector();
        float oldx = motion.x, oldy = motion.y, oldz = motion.z;

        if (entity.world.isRemote && ClientDebugSystem.enableDebugDrawing)
            VehicleDebugRenderer.PlayerCollisionsDebug.motion = Vector3fPool.getPermanentVector(motion);
        motion = handler.rotate(motion, inversedRotation);
        Vector3f origin = Vector3fPool.get(motion);

        this.inversedRotation = rotation.inverse();
        if (inversedRotation == null)
            inversedRotation = Quaternion.IDENTITY;
        /*System.out.println("Fuck rotated=" + origin +" old="+ oldx + " " + oldy +" "+oldz +" redone=" + handler.rotate(origin, rotation)+" rot=" + rotation);
        System.out.println("Rot="+rotation);
        System.out.println("Inv="+inversedRotation);
        System.out.println("Redone="+handler.rotate(origin, rotation));
        System.out.println("RedoneI="+handler.rotate(origin, inversedRotation));
        if(true)
            return Vector3fPool.get();*/

        System.out.println("MOVE DIR " + oldx + " " + oldy +" " +oldz +" "+motion);
        List<EnumFacing> collisionFaces = new ArrayList<>();
        MutableBoundingBox tempBB = handler.rotateBB(Vector3fPool.get(), Vector3fPool.get((float) entity.posX, (float) entity.posY, (float) entity.posZ).subtract(position), entityBB.toBB(), inversedRotation);
        if (entity.world.isRemote && ClientDebugSystem.enableDebugDrawing) {
            VehicleDebugRenderer.PlayerCollisionsDebug.lastTemp = tempBB.toBB();
            VehicleDebugRenderer.PlayerCollisionsDebug.rotatedmotion = Vector3fPool.getPermanentVector(motion);
        }
        /*tempBB.minX = Math.ceil(tempBB.minX * 1000)/1000;
        tempBB.minY = Math.ceil(tempBB.minY * 1000)/1000;
        tempBB.minZ = Math.ceil(tempBB.minZ * 1000)/1000;
        tempBB.maxX = Math.floor(tempBB.maxX * 1000)/1000;
        tempBB.maxY = Math.floor(tempBB.maxY * 1000)/1000;
        tempBB.maxZ = Math.floor(tempBB.maxZ * 1000)/1000;*/

        rotatedPlayer[mode] = new MutableBoundingBox(tempBB);
        motioned[mode] = new Vector3f(origin);
        rotatedPlayerWithMotion[mode] = new MutableBoundingBox(tempBB).offset(motion);
        collisionBoxed[mode] = collisionBoxes.get(0);
        //tempBB.grow(-0.1f);
        //if (Math.abs(entity.getEntityBoundingBox().minY - tempBB.minY + position.y) < 0.05)
          //  tempBB.minY = entity.getEntityBoundingBox().minY - position.y + 0.01f;
  //      System.out.println("COLLIDE " + tempBB +" /f/ " + collisionBoxes.get(0));
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
      //      System.out.println("X-test");
            for (int l5 = collisionBoxes.size(); j5 < l5; ++j5) {
                float nx = RotatedCollisionHandlerImpl.calculateXOffset(collisionBoxes.get(j5), tempBB, motion.x);
                if (nx < motion.x) {
                    collisionFaces.add(EnumFacing.WEST);
                    motion.x = nx;
                    //System.out.println("Collided X W " + nx + collisionBoxes.get(j5) + tempBB);
                } else if (nx > motion.x) {
                    collisionFaces.add(EnumFacing.EAST);
                    motion.x = nx;
                    //System.out.println("Collided X E " + nx + collisionBoxes.get(j5) + tempBB);
                }
            }
            if (motion.x != 0)
                tempBB = tempBB.offset(motion.x, 0, 0);
        }
        if (motion.z != 0) {
            boolean co = false;
            int k5 = 0;
       //     System.out.println("Z-test");
            for (int i6 = collisionBoxes.size(); k5 < i6 && motion.z != 0; ++k5) {
                float nz = RotatedCollisionHandlerImpl.calculateZOffset(collisionBoxes.get(k5), tempBB, motion.z);
                if (nz < motion.z) {
                    collisionFaces.add(EnumFacing.NORTH);
                    motion.z = nz;
                    co = true;
                    //System.out.println("Collided Z N " + nz + collisionBoxes.get(k5) + tempBB);
                } else if (nz > motion.z) {
                    collisionFaces.add(EnumFacing.SOUTH);
                    motion.z = nz;
                    co = true;
                    //System.out.println("Collided Z S " + nz + collisionBoxes.get(k5) + tempBB);
                }
            }
            if (motion.z != 0)
                tempBB = tempBB.offset(0, 0, motion.z);
        }
        rotatedPlayerAfter[mode] = new MutableBoundingBox(tempBB);
        if (entity.world.isRemote && ClientDebugSystem.enableDebugDrawing)
            VehicleDebugRenderer.PlayerCollisionsDebug.realmotionrot = Vector3fPool.getPermanentVector(motion);

        if (!motion.equals(origin)) {
            origin = motion;
            motion = handler.rotate(motion, rotation);
            if(entity.world.isRemote)
                System.out.println("Resulted motion is " + motion+ " "+ origin);
            float eps = 0.01f;
            if (Math.abs(motion.x - oldx) < eps / 5)
                motion.x = oldx;
            if (Math.abs(motion.y - oldy) < eps / 5)
                motion.y = oldy;
            if (Math.abs(motion.z - oldz) < eps / 5)
                motion.z = oldz;
            motionedAfter[mode] = new Vector3f(motion);
        } else {
            origin = motion;
            motion = handler.rotate(motion, rotation);
            if(entity.world.isRemote)
                System.out.println("NO CHANGE Resulted motion is " + motion+ " "+ origin);
            motion = Vector3fPool.get(oldx, oldy, oldz);
            motionedAfter[mode] = new Vector3f(motion);
        }
        if (entity.world.isRemote && ClientDebugSystem.enableDebugDrawing)
            VehicleDebugRenderer.PlayerCollisionsDebug.realmotion = Vector3fPool.getPermanentVector(motion);
        return motion;
    }

    public List<MutableBoundingBox> getCollisionBoxes() {
        //TODO OPTI
        return collisionBoxes.stream().map(MutableBoundingBox::new).map(b -> b.offset(position)).collect(Collectors.toList());
    }
}
