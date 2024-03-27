package fr.dynamx.common.core.mixin;

import fr.dynamx.common.core.DismountHelper;
import fr.dynamx.common.entities.BaseVehicleEntity;
import fr.dynamx.utils.DynamXConstants;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

/**
 * Patches the world raytrace to raytrace on dynamx blocks
 */
@Mixin(value = EntityLivingBase.class, remap = DynamXConstants.REMAP)
public abstract class MixinEntityLivingBase extends Entity {
    public MixinEntityLivingBase(World worldIn) {
        super(worldIn);
    }

    /**
     * @author Aym'
     * @reason Because DynamX
     */
    @Overwrite
    public void dismountEntity(Entity entityIn) {
        DismountHelper.preDismount((EntityLivingBase) (Object) this, entityIn);
    }
}
