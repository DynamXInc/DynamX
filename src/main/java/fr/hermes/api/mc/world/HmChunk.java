package fr.hermes.api.mc.world;

import fr.hermes.api.mc.entities.HmEntity;

import java.util.Set;

public interface HmChunk {
    Set<HmEntity>[] hm$getEntityLists();

    int hm$getX();

    int hm$getZ();
}
