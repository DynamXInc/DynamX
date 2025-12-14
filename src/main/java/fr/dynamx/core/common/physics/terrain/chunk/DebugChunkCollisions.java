package fr.dynamx.core.common.physics.terrain.chunk;

import fr.dynamx.core.utils.VerticalChunkPos;
import fr.hermes.api.mc.HmWorld;

import java.util.UUID;

public class DebugChunkCollisions extends ChunkCollisions {
    private final UUID id;

    public DebugChunkCollisions(HmWorld mcWorld, VerticalChunkPos pos) {
        super(mcWorld, pos);
        id = UUID.randomUUID();
    }

    public UUID getId() {
        return id;
    }

    @Override
    public String toString() {
        return "DebugChunkCollisions[x=" + getPos().x + ";y=" + getPos().y + ";z=" + getPos().z + "] with id " + id + " and state " + getChunkState();
    }
}
