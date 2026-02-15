package fr.hermes.api.mc.entities;

import fr.dynamx.core.utils.optimization.MutableBoundingBox;
import fr.hermes.api.mc.items.HmItemStack;
import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.NBTTagCompound;

public interface HmEntityLogic {
    default void onMcEntityInit() {
    }

    void readFromNbt(NBTTagCompound tag);

    void writeToNbt(NBTTagCompound tag);

    default void onSetDead() {
    }

    void onUpdate();

    default MutableBoundingBox getBoundingBox() {
        return null;
    }

    default void onRemovedFromWorld() {
    }

    default String getName() {
        return null;
    }

    void writeSpawnData(ByteBuf buffer);

    void readSpawnData(ByteBuf additionalData);

    default void onAddPassenger(HmEntity passenger) {
    }

    default void onRemovePassenger(HmEntity passenger) {
    }

    boolean updatePassenger(HmEntity passenger);

    boolean updatePassengerRotation(HmEntity passenger);

    default HmEntity getControllingPassenger() {
        return null;
    }

    default boolean canPassengerSteer() {
        return true;
    }

    default boolean shouldPassengersSit() {
        return true;
    }

    default HmItemStack getPickedResult() {
        return null;
    }

    default Boolean canFitPassenger(HmEntity passenger) {
        return null;
    }

    default Boolean isInRangeToRenderDist(double range) {
        return null;
    }

    default int getBrightnessForRender() {
        return -1;
    }
}
