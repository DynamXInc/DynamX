package fr.hermes.api.mc.utils;

import fr.hermes.api.mc.entities.HmEntity;
import net.minecraft.util.EnumFacing;
import org.joml.Vector3f;
import net.minecraft.util.math.BlockPos;

public interface HmRayTraceResult {
    enum Type {
        BLOCK,
        ENTITY,
        MISS
    }

    Type hm$getType();

    BlockPos hm$getBlockPos();

    HmEntity hm$getEntity();

    Vector3f hm$getHitVec();

    EnumFacing hm$getSide();
}
