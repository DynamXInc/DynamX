package fr.hermes.forge.abstracted.world;

import fr.dynamx.core.common.entities.PhysicsEntity;
import fr.dynamx.core.utils.DynamXConstants;
import fr.dynamx.core.utils.optimization.MutableBoundingBox;
import fr.hermes.api.mc.blocks.HmBlockEntity;
import fr.hermes.api.mc.blocks.HmBlockState;
import fr.hermes.api.mc.entities.HmEntity;
import fr.hermes.api.mc.entities.HmPlayerEntity;
import fr.hermes.api.mc.utils.HmParticleType;
import fr.hermes.api.mc.world.HmBiome;
import fr.hermes.api.mc.world.HmChunk;
import fr.hermes.api.mc.world.HmWorld;
import fr.hermes.api.mc.world.HmWorldSaveHandler;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.storage.ISaveHandler;
import org.joml.Vector3i;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.Random;

// TODO CHANGE REMAP TARGET
@Mixin(value = World.class, remap = DynamXConstants.REMAP)
public abstract class MixinWorld implements HmWorld {
    private final BlockPos.MutableBlockPos temporaryPos = new BlockPos.MutableBlockPos();

    protected BlockPos getTempPos(Vector3i pos) {
        temporaryPos.setPos(pos.x, pos.y, pos.z);
        return temporaryPos;
    }

    @Shadow
    @Final
    public boolean isRemote;

    @Shadow
    @Final
    public List<Entity> loadedEntityList;

    @Shadow
    @Nullable
    public abstract Entity getEntityByID(int id);

    @Shadow
    @Final
    public List<EntityPlayer> playerEntities;

    @Shadow
    @Final
    public Random rand;

    @Shadow
    public abstract int getHeight(int x, int z);

    @Shadow
    @Final
    public WorldProvider provider;

    @Shadow
    public abstract <T extends Entity> List<T> getEntitiesWithinAABB(Class<? extends T> classEntity, AxisAlignedBB bb);

    @Shadow
    @Nullable
    public abstract TileEntity getTileEntity(BlockPos pos);

    @Shadow
    public abstract Chunk getChunk(int chunkX, int chunkZ);

    @Shadow
    public abstract ISaveHandler getSaveHandler();

    @Shadow
    public abstract boolean isChunkGeneratedAt(int x, int z);

    @Shadow
    public abstract void spawnParticle(EnumParticleTypes particleType, double xCoord, double yCoord, double zCoord, double xSpeed, double ySpeed, double zSpeed, int... parameters);

    @Shadow
    public abstract boolean canBlockSeeSky(BlockPos pos);

    @Shadow
    public abstract boolean isRaining();

    @Shadow
    public abstract Biome getBiome(BlockPos pos);

    @Shadow
    public abstract IBlockState getBlockState(BlockPos pos);

    @Shadow
    public abstract boolean isAirBlock(BlockPos pos);

    @Shadow
    protected IChunkProvider chunkProvider;

    @Override
    public boolean hm$isClient() {
        return isRemote;
    }

    @Override
    public Collection<HmEntity> hm$getEntityList() {
        return (Collection) loadedEntityList;
    }

    @Override
    public HmEntity hm$getEntityByID(int entityId) {
        return (HmEntity) getEntityByID(entityId);
    }

    @Override
    public Collection<HmPlayerEntity> hm$getPlayerEntities() {
        return (Collection) playerEntities;
    }

    @Override
    public int hm$getHeight(int posX, int posZ) {
        return getHeight(posX, posZ);
    }

    @Override
    public Random hm$getRandom() {
        return rand;
    }

    @Override
    public boolean hm$isAirBlock(BlockPos pos) {
        return isAirBlock(pos);
    }

    @Override
    public HmBlockState hm$getBlockState(BlockPos pos) {
        return (HmBlockState) getBlockState(pos);
    }

    @Override
    public HmBiome hm$getBiome(BlockPos pos) {
        return (HmBiome) getBiome(pos);
    }

    @Override
    public boolean hm$isRaining() {
        return isRaining();
    }

    @Override
    public boolean hm$canBlockSeeSky(BlockPos pos) {
        return canBlockSeeSky(pos);
    }

    @Override
    public void hm$spawnParticle(HmParticleType skidParticle, float x, float y, float z, float speedX, float speedY, float speedZ) {
        EnumParticleTypes particleType = EnumParticleTypes.getParticleFromId(skidParticle.getId());
        spawnParticle(particleType, x, y, z, speedX, speedY, speedZ);
    }

    @Override
    public boolean hm$isChunkGeneratedAt(int x, int z) {
        return isChunkGeneratedAt(x, z);
    }

    @Override
    public abstract boolean hm$chunkExists(int x, int z);

    @Override
    public HmWorldSaveHandler hm$getSaveHandler() {
        return (HmWorldSaveHandler) getSaveHandler();
    }

    @Override
    public HmChunk hm$getChunk(int chunkX, int chunkZ) {
        return (HmChunk) getChunk(chunkX, chunkZ);
    }

    @Override
    public HmBlockEntity hm$getTileEntity(BlockPos pos) {
        return (HmBlockEntity) getTileEntity(pos);
    }

    @Override
    public List<PhysicsEntity<?>> hm$getPhysicsEntitiesWithinAABB(MutableBoundingBox aabb) {
        return (List) getEntitiesWithinAABB(PhysicsEntity.class, aabb.toBB());
    }

    @Override
    public int hm$getDimension() {
        return provider.getDimension();
    }
}
