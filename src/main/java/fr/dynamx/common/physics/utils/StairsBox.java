package fr.dynamx.common.physics.utils;

import net.minecraft.util.EnumFacing;

import java.io.Serializable;

public class StairsBox implements Serializable
{
    private final float min, max;
    private final float minY, minOtherCoord;
    private final boolean inverted;
    private final EnumFacing facing;

    public StairsBox(float min, float max, float minY, float minOtherCoord, boolean inverted, EnumFacing facing) {
        this.min = min;
        this.max = max;
        this.minY = minY;
        this.minOtherCoord = minOtherCoord;
        this.inverted = inverted;
        this.facing = facing;
    }

    public float getMin() {
        return min;
    }

    public float getMax() {
        return max;
    }

    public boolean isInverted() {
        return inverted;
    }

    public EnumFacing getFacing() {
        return facing;
    }

    public float getMinOtherCoord() {
        return minOtherCoord;
    }

    public float getMinY() {
        return minY;
    }
}
