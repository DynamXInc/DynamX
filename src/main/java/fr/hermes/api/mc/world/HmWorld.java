package fr.hermes.api.mc.world;

import fr.dynamx.core.utils.optimization.MutableBoundingBox;
import fr.hermes.api.mc.*;
import fr.hermes.api.mc.blocks.HmBlockState;
import fr.hermes.api.mc.entities.HmEntity;
import fr.hermes.api.mc.entities.HmPlayerEntity;
import fr.hermes.api.mc.utils.HmParticleType;
import org.joml.Vector3i;

import java.util.Collection;
import java.util.List;
import java.util.Random;

public interface HmWorld {
    boolean isClient();

    Collection<HmEntity> getEntityList();

    // FIXME for Physics entities, ensure this returns the actual physic entity, not the wrapper of any kind
    HmEntity getEntityByID(int entityId);

    Collection<HmPlayerEntity> getPlayerEntities();

    int getHeight(int posX, int posZ);

    Random getRandom();

    boolean isAirBlock(int x, int y, int z);

    HmBlockState getBlockState(Vector3i blockPos);

    HmBiome getBiome(Vector3i pos);

    boolean isRaining();

    boolean canBlockSeeSky(Vector3i pos);

    void spawnParticle(HmParticleType skidParticle, float x, float y, float z, float speedX, float speedY, float speedZ);

    boolean isChunkGeneratedAt(int x, int z);

    boolean chunkExists(int x, int z);

    HmWorldSaveHandler getSaveHandler();

    HmChunk getChunk(int chunkX, int chunkZ);

    HmTileEntity getTileEntity(Vector3i pos);

    <T extends HmEntity> List<T> getEntitiesWithinAABB(Class<T> clazz, MutableBoundingBox aabb);

    int getDimension();
}
