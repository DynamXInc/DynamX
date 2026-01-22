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

public interface HmEntityLogic {
    void readFromNbt(NBTTagCompound tag);

    void writeToNbt(NBTTagCompound tag);

    void onSetDead();

    void onUpdate();

    MutableBoundingBox getBoundingBox();

    void onRemovedFromWorld();

    String getName();

    void writeSpawnData(ByteBuf buffer);

    void readSpawnData(ByteBuf additionalData);

    void onAddPassenger(HmEntity passenger);

    void onRemovePassenger(HmEntity passenger);

    boolean updatePassenger(HmEntity passenger);

    boolean updatePassengerRotation(HmEntity passenger);

    HmEntity getControllingPassenger();

    HmItemStack getPickedResult();

    boolean canFitPassenger(HmEntity passenger);

    boolean isInRangeToRenderDist(double range);

    int getBrightnessForRender();
}
