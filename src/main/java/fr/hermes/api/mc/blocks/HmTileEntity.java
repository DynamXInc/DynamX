package fr.hermes.api.mc.blocks;

import fr.hermes.api.mc.world.HmWorld;
import net.minecraft.util.math.BlockPos;

public interface HmTileEntity {
    HmWorld hm$getWorld();

    BlockPos hm$getPos();

    void hm$markBlockForRenderUpdate();

    boolean hm$isInvalid();
}
