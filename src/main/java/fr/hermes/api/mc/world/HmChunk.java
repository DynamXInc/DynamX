package fr.hermes.api.mc.world;

import fr.dynamx.core.common.capability.DynamXChunkData;
import fr.hermes.api.mc.entities.HmEntity;
import net.minecraftforge.common.capabilities.Capability;

import java.util.Set;

public interface HmChunk {
    HmWorld hm$getWorld();

    Set<HmEntity>[] hm$getEntityLists();

    int hm$getX();

    int hm$getZ();

    // TODO good luck with capabilities
    DynamXChunkData getCapability(Capability<DynamXChunkData> dynamxChunkDataCapability);
}
