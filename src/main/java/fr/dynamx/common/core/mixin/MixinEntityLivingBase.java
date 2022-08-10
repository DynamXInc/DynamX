package fr.dynamx.common.core.mixin;

import fr.dynamx.common.core.DismountHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

/**
 * Patches the world raytrace to raytrace on dynamx blocks
 */
@Mixin(EntityLivingBase.class)
public abstract class MixinEntityLivingBase {
    /**
     * @author Aym'
     * @reason Because DynamX
     */
    @Overwrite
    public void dismountEntity(Entity entityIn) {
        DismountHelper.preDismount((EntityLivingBase) (Object) this, entityIn);
    }
}
