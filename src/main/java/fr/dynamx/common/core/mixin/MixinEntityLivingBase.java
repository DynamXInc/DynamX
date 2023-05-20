package fr.dynamx.common.core.mixin;

import fr.dynamx.common.core.DismountHelper;
import fr.dynamx.common.entities.BaseVehicleEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

/**
 * Patches the world raytrace to raytrace on dynamx blocks
 */
@Mixin(value = EntityLivingBase.class, remap = false)
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

    /**
     * @author Aym'
     * @reason Fix look when riding a vehicle
     */
    @Overwrite
    public Vec3d getLook(float partialTicks) {
        if (partialTicks == 1.0F)
        {
            return getLookVec();
        }
        else
        {
            float yaw = rotationYaw;
            float pitch = rotationPitch;
            float prevYaw = prevRotationYaw;
            float prevPitch = prevRotationPitch;
            if(getRidingEntity() instanceof BaseVehicleEntity) {
                Entity entity = getRidingEntity();
                yaw += entity.rotationYaw;
                pitch += entity.rotationPitch;
                yaw = yaw % 360;
                pitch = pitch % 360;

                prevYaw += entity.prevRotationYaw;
                prevPitch += entity.prevRotationPitch;
                prevYaw = prevYaw % 360;
                prevPitch = prevPitch % 360;
            }
            float f = prevPitch + (pitch - prevPitch) * partialTicks;
            float f1 = prevYaw + (yaw - prevYaw) * partialTicks;
            return this.getVectorForRotation(f, f1);
        }
    }
}
