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
}
