package fr.dynamx.api.physics.terrain;

import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.math.Vector3f;
import fr.dynamx.common.physics.terrain.chunk.ChunkCollisions;
import fr.dynamx.common.physics.terrain.element.TerrainElementsFactory;
import fr.dynamx.utils.VerticalChunkPos;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * A TerrainElement is a collision element used by {@link ChunkCollisions} that can be saved in files, then reloaded without computing the collisions again <br>
 * <strong>NOTE : all TerrainElements must have an empty constructor, used when loading them from a file</strong>
 *
 * @see fr.dynamx.common.physics.terrain.element.CompoundBoxTerrainElement
 * @see IPersistentTerrainElement
 */
public interface ITerrainElement {
    /**
     * Called once, after element init (in constructor), or after load method was called <br>
     * Should return the PhysicsRigidBody corresponding to this collision element
     *
     * @param pos The location of the body to create
     * @return A new rigid body for this terrain element
     */
    PhysicsRigidBody build(Vector3f pos);

    /**
     * Called after build has been called, should return the same body
     */
    @Nonnull
    PhysicsRigidBody getBody();

    /**
     * Saves this element into "to"
     *
     * @param type The type of the save, modifying used optimizations
     */
    void save(TerrainSaveType type, ObjectOutputStream to) throws IOException;

    /**
     * Populates this element with collision data, read from "from"
     *
     * @param type The type of the save, modifying used optimizations
     * @param from Data input stream
     * @param pos  Collision position, useful to restore rigid body position
     * @return False to cancel element loading
     */
    boolean load(TerrainSaveType type, ObjectInputStream from, VerticalChunkPos pos) throws IOException, ClassNotFoundException;

    /**
     * Called when this element is loaded into bullet, useful to enable visual debug
     *
     * @param pos The pos where the body is added
     */
    void addDebugToWorld(World mcWorld, Vector3f pos);

    /**
     * Called when this element is removed from bullet, useful to disable visual debug
     */
    void removeDebugFromWorld(World mcWorld);

    /**
     * Clears data contained in this element, freeing some memory <br>
     * The TerrainElement is not reused after this
     */
    void clear();

    TerrainElementsFactory getFactory();

    /**
     * The target of a TerrainElement save <br>
     * <ul>
     * <li>Disk is a local save of the terrain, where all optimizations can be used </li>
     * <li>Network is a save shared between different computers, platform-dependant optimizations (as MeshCollisionShape bvh serialization cannot be used)</li>
     * </ul>
     */
    enum TerrainSaveType {
        DISK, NETWORK;

        public boolean usesPlatformDependantOptimizations() {
            return this == DISK;
        }
    }

    int[] DEFAULT_SIZE = new int[]{16, 16, 16};

    /**
     * @return The max size used by this terrain element, in each dimension
     */
    default int[] getMaxSize() {
        return DEFAULT_SIZE;
    }

    /**
     * The element will not be deleted when chunk is invalidated
     *
     * @see fr.dynamx.common.physics.terrain.element.CustomSlopeTerrainElement
     */
    interface IPersistentTerrainElement extends ITerrainElement {
    }
}
