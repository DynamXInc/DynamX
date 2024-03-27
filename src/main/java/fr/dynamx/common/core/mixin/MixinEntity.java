package fr.dynamx.common.core.mixin;

import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.entities.BaseVehicleEntity;
import fr.dynamx.utils.DynamXConstants;
import fr.dynamx.utils.debug.Profiler;
import fr.dynamx.utils.optimization.Vector3fPool;
import net.minecraft.entity.Entity;
import net.minecraft.entity.MoverType;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.Vec3d;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Patches the world raytrace to raytrace on dynamx blocks
 */
@Mixin(value = Entity.class, priority = 800, remap = DynamXConstants.REMAP)
public abstract class MixinEntity {

    @Shadow public float prevRotationYaw;
    @Shadow public float prevRotationPitch;

    @Shadow
    public abstract void setEntityBoundingBox(AxisAlignedBB bb);

    @Shadow
    public abstract AxisAlignedBB getEntityBoundingBox();

    @Shadow private Entity ridingEntity;
    @Shadow public float rotationPitch;
    @Shadow public float rotationYaw;

    @Shadow protected abstract Vec3d getVectorForRotation(float pitch, float yaw);

    private double x1, y1, z1;

    /**
     * @author Aym'
     * @reason Collisions with DynamX entities and blocks
     */
    @Inject(method = "move",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/entity/Entity;getEntityBoundingBox()Lnet/minecraft/util/math/AxisAlignedBB;",
                    shift = At.Shift.AFTER, ordinal = 5))
    private void move(MoverType type, double x, double y, double z, CallbackInfo ci) {
        AxisAlignedBB axisalignedbb = getEntityBoundingBox();
        Vector3fPool.openPool();
        Profiler.get().start(Profiler.Profiles.ENTITY_COLLISION);
        double[] data = DynamXContext.getCollisionHandler().handleCollisionWithBulletEntities((Entity) (Object) this, x, y, z);
        Profiler.get().end(Profiler.Profiles.ENTITY_COLLISION);
        Vector3fPool.closePool();
        x1 = data[0];
        y1 = data[1];
        z1 = data[2];
        setEntityBoundingBox(axisalignedbb.offset(x1, y1, z1));
    }

    @ModifyVariable(method = "move", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/entity/Entity;getEntityBoundingBox()Lnet/minecraft/util/math/AxisAlignedBB;",
            shift = At.Shift.AFTER, ordinal = 5), argsOnly = true, ordinal = 0)
    private double injectX1(double x) {
        return 0;
    }

    @ModifyVariable(method = "move", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/entity/Entity;getEntityBoundingBox()Lnet/minecraft/util/math/AxisAlignedBB;",
            shift = At.Shift.AFTER, ordinal = 5), argsOnly = true, ordinal = 1)
    private double injectY1(double y) {
        return 0;
    }

    @ModifyVariable(method = "move", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/entity/Entity;getEntityBoundingBox()Lnet/minecraft/util/math/AxisAlignedBB;",
            shift = At.Shift.AFTER, ordinal = 5), argsOnly = true, ordinal = 2)
    private double injectZ1(double z) {
        return 0;
    }

    @ModifyVariable(method = "move", at = @At(value = "FIELD",
            target = "Lnet/minecraft/entity/Entity;onGround:Z",
            opcode = Opcodes.GETFIELD, ordinal = 1), argsOnly = true, ordinal = 0)
    private double injectX(double x) {
        return x1;
    }

    @ModifyVariable(method = "move", at = @At(value = "FIELD",
            target = "Lnet/minecraft/entity/Entity;onGround:Z",
            opcode = Opcodes.GETFIELD, ordinal = 1), argsOnly = true, ordinal = 1)
    private double injectY(double y) {
        return y1;
    }

    @ModifyVariable(method = "move",
            at = @At(value = "FIELD", target = "Lnet/minecraft/entity/Entity;onGround:Z",
                    opcode = Opcodes.GETFIELD, ordinal = 1), argsOnly = true, ordinal = 2)
    private double injectZ(double z) {
        return z1;
    }

    /*@Redirect(method = "move", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/Entity;onGround:Z", opcode = Opcodes.GETFIELD, ordinal = 1))
    private boolean injected(Entity instance) {
        //System.out.println("RetIS = " + (onGround && !DynamXContext.getCollisionHandler().motionHasChanged()));
        return onGround && !DynamXContext.getCollisionHandler().motionHasChanged();
    }*/
    @ModifyVariable(method = "move", at = @At("STORE"), ordinal = 0)
    private boolean fixFlag(boolean flag) {
        return flag && !DynamXContext.getCollisionHandler().motionHasChanged();
    }
}
