package fr.hermes.forge.abstracted.entities;

import fr.dynamx.core.utils.DynamXConstants;
import fr.dynamx.core.utils.optimization.MutableBoundingBox;
import fr.dynamx.core.utils.optimization.Vector3fPool;
import fr.hermes.api.mc.entities.HmEntity;
import fr.hermes.api.mc.world.HmWorld;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.joml.Vector3f;
import org.joml.Vector3i;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

// TODO CHANGE REMAP TARGET
@Mixin(value = Entity.class, remap = DynamXConstants.REMAP)
public abstract class MixinEntity implements HmEntity {
    @Shadow
    private int entityId;

    @Shadow
    public World world;

    @Shadow
    public boolean noClip;

    @Shadow
    public boolean preventEntitySpawning;

    @Shadow
    public boolean ignoreFrustumCheck;

    @Shadow
    public abstract void setPosition(double x, double y, double z);

    @Shadow
    public float rotationYaw;

    @Shadow
    public double posX;

    @Shadow
    public double posY;

    @Shadow
    public double posZ;

    @Shadow
    public float prevRotationYaw;

    @Shadow
    public float rotationPitch;

    @Shadow
    public float prevRotationPitch;

    @Shadow
    public double motionX;

    @Shadow
    public double motionY;

    @Shadow
    public double motionZ;

    @Shadow
    public abstract void setDead();

    @Shadow
    public abstract AxisAlignedBB getEntityBoundingBox();

    @Shadow
    public abstract String getName();

    @Shadow
    public abstract UUID getUniqueID();

    @Shadow
    public boolean isDead;

    @Shadow
    public abstract double getDistanceSq(Entity entityIn);

    @Shadow
    public int ticksExisted;

    @Shadow
    public abstract List<Entity> getPassengers();

    @Shadow
    public abstract Vec3d getLook(float partialTicks);

    @Shadow
    public double prevPosZ;

    @Shadow
    public double prevPosY;

    @Shadow
    public double prevPosX;

    @Shadow
    public abstract boolean isRiding();

    @Shadow
    public abstract Vec3d getPositionEyes(float partialTicks);

    @Shadow
    public int chunkCoordZ;

    @Shadow
    public int chunkCoordY;

    @Shadow
    public int chunkCoordX;

    @Shadow
    public abstract float getEyeHeight();

    @Shadow
    public abstract BlockPos getPosition();

    @Shadow
    public float fallDistance;

    @Override
    public int hm$getEntityId() {
        return entityId;
    }

    @Override
    public HmWorld hm$getWorld() {
        return (HmWorld) world;
    }

    @Override
    public void hm$setNoClip(boolean value) {
        noClip = value;
    }

    @Override
    public void hm$setPreventEntitySpawning(boolean value) {
        preventEntitySpawning = value;
    }

    @Override
    public void hm$setIgnoreFrustumCheck(boolean value) {
        ignoreFrustumCheck = value;
    }

    @Override
    public void hm$setPosition(float x, float y, float z) {
        setPosition(x, y, z);
    }

    @Override
    public void hm$setRotationYaw(float yaw) {
        rotationYaw = yaw;
    }

    @Override
    public double hm$getPosX() {
        return posX;
    }

    @Override
    public double hm$getPosY() {
        return posY;
    }

    @Override
    public double hm$getPosZ() {
        return posZ;
    }

    @Override
    public float hm$getRotationYaw() {
        return rotationYaw;
    }

    @Override
    public float hm$getPrevRotationYaw() {
        return prevRotationYaw;
    }

    @Override
    public float hm$getRotationPitch() {
        return rotationPitch;
    }

    @Override
    public float hm$getPrevRotationPitch() {
        return prevRotationPitch;
    }

    @Override
    public double hm$getMotionX() {
        return motionX;
    }

    @Override
    public double hm$getMotionY() {
        return motionY;
    }

    @Override
    public double hm$getMotionZ() {
        return motionZ;
    }

    @Override
    public void hm$setDead() {
        setDead();
    }

    @Override
    public AxisAlignedBB hm$getBoundingBox() {
        return getEntityBoundingBox();
    }

    @Override
    public String hm$getName() {
        return getName();
    }

    @Override
    public UUID hm$getUniqueID() {
        return getUniqueID();
    }

    @Override
    public boolean hm$isDead() {
        return isDead;
    }

    @Override
    public float hm$getDistanceSq(HmEntity entity) {
        return (float) getDistanceSq((Entity) entity);
    }

    @Override
    public int hm$getTicksExisted() {
        return ticksExisted;
    }

    @Override
    public Collection<HmEntity> hm$getPassengers() {
        return (Collection)getPassengers();
    }

    @Override
    public Vector3f hm$getLook(float partialTicks) {
        Vec3d look = getLook(partialTicks);
        return Vector3fPool.get(look.x, look.y, look.z);
    }

    @Override
    public Vector3f hm$getPosition() {
        return Vector3fPool.get(posX, posY, posZ);
    }

    @Override
    public BlockPos hm$getBlockPosition() {
        return getPosition();
    }

    @Override
    public float hm$getEyeHeight() {
        return getEyeHeight();
    }

    @Override
    public int hm$getChunkX() {
        return chunkCoordX;
    }

    @Override
    public int hm$getChunkY() {
        return chunkCoordY;
    }

    @Override
    public int hm$getChunkZ() {
        return chunkCoordZ;
    }

    @Override
    public void hm$setMotionX(double motionX) {
        this.motionX = motionX;
    }

    @Override
    public void hm$setMotionY(double motionY) {
        this.motionY = motionY;
    }

    @Override
    public void hm$setMotionZ(double motionZ) {
        this.motionZ = motionZ;
    }

    @Override
    public double hm$getPrevPosX() {
        return prevPosX;
    }

    @Override
    public void hm$setPrevPosX(double prevPosX) {
        this.prevPosX = prevPosX;
    }

    @Override
    public double hm$getPrevPosY() {
        return prevPosY;
    }

    @Override
    public void hm$setPrevPosY(double prevPosY) {
        this.prevPosY = prevPosY;
    }

    @Override
    public double hm$getPrevPosZ() {
        return prevPosZ;
    }

    @Override
    public void hm$setPrevPosZ(double prevPosZ) {
        this.prevPosZ = prevPosZ;
    }

    @Override
    public Vector3f hm$getEyesPosition(float partialTicks) {
        Vec3d eyes = getPositionEyes(partialTicks);
        return Vector3fPool.get(eyes.x, eyes.y, eyes.z);
    }

    @Override
    public boolean hm$isRiding() {
        return isRiding();
    }

    @Override
    public float hm$getFallDistance() {
        return fallDistance;
    }
}
