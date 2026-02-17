package fr.hermes.api.mc.world;

import fr.dynamx.core.common.entities.PhysicsEntity;
import fr.dynamx.core.utils.optimization.MutableBoundingBox;
import fr.hermes.api.mc.blocks.HmBlockState;
import fr.hermes.api.mc.blocks.HmBlockEntity;
import fr.hermes.api.mc.entities.HmEntity;
import fr.hermes.api.mc.entities.HmEntityFactory;
import fr.hermes.api.mc.entities.HmPlayerEntity;
import fr.hermes.api.mc.utils.HmParticleType;
import fr.hermes.api.mc.utils.HmRayTraceResult;
import net.minecraft.util.math.BlockPos;
import org.joml.Vector3f;

import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.function.Consumer;

public interface HmWorld {
    boolean hm$isClient();

    Collection<HmEntity> hm$getEntityList();

    // FIXME for Physics entities, ensure this returns the actual physic entity, not the wrapper of any kind
    HmEntity hm$getEntityByID(int entityId);

    Collection<HmPlayerEntity> hm$getPlayerEntities();

    int hm$getHeight(int posX, int posZ);

    Random hm$getRandom();

    boolean hm$isAirBlock(BlockPos pos);

    HmBlockState hm$getBlockState(BlockPos blockPos);

    HmBiome hm$getBiome(BlockPos pos);

    boolean hm$isRaining();

    boolean hm$canBlockSeeSky(BlockPos pos);

    void hm$spawnParticle(HmParticleType skidParticle, float x, float y, float z, float speedX, float speedY, float speedZ);

    boolean hm$isChunkGeneratedAt(int x, int z);

    boolean hm$chunkExists(int x, int z);

    HmWorldSaveHandler hm$getSaveHandler();

    HmChunk hm$getChunk(int chunkX, int chunkZ);

    HmBlockEntity hm$getTileEntity(BlockPos pos);

    Collection<HmBlockEntity> hm$getBlockEntityList();

    List<PhysicsEntity<?>> hm$getPhysicsEntitiesWithinAABB(MutableBoundingBox aabb);

    int hm$getDimension();

    void hm$addEntityRemovedListener(Consumer<HmEntity> callback);

    boolean hm$isBlockLoaded(Vector3f vector3f);

    int hm$getLightAt(BlockPos pos);

    void hm$setBlockToAir(BlockPos blockPos);

    HmEntity spawnHmEntity(HmEntityFactory entity, Vector3f pos);

    // args for the World method: false, true, false
    HmRayTraceResult hm$rayTraceBlocks(com.jme3.math.Vector3f start, com.jme3.math.Vector3f end);

    HmEntity hm$getEntityByUuid(UUID uuid);
}
