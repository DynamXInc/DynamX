package fr.hermes.api.mc.world;

import fr.hermes.api.mc.entities.HmEntity;

public interface HmChunk {
    Iterable<HmEntity>[] getEntityLists();

    int getX();

    int getZ();
}
