package fr.dynamx.common.network.sync.vars;

import fr.dynamx.common.entities.RagdollEntity;
import fr.dynamx.utils.DynamXConstants;
import net.minecraft.util.ResourceLocation;

public class RagdollPartsSynchronizedVariable extends AttachedBodySynchronizedVariable<RagdollEntity> {
    //public static final ResourceLocation NAME = new ResourceLocation(DynamXConstants.ID, "attach/ragdoll");

    @Override
    public AttachedBodySynchronizer getSynchronizer(RagdollEntity on) {
        return on;
    }
}
