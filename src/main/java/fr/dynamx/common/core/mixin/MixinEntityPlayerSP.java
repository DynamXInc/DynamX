package fr.dynamx.common.core.mixin;

import com.mojang.authlib.GameProfile;
import fr.dynamx.common.core.DismountHelper;
import fr.dynamx.common.entities.BaseVehicleEntity;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

/**
 * Patches the world raytrace to raytrace on dynamx blocks
 */
@Mixin(value = EntityPlayerSP.class)
public abstract class MixinEntityPlayerSP extends AbstractClientPlayer {
    public MixinEntityPlayerSP(World worldIn, GameProfile playerProfile) {
        super(worldIn, playerProfile);
    }

    /**
     * @author Aym'
     * @reason Fix look when riding a vehicle
     */
    @Overwrite
    public Vec3d getLook(float partialTicks) {
        return getLookVec();
    }
}
