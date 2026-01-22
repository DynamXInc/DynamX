package fr.hermes.api.mc.entities;

import fr.dynamx.core.utils.optimization.MutableBoundingBox;
import fr.hermes.api.mc.world.HmWorld;
import org.joml.Vector3f;
import org.joml.Vector3i;

import java.util.Collection;
import java.util.UUID;

public interface HmEntity {
    int hm$getEntityId();

    HmWorld hm$getWorld();

    void hm$setNoClip(boolean value);

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

    Vector3f hm$getLook();

    Vector3f hm$getPosition();

    Vector3i hm$getBlockPosition();

    float hm$getEyeHeight();

    int hm$getChunkX();

    int hm$getChunkY();

    int hm$getChunkZ();

    void hm$setMotionX(float motionX);

    void hm$setMotionY(float motionY);

    void hm$setMotionZ(float motionZ);

    double hm$getPrevPosX();

    void hm$setPrevPosX(double prevPosX);

    double hm$getPrevPosY();

    void hm$setPrevPosY(double prevPosY);

    double hm$getPrevPosZ();

    void hm$setPrevPosZ(double prevPosZ);

    Vector3f hm$getEyesPosition();

    boolean hm$isRiding();
}
