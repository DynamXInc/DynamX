package fr.hermes.api.mc.entities;

import fr.dynamx.core.utils.optimization.MutableBoundingBox;
import fr.hermes.api.mc.items.HmItemStack;
import fr.hermes.api.mc.world.HmServerWorld;
import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.NBTTagCompound;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector3i;

import java.util.Collection;
import java.util.UUID;

public interface HmEntity {
    int getEntityId();

    HmServerWorld getHmWorld();

    void readFromNbt(NBTTagCompound tag);

    void writeToNbt(NBTTagCompound tag);

    void setNoClip(boolean value);

    void setPreventEntitySpawning(boolean value);

    void setIgnoreFrustumCheck(boolean value);

    void setPosition(float x, float y, float z);

    void setRotationYaw(float yaw);

    double getPosX();

    double getPosY();

    double getPosZ();

    float getRotationYaw();

    float getPrevRotationYaw();

    float getRotationPitch();

    float getPrevRotationPitch();

    double getMotionX();

    double getMotionY();

    double getMotionZ();

    void setDead();

    void onSetDead();

    /**
     * Called in minecraft thread to update vanilla position and rotation fields, also used for render and updating "prev" fields
     */
    void updateMinecraftPos(Vector3f physicsPosition, Quaternionf physicsRotation);

    void onUpdate();

    MutableBoundingBox getHmBoundingBox();

    void onRemovedFromWorld();

    String getName();

    void writeSpawnData(ByteBuf buffer);

    void readSpawnData(ByteBuf additionalData);

    default void onAddPassenger(HmEntity passenger) {
    }

    default void onRemovePassenger(HmEntity passenger) {
    }

    default boolean updatePassenger(HmEntity passenger) {
        return false;
    }

    default boolean updatePassengerRotation(HmEntity passenger) {
        return false;
    }

    UUID getUniqueID();

    boolean isDead();

    float getDistanceSq(HmEntity entity);

    int getTicksExisted();

    Collection<HmEntity> getHmPassengers();

    default HmEntity getHmControllingPassenger() {
        return null;
    }

    HmItemStack getHmPickedResult();

    boolean canFitPassenger(HmEntity passenger);

    boolean isInRangeToRenderDist(double range);

    int getBrightnessForRender();

    Vector3f getHmLook();

    Vector3f getHmPosition();

    Vector3i getHmBlockPosition();

    float getEyeHeight();

    //TODO NEW TO IMPLEMENT

    int getChunkX();

    int getChunkY();

    int getChunkZ();

    void setMotionX(float motionX);

    void setMotionY(float motionY);

    void setMotionZ(float motionZ);

    double getPrevPosX();

    void setPrevPosX(double prevPosX);

    double getPrevPosY();

    void setPrevPosY(double prevPosY);

    double getPrevPosZ();

    void setPrevPosZ(double prevPosZ);

    boolean startRiding(HmEntity riddenEntity);

    boolean startRiding(HmEntity riddenEntity, boolean force);

    Vector3f getEyesPosition();

    boolean isRiding();
}
