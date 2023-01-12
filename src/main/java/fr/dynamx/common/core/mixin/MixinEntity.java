package fr.dynamx.common.core.mixin;

import fr.dynamx.common.core.AABBCollisionHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.MoverType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

/**
 * Patches the world raytrace to raytrace on dynamx blocks
 */
@Mixin(value = Entity.class, priority = 800, remap = MixinChunk.REMAP)
public abstract class MixinEntity {
    /**
     * @author Aym'
     * @reason Because DynamX
     */
    @Overwrite
    public void move(MoverType type, double x, double y, double z) {
        AABBCollisionHandler.vanillaMove((Entity) (Object) this, type, x, y, z);
    }
}
