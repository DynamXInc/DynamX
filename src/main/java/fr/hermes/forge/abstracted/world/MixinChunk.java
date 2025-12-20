package fr.hermes.forge.abstracted.world;

import fr.dynamx.core.utils.DynamXConstants;
import fr.hermes.api.mc.entities.HmEntity;
import fr.hermes.api.mc.world.HmChunk;
import net.minecraft.entity.Entity;
import net.minecraft.util.ClassInheritanceMultiMap;
import net.minecraft.world.chunk.Chunk;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Set;

@Mixin(value = Chunk.class, remap = DynamXConstants.REMAP)
public abstract class MixinChunk implements HmChunk {
    @Shadow
    public abstract ClassInheritanceMultiMap<Entity>[] getEntityLists();

    @Shadow
    @Final
    public int x;

    @Shadow
    @Final
    public int z;

    @Override
    public Set<HmEntity>[] hm$getEntityLists() {
        return (Set[]) getEntityLists();
    }

    @Override
    public int hm$getX() {
        return x;
    }

    @Override
    public int hm$getZ() {
        return z;
    }
}
