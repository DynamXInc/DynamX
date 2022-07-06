package fr.dynamx.common.physics.terrain.chunk;

import fr.dynamx.api.physics.IPhysicsWorld;
import fr.dynamx.utils.VerticalChunkPos;
import net.minecraft.world.World;

import java.util.UUID;

public class DebugChunkCollisions extends ChunkCollisions
{
    private final UUID id;

    public DebugChunkCollisions(World mcWorld, VerticalChunkPos pos, IPhysicsWorld world) {
        super(mcWorld, pos);
        id = UUID.randomUUID();
    }

    public UUID getId() {
        return id;
    }

    @Override
    public String toString() {
        return "DebugChunkCollisions[x="+ getPos().x+";y="+ getPos().y+";z="+ getPos().z+"] with id "+id;
    }
}
