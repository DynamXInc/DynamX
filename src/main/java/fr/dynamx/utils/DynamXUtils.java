package fr.dynamx.utils;

import com.google.common.base.Predicates;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import de.javagl.jgltf.model.NodeModel;
import fr.dynamx.api.contentpack.ContentPackType;
import fr.dynamx.api.contentpack.object.IPackInfoReloadListener;
import fr.dynamx.api.contentpack.object.IPartContainer;
import fr.dynamx.api.contentpack.object.part.BasePart;
import fr.dynamx.api.dxmodel.DxModelPath;
import fr.dynamx.api.dxmodel.EnumDxModelFormats;
import fr.dynamx.api.entities.VehicleEntityProperties;
import fr.dynamx.api.physics.EnumBulletShapeType;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.DynamXMain;
import fr.dynamx.common.contentpack.DynamXObjectLoaders;
import fr.dynamx.common.contentpack.PackInfo;
import fr.dynamx.common.entities.BaseVehicleEntity;
import fr.dynamx.common.entities.PackPhysicsEntity;
import fr.dynamx.common.entities.modules.TrailerAttachModule;
import fr.dynamx.common.entities.modules.engines.BasicEngineModule;
import fr.dynamx.common.entities.vehicles.TrailerEntity;
import fr.dynamx.common.objloader.data.DxModelData;
import fr.dynamx.common.objloader.data.GltfModelData;
import fr.dynamx.common.physics.joints.EntityJoint;
import fr.dynamx.common.physics.joints.EntityJointsHandler;
import fr.dynamx.common.physics.utils.StairsBox;
import fr.dynamx.utils.maths.DynamXGeometry;
import fr.dynamx.utils.optimization.MutableBoundingBox;
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
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EntitySelectors;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.joml.Quaternionf;
import org.lwjgl.BufferUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.*;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.*;
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
     * @return A new {@link DxModelPath} for this model
     */
    public static DxModelPath getModelPath(String packName, ResourceLocation model) {
        List<PackInfo> packLocations = DynamXObjectLoaders.PACKS.findPackLocations(packName);
        if (packLocations.isEmpty()) {
            DynamXMain.log.error("Pack info " + packName + " not found. This should not happen.");
            return new DxModelPath(PackInfo.forAddon(packName).setPackType(ContentPackType.FOLDER), model);
        }
        return new DxModelPath(packLocations, model);
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
        resource.close();
        return out.toByteArray();
    }

    public static Vector3f toVector3f(Vec3d pos) {
        return Vector3fPool.get((float) pos.x, (float) pos.y, (float) pos.z);
    }

    public static Vector3f toVector3f(javax.vecmath.Vector3f pos) {
        return Vector3fPool.get(pos.x, pos.y, pos.z);
    }

    public static Vector3f toVector3f(BlockPos pos) {
        return Vector3fPool.get((float) pos.getX(), (float) pos.getY(), (float) pos.getZ());
    }

    public static Vector3f toVector3f(org.joml.Vector3f pos) {
        return Vector3fPool.get(pos.x, pos.y, pos.z);
    }

    public static org.joml.Vector3f toVector3f(Vector3f pos) {
        return new org.joml.Vector3f(pos.x, pos.y, pos.z);
    }

    public static org.joml.Vector3f toVector3f(float x, float y, float z) {
        return new org.joml.Vector3f(x, y, z);
    }


    public static Quaternionf toQuaternion(Quaternion quat) {
        return new Quaternionf(quat.getX(), quat.getY(), quat.getZ(), quat.getW());
    }

    public static Quaternionf toQuaternion(org.lwjgl.util.vector.Quaternion quat) {
        return new Quaternionf(quat.getX(), quat.getY(), quat.getZ(), quat.getW());
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

        return DynamXPhysicsHelper.castRay(DynamXContext.getPhysicsWorld(entity.world), eyePos, lookAt, ignoredPredicate);
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
        return entity.world.rayTraceBlocks(vec3d, vec3d2, true, false, true);
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
                if (wantedPart != null && !wantedPart.test(part)) {
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

    public static IntBuffer createIntBuffer(int[] data) {
        IntBuffer buffer = BufferUtils.createIntBuffer(data.length);
        buffer.put(data);
        buffer.flip();
        return buffer;
    }

    public static FloatBuffer createFloatBuffer(float[] data) {
        FloatBuffer buffer = BufferUtils.createFloatBuffer(data.length);
        buffer.put(data);
        buffer.flip();
        return buffer;
    }

    //DUPLICATE (function is already in the BasicsAddon)
    public static int getSpeed(BaseVehicleEntity<?> entity) {
        BasicEngineModule engine = entity.getModuleByType(BasicEngineModule.class);
        if (engine != null) {
            float[] ab = engine.getEngineProperties();
            if (ab == null) return 0;
            return (int) Math.abs(ab[VehicleEntityProperties.EnumEngineProperties.SPEED.ordinal()]);
        }
        return -1;
    }

    public static void attachTrailer(EntityPlayer player, BaseVehicleEntity<?> carEntity, BaseVehicleEntity<?> trailer) {
        Vector3fPool.openPool();
        Vector3f p1r = DynamXGeometry.rotateVectorByQuaternion(carEntity.getModuleByType(TrailerAttachModule.class).getAttachPoint(), carEntity.physicsRotation);
        Vector3f p2r = DynamXGeometry.rotateVectorByQuaternion(trailer.getModuleByType(TrailerAttachModule.class).getAttachPoint(), trailer.physicsRotation);
        if (p1r.addLocal(carEntity.physicsPosition).subtract(p2r.addLocal(trailer.physicsPosition)).lengthSquared() < 60) {
            if (carEntity.getJointsHandler() == null) {
                return;
            }
            EntityJointsHandler handler = carEntity.getJointsHandler();
            Collection<EntityJoint<?>> curJoints = handler.getJoints();
            TrailerEntity trailerIsAttached = null;
            for (EntityJoint<?> joint : curJoints) {
                if (joint.getEntity2() instanceof TrailerEntity) {
                    trailerIsAttached = (TrailerEntity) joint.getEntity2();
                    break;
                }
            }
            if (trailerIsAttached == null) {
                if (TrailerAttachModule.HANDLER.createJoint(carEntity, trailer, (byte) 0)) {
                    TextComponentTranslation msg = new TextComponentTranslation("trailer.attached", trailer.getPackInfo().getName(), carEntity.getPackInfo().getName());
                    msg.getStyle().setColor(TextFormatting.GREEN);
                    player.sendMessage(msg);
                    if (player.world.isRemote && trailer instanceof TrailerEntity)
                        ((TrailerEntity<?>) trailer).playAttachSound();
                } else {
                    TextComponentTranslation msg = new TextComponentTranslation("trailer.attach.fail", trailer.getPackInfo().getName(), carEntity.getPackInfo().getName());
                    msg.getStyle().setColor(TextFormatting.RED);
                    player.sendMessage(msg);
                }
            } else {
                carEntity.getJointsHandler().removeJointWith(trailerIsAttached, TrailerAttachModule.JOINT_NAME, (byte) 0);
                player.sendMessage(new TextComponentTranslation("trailer.detached"));
            }
        } else {
            player.sendMessage(new TextComponentTranslation("trailer.attach.toofar"));
        }
        Vector3fPool.closePool();
    }


    public static void hotswapWorldPackInfos(World w) {
        DynamXMain.log.info("Hot-swapping pack infos in models and spawn entities/tile entities in world " + w);
        for (Entity e : w.loadedEntityList) {
            if (e instanceof IPackInfoReloadListener)
                ((IPackInfoReloadListener) e).onPackInfosReloaded();
        }
        for (TileEntity te : w.loadedTileEntityList) {
            if (te instanceof IPackInfoReloadListener)
                ((IPackInfoReloadListener) te).onPackInfosReloaded();
        }
        if (w.isRemote)
            DynamXContext.getDxModelRegistry().onPackInfosReloaded();
    }

    /**
     * Shorthand to get a secured input stream able to read terrain data
     *
     * @param is The input stream to read
     * @return An ObjectInputStream that can only read primitives and terrain elements
     * @throws IOException If an I/O error occurs
     */
    public static ObjectInputStream getTerrainObjectsIS(InputStream is) throws IOException {
        Set<String> classesSet = Collections.unmodifiableSet(new HashSet(Arrays.asList(byte[].class.getName(), MutableBoundingBox.class.getName(), StairsBox.class.getName(), EnumFacing.class.getName(), Enum.class.getName())));
        return new ObjectInputStream(is) {
            @Override
            protected Class<?> resolveClass(ObjectStreamClass desc) throws IOException, ClassNotFoundException {
                if (!classesSet.contains(desc.getName())) {
                    throw new InvalidClassException("Unauthorized deserialization attempt", desc.getName());
                }
                return super.resolveClass(desc);
            }
        };
    }

    /**
     * Gets the position of the given object in the given 3D model
     *
     * @param modelData The 3D model
     * @param objectName The name of the object to get the pos of
     * @param allowPartCenter If true, the center of the object will be used as position for obj models (and the translation for gltf models) <br>
     *                        If false, the position can only be read from gltf models
     * @return The translation of the object, is this is a gltf model, or the center of the object if this is an obj model and allowPartCenter is true
     */
    @Nullable
    public static Vector3f readPartPosition(DxModelData modelData, String objectName, boolean allowPartCenter) {
        return readPartPosition(modelData, objectName, allowPartCenter, false);
    }

    /**
     * Gets the position of the given object in the given 3D model
     *
     * @param modelData The 3D model
     * @param objectName The name of the object to get the pos of
     * @param allowPartCenter If true, the center of the object will be used as position for obj models (and the translation for gltf models) <br>
     *                        If false, the position can only be read from gltf models
     * @param forceCenter If true, the center of the object will be returned for both obj and gltf models
     * @return The translation of the object, is this is a gltf model and forceCenter is false, or the center of the object if this is an obj model and allowPartCenter is true, or forceCenter is true
     */
    @Nullable
    public static Vector3f readPartPosition(DxModelData modelData, String objectName, boolean allowPartCenter, boolean forceCenter) {
        assert !forceCenter || allowPartCenter : "forceCenter is true but allowPartCenter is false";
        if (!modelData.getMeshNames().contains(objectName.toLowerCase()))
            return null;
        if (forceCenter || modelData.getFormat() == EnumDxModelFormats.OBJ) {
            return allowPartCenter ? modelData.getMeshCenter(objectName, new Vector3f()) : null;
        } else if (modelData.getFormat() == EnumDxModelFormats.GLTF) {
            NodeModel nodeModel = ((GltfModelData) modelData).getNodeModel(objectName);
            float[] trans = nodeModel.getTranslation();
            return trans == null ? null : new Vector3f(trans[0], trans[1], trans[2]);
        }
        return null;
    }

    /**
     * Gets the rotation of the given object in the given 3D model <br>
     * Note: This method only works for gltf models
     *
     * @param modelData The 3D model
     * @param objectName The name of the object to get the rotation of
     * @return The rotation of the object, or null if the model is not a gltf model or if the object has no rotation
     */
    @Nullable
    public static Quaternion readPartRotation(DxModelData modelData, String objectName) {
        if (modelData.getFormat() != EnumDxModelFormats.GLTF || !modelData.getMeshNames().contains(objectName.toLowerCase()))
            return null;
        NodeModel nodeModel = ((GltfModelData) modelData).getNodeModel(objectName);
        float[] rot = nodeModel.getRotation();
        if (rot != null) {
            return new Quaternion(rot[0], rot[1], rot[2], rot[3]);
        }
        return null;
    }

    /**
     * Gets the scale (size) of the given object in the given 3D model
     *
     * @param modelData The 3D model
     * @param objectName The name of the object to get the scale of
     * @return The scale of the object, or an empty vector if the object isn't found in the model
     */
    @Nonnull
    public static Vector3f readPartScale(DxModelData modelData, String objectName) {
        if (!modelData.getMeshNames().contains(objectName.toLowerCase()))
            return new Vector3f();
        return modelData.getMeshDimension(objectName, new Vector3f());
    }
}
