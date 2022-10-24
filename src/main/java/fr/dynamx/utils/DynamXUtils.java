package fr.dynamx.utils;

import com.google.common.base.Predicates;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import fr.dynamx.api.contentpack.object.IPartContainer;
import fr.dynamx.api.contentpack.object.part.BasePart;
import fr.dynamx.api.entities.VehicleEntityProperties;
import fr.dynamx.api.contentpack.ContentPackType;
import fr.dynamx.api.obj.ObjModelPath;
import fr.dynamx.api.physics.EnumBulletShapeType;
import fr.dynamx.common.contentpack.DynamXObjectLoaders;
import fr.dynamx.common.contentpack.PackInfo;
import fr.dynamx.common.entities.BaseVehicleEntity;
import fr.dynamx.common.entities.PackPhysicsEntity;
import fr.dynamx.common.entities.modules.EngineModule;
import fr.dynamx.utils.maths.DynamXGeometry;
import fr.dynamx.utils.optimization.Vector3fPool;
import fr.dynamx.utils.physics.DynamXPhysicsHelper;
import fr.dynamx.utils.physics.PhysicsRaycastResult;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagDouble;
import net.minecraft.nbt.NBTTagFloat;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.EntitySelectors;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.apache.commons.io.output.ByteArrayOutputStream;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * General utility methods
 *
 * @see fr.dynamx.utils.maths.DynamXMath
 * @see fr.dynamx.utils.maths.DynamXGeometry
 * @see DynamXPhysicsHelper
 */
public class DynamXUtils {

    public static void writeBlockPos(ByteBuf buf, BlockPos blockPos) {
        buf.writeDouble(blockPos.getX());
        buf.writeDouble(blockPos.getY());
        buf.writeDouble(blockPos.getZ());
    }

    public static BlockPos readBlockPos(ByteBuf buf) {
        return new BlockPos(buf.readDouble(), buf.readDouble(), buf.readDouble());
    }

    public static void writeVector3f(ByteBuf buf, Vector3f vector3f) {
        buf.writeFloat(vector3f.x);
        buf.writeFloat(vector3f.y);
        buf.writeFloat(vector3f.z);
    }

    public static Vector3f readVector3f(ByteBuf buf) {
        return new Vector3f(buf.readFloat(), buf.readFloat(), buf.readFloat());
    }

    public static void writeQuaternion(ByteBuf buf, Quaternion quaternion) {
        buf.writeFloat(quaternion.getX());
        buf.writeFloat(quaternion.getY());
        buf.writeFloat(quaternion.getZ());
        buf.writeFloat(quaternion.getW());
    }

    public static Quaternion readQuaternion(ByteBuf buf) {
        return new Quaternion(buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat());
    }

    public static void writeQuaternionNBT(NBTTagCompound compound, Quaternion quaternion) {
        compound.setFloat("QuatX", quaternion.getX());
        compound.setFloat("QuatY", quaternion.getY());
        compound.setFloat("QuatZ", quaternion.getZ());
        compound.setFloat("QuatW", quaternion.getW());
    }

    public static Quaternion readQuaternionNBT(NBTTagCompound compound) {
        return new Quaternion(compound.getFloat("QuatX"), compound.getFloat("QuatY"), compound.getFloat("QuatZ"), compound.getFloat("QuatW"));
    }

    /**
     * @return A new {@link ObjModelPath} for this model
     */
    public static ObjModelPath getModelPath(String packName, String model) {
        PackInfo info = DynamXObjectLoaders.PACKS.findPackInfoByPackName(packName);
        if (info == null) {
            System.err.println("WTF PACK INFO " + packName + " NOT FOUND");
            return new ObjModelPath(new PackInfo(packName, ContentPackType.FOLDER), RegistryNameSetter.getDynamXModelResourceLocation(model));
        }
        return new ObjModelPath(info, RegistryNameSetter.getDynamXModelResourceLocation(model));
    }

    public static byte[] readInputStream(InputStream resource) throws IOException {
        int i;
        byte[] buffer = new byte[65565];
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        while ((i = resource.read(buffer, 0, buffer.length)) != -1) {
            out.write(buffer, 0, i);
        }
        out.flush();
        out.close();
        return out.toByteArray();
    }

    public static Vector3f toVector3f(Vec3d pos) {
        return Vector3fPool.get((float) pos.x, (float) pos.y, (float) pos.z);
    }

    public static Vector3f toVector3f(BlockPos pos) {
        return Vector3fPool.get((float) pos.getX(), (float) pos.getY(), (float) pos.getZ());
    }

    public static Vector3f getPositionEyes(Entity entity) {
        return Vector3fPool.get((float) entity.posX, (float) entity.posY + entity.getEyeHeight(), (float) entity.posZ);
    }

    public static Vector3f calculateRay(Entity base, float distance, Vector3f offset) {
        Vec3d vec3 = base.getPositionVector();
        Vec3d vec31 = base.getLook(1);
        Vec3d vec32 = vec3.add(vec31.x * distance, vec31.y * distance, vec31.z * distance);
        Vector3f lookAt = Vector3fPool.get((float) vec32.x, (float) vec32.y, (float) vec32.z);
        lookAt.subtractLocal(offset.x, offset.y, offset.z);
        return lookAt;
    }

    public static PhysicsRaycastResult castRayFromEntity(Entity entity, float distanceMax, Predicate<EnumBulletShapeType> ignoredPredicate) {
        Vector3f eyePos = DynamXUtils.getPositionEyes(entity); //from
        Vector3f eyeLook = DynamXUtils.toVector3f(entity.getLook(1)); //to
        Vector3f lookAt = new Vector3f(eyePos.x, eyePos.y, eyePos.z);
        eyeLook.multLocal(distanceMax);
        lookAt.addLocal(eyeLook);

        return DynamXPhysicsHelper.castRay(eyePos, lookAt, ignoredPredicate);
    }

    public static NBTTagList newDoubleNBTList(double... numbers) {
        NBTTagList nbttaglist = new NBTTagList();

        for (double d0 : numbers) {
            nbttaglist.appendTag(new NBTTagDouble(d0));
        }

        return nbttaglist;
    }

    public static NBTTagList newFloatNBTList(float... numbers) {
        NBTTagList nbttaglist = new NBTTagList();

        for (float f : numbers) {
            nbttaglist.appendTag(new NBTTagFloat(f));
        }

        return nbttaglist;
    }

    public static Vector3f getCameraTranslation(Minecraft mc, float delta) {
        return Vector3fPool.get((float) mc.player.prevPosX + (float) (mc.player.posX - mc.player.prevPosX) * delta, (float) mc.player.prevPosY + (float) (mc.player.posY - (float) mc.player.prevPosY) * delta, (float) mc.player.prevPosZ + (float) (mc.player.posZ - (float) mc.player.prevPosZ) * delta);
    }

    public static RayTraceResult rayTraceEntitySpawn(World worldIn, EntityPlayer playerIn, EnumHand hand) {
        return getMouseOver(playerIn, 1);
    }

    /**
     * We put that here, because smart people of Forge think a ray trace is a client thing
     */
    private static RayTraceResult rayTrace(Entity entity, double blockReachDistance, float partialTicks) {
        Vec3d vec3d = entity.getPositionEyes(partialTicks);
        Vec3d vec3d1 = entity.getLook(partialTicks);
        Vec3d vec3d2 = vec3d.add(vec3d1.x * blockReachDistance, vec3d1.y * blockReachDistance, vec3d1.z * blockReachDistance);
        return entity.world.rayTraceBlocks(vec3d, vec3d2, false, false, true);
    }

    public static RayTraceResult getMouseOver(Entity entity, float partialTicks) {
        RayTraceResult objectMouseOver = null;
        if (entity != null) {
            if (entity.world != null) {
                Entity pointedEntity = null;
                double d0 = 5;
                objectMouseOver = rayTrace(entity, d0, partialTicks);
                Vec3d vec3d = entity.getPositionEyes(partialTicks);
                int i = 3;
                double d1 = d0;

                if (objectMouseOver != null) {
                    d1 = objectMouseOver.hitVec.distanceTo(vec3d);
                }

                Vec3d vec3d1 = entity.getLook(1.0F);
                Vec3d vec3d2 = vec3d.add(vec3d1.x * d0, vec3d1.y * d0, vec3d1.z * d0);
                Vec3d vec3d3 = null;
                float f = 1.0F;
                List<Entity> list = entity.world.getEntitiesInAABBexcluding(entity, entity.getEntityBoundingBox().expand(vec3d1.x * d0, vec3d1.y * d0, vec3d1.z * d0).grow(1.0D, 1.0D, 1.0D), Predicates.and(EntitySelectors.NOT_SPECTATING, new com.google.common.base.Predicate<Entity>() {
                    public boolean apply(@Nullable Entity p_apply_1_) {
                        return p_apply_1_ != null && p_apply_1_.canBeCollidedWith();
                    }
                }));
                double d2 = d1;

                for (Entity entity1 : list) {
                    AxisAlignedBB axisalignedbb = entity1.getEntityBoundingBox().grow(entity1.getCollisionBorderSize());
                    RayTraceResult raytraceresult = axisalignedbb.calculateIntercept(vec3d, vec3d2);

                    if (axisalignedbb.contains(vec3d)) {
                        if (d2 >= 0.0D) {
                            pointedEntity = entity1;
                            vec3d3 = raytraceresult == null ? vec3d : raytraceresult.hitVec;
                            d2 = 0.0D;
                        }
                    } else if (raytraceresult != null) {
                        double d3 = vec3d.distanceTo(raytraceresult.hitVec);

                        if (d3 < d2 || d2 == 0.0D) {
                            if (entity1.getLowestRidingEntity() == entity.getLowestRidingEntity() && !entity1.canRiderInteract()) {
                                if (d2 == 0.0D) {
                                    pointedEntity = entity1;
                                    vec3d3 = raytraceresult.hitVec;
                                }
                            } else {
                                pointedEntity = entity1;
                                vec3d3 = raytraceresult.hitVec;
                                d2 = d3;
                            }
                        }
                    }
                }

                if (pointedEntity != null && vec3d.distanceTo(vec3d3) > 3.0D) {
                    pointedEntity = null;
                    objectMouseOver = new RayTraceResult(RayTraceResult.Type.MISS, vec3d3, null, new BlockPos(vec3d3));
                }

                if (pointedEntity != null && (d2 < d1 || objectMouseOver == null)) {
                    objectMouseOver = new RayTraceResult(pointedEntity, vec3d3);
                    /*if (pointedEntity instanceof EntityLivingBase || this.pointedEntity instanceof EntityItemFrame)
                    {
                        this.mc.pointedEntity = this.pointedEntity;
                    }*/
                }
            }
        }
        return objectMouseOver;
    }

    public static BasePart<?> rayTestPart(EntityPlayer player, PackPhysicsEntity<?, ?> entityPart, IPartContainer<?> packInfo, Predicate<BasePart<?>> wantedPart) {
        Vector3fPool.openPool();
        Vec3d lookVec = player.getLook(1.0F);
        Vec3d hitVec = player.getPositionVector().add(0, player.getEyeHeight(), 0);
        BasePart<?> nearest = null;
        Vector3f nearestPos = null;
        Vector3f playerPos = Vector3fPool.get((float) player.posX, (float) player.posY, (float) player.posZ);
        for (float f = 1.0F; f < 4.0F; f += 0.1F) {
            for (BasePart<?> part : packInfo.getAllParts()) {
                if(wantedPart != null && !wantedPart.test(part)){
                    continue;
                }
                Vector3f partPos = DynamXGeometry.rotateVectorByQuaternion(part.getPosition(), entityPart.physicsRotation);
                Vector3fPool.openPool();
                partPos.addLocal(toVector3f(entityPart.getPositionVector()));
                Vector3fPool.closePool();
                if ((nearestPos == null || DynamXGeometry.distanceBetween(partPos, playerPos) < DynamXGeometry.distanceBetween(nearestPos, playerPos))
                        && vecInsideBox(hitVec, part, partPos)) {
                    nearest = part;
                    nearestPos = partPos;
                }
            }
            hitVec = hitVec.add(lookVec.x * 0.1F, lookVec.y * 0.1F, lookVec.z * 0.1F);
        }
        Vector3fPool.closePool();
        return nearest;
    }

    public static boolean vecInsideBox(Vec3d vec1, BasePart<?> part, Vector3f pos) {
        float minX = -part.getScale().x + pos.x;
        float minY = pos.y;
        float minZ = -part.getScale().z + pos.z;
        float maxX = part.getScale().x + pos.x;
        float maxY = part.getScale().y + pos.y;
        float maxZ = part.getScale().z + pos.z;
        return vec1.x > minX && vec1.x < maxX && vec1.y > minY && vec1.y < maxY && vec1.z > minZ && vec1.z < maxZ;
    }

    public static List<Vector3f> floatBufferToVec3f(FloatBuffer buffer, Vector3f offset) {
        List<Vector3f> vector3fList = new ArrayList<>();
        for (int i = 0; i < buffer.limit() / 3; i++) {
            float xF = buffer.get(i * 3);
            float yF = buffer.get(i * 3 + 1);
            float zF = buffer.get(i * 3 + 2);
            vector3fList.add(new Vector3f(xF + offset.x, yF + offset.y, zF + offset.z));
        }
        return vector3fList;
    }

    //DUPLICATE (function is already in the BasicsAddon)
    public static int getSpeed(BaseVehicleEntity<?> entity) {
        EngineModule engine = entity.getModuleByType(EngineModule.class);
        if(engine != null){
            float[] ab = engine.getEngineProperties();
            if(ab == null) return 0;
            return (int) Math.abs(ab[VehicleEntityProperties.EnumEngineProperties.SPEED.ordinal()]);
        }
        return -1;
    }

}
