package fr.hermes.api.mc.entities;

import fr.dynamx.core.common.entities.SeatEntity;
import fr.dynamx.core.utils.optimization.MutableBoundingBox;
import fr.hermes.api.mc.world.HmWorld;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import org.joml.Vector3f;

import java.util.Collection;
import java.util.UUID;

public interface HmEntity {
    int hm$getEntityId();

    HmWorld hm$getWorld();

    void hm$setNoClip(boolean value);

    void hm$setNoGravity(boolean value);

    void hm$setPreventEntitySpawning(boolean value);

    void hm$setIgnoreFrustumCheck(boolean value);

    void hm$setPosition(float x, float y, float z);

    void hm$setRotationYaw(float yaw);

    double hm$getPosX();

    double hm$getPosY();

    double hm$getPosZ();

    float hm$getRotationYaw();

    float hm$getPrevRotationYaw();

    float hm$getRotationPitch();

    float hm$getPrevRotationPitch();

    double hm$getMotionX();

    double hm$getMotionY();

    double hm$getMotionZ();

    void hm$setDead();

    MutableBoundingBox hm$getBoundingBox();

    String hm$getName();

    UUID hm$getUniqueID();

    boolean hm$isDead();

    float hm$getDistanceSq(HmEntity entity);

    int hm$getTicksExisted();

    Collection<HmEntity> hm$getPassengers();

    default Vector3f hm$getLook() { return hm$getLook(0); }

    Vector3f hm$getLook(float partialTicks);

    Vector3f hm$getPosition();

    BlockPos hm$getBlockPosition();

    float hm$getEyeHeight();

    int hm$getChunkX();

    int hm$getChunkY();

    int hm$getChunkZ();

    void hm$setMotionX(double motionX);

    void hm$setMotionY(double motionY);

    void hm$setMotionZ(double motionZ);

    double hm$getPrevPosX();

    void hm$setPrevPosX(double prevPosX);

    double hm$getPrevPosY();

    void hm$setPrevPosY(double prevPosY);

    double hm$getPrevPosZ();

    void hm$setPrevPosZ(double prevPosZ);

    default Vector3f hm$getEyesPosition() { return hm$getEyesPosition(0); }

    Vector3f hm$getEyesPosition(float partialTicks);

    boolean hm$isRiding();

    // TODO Mixin implem
    boolean hm$isRidingOrBeingRiddenBy(HmEntity other);

    HmEntity hm$getRidingEntity();

    float hm$getRotationYawHead();

    void hm$setRotationYawHead(float yawHead);

    void hm$setRenderYawOffset(float offset);

    void hm$setPrevRotationYaw(float prevRotationYaw);

    void hm$setRotationPitch(float rotationPitch);

    void hm$setPrevRotationPitch(float prevRotationPitch);

    boolean hm$isInvisible();

    float hm$getLastTickPosX();

    float hm$getLastTickPosY();

    float hm$getLastTickPosZ();

    boolean hm$startRiding(HmEntity entity);

    void hm$setSize(float width, float height);

    boolean hm$isInWater();

    void hm$removePassengers();

    void hm$writeToNbt(NBTTagCompound tag);

    void hm$readFromNbt(NBTTagCompound tag);
}
