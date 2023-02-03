package fr.dynamx.common.handlers;

import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import fr.dynamx.api.physics.IRotatedCollisionHandler;
import fr.dynamx.client.handlers.ClientDebugSystem;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.contentpack.parts.PartShape;
import fr.dynamx.common.entities.ICollidableObject;
import fr.dynamx.common.entities.PhysicsEntity;
import fr.dynamx.common.entities.PropsEntity;
import fr.dynamx.common.physics.player.WalkingOnPlayerController;
import fr.dynamx.utils.DynamXConfig;
import fr.dynamx.utils.debug.renderer.VehicleDebugRenderer;
import fr.dynamx.utils.maths.DynamXGeometry;
import fr.dynamx.utils.maths.DynamXMath;
import fr.dynamx.utils.optimization.*;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Provides helper methods for rotated collisions and handles collisions with entities <br>
 * This code can be largely improved, feel free to suggest modifications :)
 */
public class RotatedCollisionHandlerImpl implements IRotatedCollisionHandler {
    @Override
    public Vector3f rotate(Vector3f pos, Quaternion rotation) {
        if (rotation == null) {
            return Vector3fPool.get(pos);
        }
        return DynamXGeometry.rotateVectorByQuaternion(pos, rotation);
    }

    @Override
    public MutableBoundingBox rotateBB(Vector3f offset, Vector3f pos, AxisAlignedBB from, Quaternion rotation) {
        Vector3f tp = rotate(Vector3fPool.get(pos.x - offset.x, pos.y - offset.y, pos.z - offset.z), rotation); //get rotated offset viewed from the bullet entity with a yaw of 0
        MutableBoundingBox bb = rotateBB(pos, new MutableBoundingBox(from), rotation);
        bb.offset(Vector3fPool.get(offset.x + tp.x - pos.x, offset.y + tp.y - pos.y, offset.z + tp.z - pos.z)); //The player box is symmetric, re-put the player box next to the bullet entity, and remove box offset (pos) added by previous rotateBB call
        return bb;
    }

    @Override
    public MutableBoundingBox rotateBB(Vector3f pos, MutableBoundingBox from, Quaternion rotation) {
        from.offset(pos.multLocal(-1)); //put at 0
        pos.multLocal(-1); //Restore pos value
        Vector3f v1 = rotate(Vector3fPool.get((float) from.minX, 0, 0), rotation);
        Vector3f v2 = rotate(Vector3fPool.get(0, (float) from.minY, 0), rotation);
        Vector3f v3 = rotate(Vector3fPool.get(0, 0, (float) from.minZ), rotation);
        Vector3f v4 = rotate(Vector3fPool.get((float) from.maxX, 0, 0), rotation);
        Vector3f v5 = rotate(Vector3fPool.get(0, (float) from.maxY, 0), rotation);
        Vector3f v6 = rotate(Vector3fPool.get(0, 0, (float) from.maxZ), rotation);
        MutableBoundingBox n = new MutableBoundingBox(DynamXMath.getMin(v1.x, v2.x, v3.x, v4.x, v5.x, v6.x), DynamXMath.getMin(v1.y, v2.y, v3.y, v4.y, v5.y, v6.y), DynamXMath.getMin(v1.z, v2.z, v3.z, v4.z, v5.z, v6.z),
                DynamXMath.getMax(v1.x, v2.x, v3.x, v4.x, v5.x, v6.x), DynamXMath.getMax(v1.y, v2.y, v3.y, v4.y, v5.y, v6.y), DynamXMath.getMax(v1.z, v2.z, v3.z, v4.z, v5.z, v6.z));
        n.offset(pos);
        return n;
    }

    public static double eps = 0.2f;

    public static float calculateXOffset(AxisAlignedBB against, MutableBoundingBox other, float offsetX) {
        if (other.maxY > against.minY && other.minY < against.maxY && other.maxZ > against.minZ && other.minZ < against.maxZ) {
            if (offsetX > 0.0D && other.maxX - eps <= against.minX) {
                float d1 = (float) (against.minX - other.maxX);

                if (d1 < offsetX) {
                    offsetX = d1;
                }
            } else if (offsetX < 0.0D && other.minX + eps >= against.maxX) {
                float d0 = (float) (against.maxX - other.minX);

                if (d0 > offsetX) {
                    offsetX = d0;
                }
            }

            return offsetX;
        } else {
            return offsetX;
        }
    }

    public static float calculateYOffset(AxisAlignedBB against, MutableBoundingBox other, float offsetY) {
        if (other.maxX > against.minX && other.minX < against.maxX && other.maxZ > against.minZ && other.minZ < against.maxZ) {
            if (offsetY > 0.0D && other.maxY - eps <= against.minY) {
                float d1 = (float) (against.minY - other.maxY);

                if (d1 < offsetY) {
                    offsetY = d1;
                }
            } else if (offsetY < 0.0D && other.minY + eps >= against.maxY) {
                float d0 = (float) (against.maxY - other.minY);

                if (d0 > offsetY) {
                    offsetY = d0;
                }
            }

            return offsetY;
        } else {
            return offsetY;
        }
    }

    public static float calculateZOffset(AxisAlignedBB against, MutableBoundingBox other, float offsetZ) {
        if (other.maxX > against.minX && other.minX < against.maxX && other.maxY > against.minY && other.minY < against.maxY) {
            if (offsetZ > 0.0D && other.maxZ - eps <= against.minZ) {
                float d1 = (float) (against.minZ - other.maxZ);

                if (d1 < offsetZ) {
                    offsetZ = d1;
                }
            } else if (offsetZ < 0.0D && other.minZ + eps >= against.maxZ) {
                float d0 = (float) (against.maxZ - other.minZ);

                if (d0 > offsetZ) {
                    offsetZ = d0;
                }
            }

            return offsetZ;
        } else {
            return offsetZ;
        }
    }

    /*private Vector3f collideWith(Entity entity, ICollidableObject with, float mx, float my, float mz, int step) {
        if (entity.world.isRemote && ClientDebugSystem.enableDebugDrawing)
            VehicleDebugRenderer.PlayerCollisionsDebug.pos = entity.getPositionVector();
        float oldx = mx, oldy = my, oldz = mz;
        eps = 0.1;

        Vector3f data = Vector3fPool.get(mx, my, mz);
        if (entity.world.isRemote && ClientDebugSystem.enableDebugDrawing)
            VehicleDebugRenderer.PlayerCollisionsDebug.motion = Vector3fPool.getPermanentVector(data);
        Quaternion withRotation = with.getCollisionInfo().getRotation();
        Quaternion inversedWithRotation = withRotation.inverse();
        if (inversedWithRotation == null) //error when loading world
            return Vector3fPool.get(oldx, oldy, oldz);
        data = rotate(data, inversedWithRotation);
        mx = data.x;
        my = data.y;
        mz = data.z;
        float ox = mx, oy = my, oz = mz;

        List<EnumFacing> collisionFaces = new ArrayList<>();
        List<MutableBoundingBox> list1 = with.getCollisionInfo().getCollisionBoxes();
        MutableBoundingBox tempBB = rotateBB(with.getCollisionInfo().getPosition(), Vector3fPool.get((float) entity.posX, (float) entity.posY, (float) entity.posZ), entity.getEntityBoundingBox(), inversedWithRotation);
        if (entity.world.isRemote && ClientDebugSystem.enableDebugDrawing) {
            VehicleDebugRenderer.PlayerCollisionsDebug.lastTemp = tempBB.toBB();
            VehicleDebugRenderer.PlayerCollisionsDebug.rotatedmotion = Vector3fPool.getPermanentVector(data);
        }
        if(Math.abs(entity.getEntityBoundingBox().minY - tempBB.minY) < 0.1)
            tempBB.minY = entity.getEntityBoundingBox().minY+0.01f;
        if (step == 0 || step == 1)
        {
            int k = 0;
            for (int l = list1.size(); k < l; ++k) {
                float ny = calculateYOffset(list1.get(k), tempBB, my);
                if (ny < my) {
                    collisionFaces.add(EnumFacing.DOWN);
                    my = ny;
                } else if (ny > my) {
                    collisionFaces.add(EnumFacing.UP);
                    my = ny;
                }
            }
            if (my != 0)
                tempBB = tempBB.offset(0, my, 0);
        }
        if (step == 0 || step == 2)
        {
            int j5 = 0;
            for (int l5 = list1.size(); j5 < l5; ++j5) {
                float nx = calculateXOffset(list1.get(j5), tempBB, mx);
                if (nx < mx) {
                    collisionFaces.add(EnumFacing.WEST);
                    mx = nx;
                } else if (nx > mx) {
                    collisionFaces.add(EnumFacing.EAST);
                    mx = nx;
                }
            }
            if (mx != 0)
                tempBB = tempBB.offset(mx, 0, 0);
        }
        if (step == 0 || step == 3)
        {
            int k5 = 0;
            for (int i6 = list1.size(); k5 < i6; ++k5) {
                float nz = calculateZOffset(list1.get(k5), tempBB, mz);
                if (nz < mz) {
                    collisionFaces.add(EnumFacing.NORTH);
                    mz = nz;
                } else if (nz > mz) {
                    collisionFaces.add(EnumFacing.SOUTH);
                    mz = nz;
                }
            }
            //if(mz != 0)
            //tempBB = tempBB.offset(0, 0, mz);
        }
        data = Vector3fPool.get(mx, my, mz);
        if (entity.world.isRemote && ClientDebugSystem.enableDebugDrawing)
            VehicleDebugRenderer.PlayerCollisionsDebug.realmotionrot = Vector3fPool.getPermanentVector(data);

        if (mx != ox || my != oy || mz != oz) {
            // if(entity.world.isRemote)
            //System.out.println("COLL With is "+withRotation+" "+entity.ticksExisted+" "+with+" "+withPosition+" "+data+" "+tempBB);
            data = rotate(data, withRotation);

            /*if(Math.abs(data.x) > Math.abs(oldx))
                data.x = oldx;
            else *//*
            float eps = 0.1f;
            if (Math.abs(data.x - oldx) < eps / 5)
                data.x = oldx;
            /*if(Math.abs(data.y) > Math.abs(oldy))
                data.y = oldy;
            else *//*
            if (Math.abs(data.y - oldy) < eps / 5)
                data.y = oldy;
            /*if(Math.abs(data.z) > Math.abs(oldz))
                data.z = oldz;
            else *//*
            if (Math.abs(data.z - oldz) < eps / 5)
                data.z = oldz;

            if (with instanceof PhysicsEntity && entity.world.isRemote && entity instanceof EntityPlayer &&
                    (!(with instanceof PropsEntity) || ((PropsEntity<?>) with).getPackInfo().getPartShapes().isEmpty() || ((PropsEntity<?>) with).getPackInfo().getPartShapes().get(0).getShapeType() == PartShape.EnumPartType.BOX) &&
                    !collisionFaces.isEmpty() && WalkingOnPlayerController.controller == null && ((EntityPlayer) entity).isUser() && !DynamXContext.getPlayerPickingObjects().containsKey(entity.getEntityId())) //WIP
            {
                PhysicsEntity<?> collidingWith = (PhysicsEntity<?>) with;
                for (EnumFacing f : collisionFaces) {
                    if (!collisionFaces.contains(f.getOpposite())) //If not stuck between 2 aabb
                    {
                        Vector3f vh = rotate(Vector3fPool.get((float) collidingWith.motionX, (float) collidingWith.motionY, (float) collidingWith.motionZ), inversedWithRotation);
                        float projVehicMotion = Vector3fPool.get(vh.x, vh.y, vh.z).dot(Vector3fPool.get(f.getDirectionVec().getX(), f.getDirectionVec().getY(), f.getDirectionVec().getZ()));
                        //if (projVehicMotion != 0)
                        //  System.out.println("Collide on face " + f + " " + projVehicMotion);
                        if (projVehicMotion != 0) //We push the player
                        {
                            switch (f) {
                                case DOWN:
                                    data.y += collidingWith.motionY;
                                    break;
                                case NORTH:
                                case SOUTH:
                                    //System.out.println("Collide on " + f + " " + mx + " " + with.motionX);
                                    //entity.addVelocity(with.motionX * 1.5f, 0, 0);
                                case WEST:
                                case EAST:
                                    //System.out.println("Collide on " + f + " " + mz + " " + with.motionZ);
                                    //entity.addVelocity(0, 0, with.motionZ * 1.5f);
                                    break;
                                case UP:
                                    if (collidingWith.canPlayerStandOnTop()) {
                                        Vector3f offset = Vector3fPool.get((float) (entity.posX - collidingWith.posX + data.x), (float) (entity.posY - collidingWith.posY + data.y), (float) (entity.posZ - collidingWith.posZ + data.z));
                                        offset = rotate(offset, inversedWithRotation);
                                        offset = Vector3fPool.getPermanentVector(offset); //We don't want an instance from the pool
                                        //System.out.println("Collision point for " + collisionFaces.get(0) + " at " + offsetv);
                                        WalkingOnPlayerController.controller = new WalkingOnPlayerController((EntityPlayer) entity, collidingWith, f, offset);
                                        collidingWith.walkingOnPlayers.put((EntityPlayer) entity, WalkingOnPlayerController.controller);
                                        DynamXContext.getWalkingPlayers().put((EntityPlayer) entity, collidingWith);
                                        collidingWith.getSynchronizer().onWalkingPlayerChange(entity.getEntityId(), offset, (byte) f.getIndex());
                                    } else
                                        data.y += collidingWith.motionY;
                                    break;
                            }
                        }
                    }
                }
            }
        } else {
            // if(entity.world.isRemote)
            // System.out.println("PAS COLL With is "+withRotation+" "+entity.ticksExisted+" "+with+" "+withPosition+" "+data+" "+tempBB);
            data = Vector3fPool.get(oldx, oldy, oldz);
        }
        if (entity.world.isRemote && ClientDebugSystem.enableDebugDrawing)
            VehicleDebugRenderer.PlayerCollisionsDebug.realmotion = Vector3fPool.getPermanentVector(data);
        return data;
    }*/

    private static double min(double a, double b) {
        if (Math.abs(a) > Math.abs(b))
            return b;
        return a;
    }

    /**
     * TODO UPDATE DOC
     * Gets all the {@link CollisionInfo} of all {@link ICollidableObject} TileEntities in the given bounding box
     *
     * @param world The world
     * @param inBox The box where we search the {@link ICollidableObject}
     * @param result The {@link CollisionInfo} of all {@link ICollidableObject} {@link TileEntity} inside of the box
     */
    private void getCollidableTileEntities(World world, MutableBoundingBox inBox, List<CollisionInfo> result) {
        BlockPos.MutableBlockPos pos = BlockPos.PooledMutableBlockPos.retain();
        Chunk lastChunk = null;
        Vector3f radius = inBox.getSize().divideLocal(2);
        // Iterate on chunks near to the player
        for (int x = (int) Math.floor(inBox.minX); x <= inBox.maxX; x += Math.ceil(radius.x)) {
            for (int y = (int) Math.floor(inBox.minY); y <= inBox.maxY; y += Math.ceil(radius.y)) {
                for (int z = (int) Math.floor(inBox.minZ); z <= inBox.maxZ; z += Math.ceil(radius.z)) {
                    pos.setPos(x, y, z);
                    if (lastChunk == null || lastChunk.x != (x >> 4) || lastChunk.z != (z >> 4)) {
                        // If it's a new chunk, add all ICollidableObject inside the box
                        lastChunk = world.getChunk(pos);
                        for (Map.Entry<BlockPos, TileEntity> te : lastChunk.getTileEntityMap().entrySet()) {
                            if (te.getValue() instanceof ICollidableObject && inBox.contains(te.getKey()))
                                result.add(((ICollidableObject) te.getValue()).getCollisionInfo());
                        }
                    }
                }
            }
        }
    }

    private boolean motionChanged;

    @Override
    public boolean motionHasChanged() {
        return motionChanged;
    }

    private final ThreadLocal<List<CollisionInfo>> collisions = ThreadLocal.withInitial(LinkedList::new);

    @Override
    public double[] handleCollisionWithBulletEntities(Entity entity, double mx, double my, double mz) {
        double icollidableCheckRadius = entity instanceof EntityPlayer ? DynamXConfig.blockCollisionRadius : 0;
        AxisAlignedBB copy = entity.getEntityBoundingBox().grow(0);

        int multiplier = entity instanceof EntityPlayer ? 2 : 1;
        List<AxisAlignedBB> list1 = entity.world.getCollisionBoxes(entity, entity.getEntityBoundingBox().expand(mx * multiplier, my * multiplier, mz * multiplier));

        double nx = mx, ny = my, nz = mz;
        if (ny != 0.0D) {
            int k = 0;
            for (int l = list1.size(); k < l; ++k) {
                ny = (list1.get(k)).calculateYOffset(copy, ny);
            }
            if (ny != 0) {
                copy = copy.offset(0.0D, ny, 0.0D);
            }
        }
        if (nx != 0.0D) {
            int j5 = 0;
            for (int l5 = list1.size(); j5 < l5; ++j5) {
                nx = (list1.get(j5)).calculateXOffset(copy, nx);
            }
            if (nx != 0.0D) {
                copy = copy.offset(nx, 0.0D, 0.0D);
            }
        }
        if (nz != 0.0D) {
            int k5 = 0;
            for (int i6 = list1.size(); k5 < i6; ++k5) {
                nz = (list1.get(k5)).calculateZOffset(copy, nz);
            }
        }

        motionChanged = false;
        if (entity instanceof EntityLivingBase)
        {
            Vector3fPool.openPool();
            QuaternionPool.openPool();
            List<CollisionInfo> collisions = this.collisions.get();
            // get TEs
            getCollidableTileEntities(entity.world, new MutableBoundingBox(entity.getEntityBoundingBox()).grow(icollidableCheckRadius), collisions);
            // get entities
            List<CollisionInfo> entities = entity.world.getEntitiesWithinAABB(PhysicsEntity.class, entity.getEntityBoundingBox().grow(icollidableCheckRadius)).stream().map(ICollidableObject::getCollisionInfo).collect(Collectors.toList());
            collisions.addAll(entities);
            // do collide
            float castx = (float) nx, casty = (float) ny, castz = (float) nz;
            MutableBoundingBox tempBB = new MutableBoundingBox(entity.getEntityBoundingBox());
            if (ny != 0) {
                for(CollisionInfo collision : collisions) {
                    float move = collision.collideY(this, entity, tempBB, casty);
                    if(move != casty) {
                        casty = move;
                        ny = move;
                        motionChanged = true;
                    }
                }
                if (ny != 0)
                    tempBB = tempBB.offset(0, ny, 0);
            }
            if (nx != 0) {
                for(CollisionInfo collision : collisions) {
                    float move = collision.collideX(this, entity, tempBB, castx);
                    if(move != castx) {
                        castx = move;
                        nx = move;
                        motionChanged = true;
                    }
                }
                if (nx != 0)
                    tempBB = tempBB.offset(nx, 0, 0);
            }
            if (nz != 0) {
                for(CollisionInfo collision : collisions) {
                    float move = collision.collideZ(this, entity, tempBB, castz);
                    if(move != castz) {
                        castz = move;
                        nz = move;
                        motionChanged = true;
                    }
                }
            }
            collisions.clear();
            QuaternionPool.closePool();
            Vector3fPool.closePool();
            /*for (PhysicsEntity e : entities) {
                if (!DynamXContext.getPlayerPickingObjects().containsValue(e.getEntityId())) {
                    //System.out.println("Input "+mx+" "+my+" "+mz+" "+nx+" "+ny+" "+nz+" "+entity.onGround+" "+entity.collidedVertically+" "+e.physicsPosition);
                    Vector3fPool.openPool();
                    QuaternionPool.openPool();
                    castx = (float) nx; casty = (float) ny; castz = (float) nz;
                    Vector3f n = collideWith(entity, e, castx, casty, castz, 0);
                    if (castx != n.x) {
                        nx = n.x;
                        motionChanged = true;
                    }
                    if (casty != n.y) {
                        motionChanged = true;
                        ny = n.y;
                    }
                    if (castz != n.z) {
                        motionChanged = true;
                        nz = n.z;
                    }
                    QuaternionPool.closePool();
                    Vector3fPool.closePool();
                }
            }*/
        }
        my = ny;
        mx = nx;
        mz = nz;
        if (my != 0.0D) {
            int k = 0;
            if (motionChanged) {
                for (int l = list1.size(); k < l; ++k) {
                    my = (list1.get(k)).calculateYOffset(entity.getEntityBoundingBox(), my);
                }
            }
            if (my != 0) {
                entity.setEntityBoundingBox(entity.getEntityBoundingBox().offset(0.0D, min(my, ny), 0.0D));
            }
        }
        if (mx != 0.0D) {
            int j5 = 0;
            if (motionChanged) {
                for (int l5 = list1.size(); j5 < l5; ++j5) {
                    mx = (list1.get(j5)).calculateXOffset(entity.getEntityBoundingBox(), mx);
                }
            }
            if (mx != 0.0D) {
                entity.setEntityBoundingBox(entity.getEntityBoundingBox().offset(min(mx, nx), 0.0D, 0.0D));
            }
        }
        if (mz != 0.0D) {
            int k5 = 0;
            if (motionChanged) {
                for (int i6 = list1.size(); k5 < i6; ++k5) {
                    mz = (list1.get(k5)).calculateZOffset(entity.getEntityBoundingBox(), mz);
                }
            }
            if (mz != 0.0D) {
                entity.setEntityBoundingBox(entity.getEntityBoundingBox().offset(0.0D, 0.0D, min(mz, nz)));
            }
        }
        return new double[]{mx, my, mz};
    }
}
