package fr.hermes.api.mc;

import org.joml.Vector3i;

import java.util.Collection;
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

    HmBiome getBiome(Vector3f pos);

    boolean isRaining();

    boolean canBlockSeeSky(Vector3f pos);

    void spawnParticle(HmParticleType skidParticle, float x, float y, float z, float speedX, float speedY, float speedZ);

    boolean isChunkGeneratedAt(int x, int z);

    boolean chunkExists(int x, int z);

    HmWorldSaveHandler getSaveHandler();

    HmChunk getChunk(int chunkX, int chunkZ);

    HmTileEntity getTileEntity(Vector3i pos);
}
