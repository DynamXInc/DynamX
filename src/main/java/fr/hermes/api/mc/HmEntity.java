package fr.hermes.api.mc;

import fr.dynamx.core.utils.optimization.MutableBoundingBox;
import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.NBTTagCompound;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public interface HmEntity {
    int getEntityId();

    HmServerWorld getWorld();

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

    float getRotationPitch();

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

    MutableBoundingBox getBoundingBox();

    void onRemovedFromWorld();

    String getName();

    void writeSpawnData(ByteBuf buffer);

    void readSpawnData(ByteBuf additionalData);
}
