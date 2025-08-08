package fr.dynamx.common.physics.terrain.element;

import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.math.Vector3f;
import fr.dynamx.api.physics.terrain.ITerrainElement;
import fr.dynamx.utils.VerticalChunkPos;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * An empty terrain element <br>
 * Helps to know that chunk collisions had been successfully loaded, but for an empty chunk
 */
public class EmptyTerrainElement implements ITerrainElement {
    @Nullable
    @Override
    public PhysicsRigidBody build(World world, Vector3f pos) {
        return null;
    }

    @Nullable
    @Override
    public PhysicsRigidBody getBody() {
        return null;
    }

    @Override
    public void save(TerrainSaveType type, ObjectOutputStream to) throws IOException {
    }

    @Override
    public boolean load(TerrainSaveType type, ObjectInputStream from, VerticalChunkPos pos) throws IOException, ClassNotFoundException {
        return true;
    }

    @Override
    public void addDebugToWorld(World mcWorld, Vector3f pos) {
    }

    @Override
    public void removeDebugFromWorld(World mcWorld) {
    }

    @Override
    public void clear() {
    }

    @Override
    public TerrainElementsFactory getFactory() {
        return TerrainElementsFactory.EMPTY;
    }
}
