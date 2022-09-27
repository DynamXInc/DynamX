package fr.dynamx.api.physics.terrain;

import fr.dynamx.common.physics.terrain.computing.TerrainCollisionsCalculator;

import java.util.ArrayList;
import java.util.List;

/**
 * An helper class to use some features of the DynamX terrain
 *
 * @see IPhysicsTerrainLoader
 * @see IBlockCollisionBehavior
 */
public class DynamXTerrainApi {
    /**
     * Custom terrain loaders
     */
    private static final List<IPhysicsTerrainLoader> customTerrainLoaders = new ArrayList<>();

    /**
     * Adds a custom terrain loader that will be loaded by the {@link ITerrainManager}
     */
    public static void addCustomTerrainLoader(IPhysicsTerrainLoader loader) {
        customTerrainLoaders.add(loader);
    }

    /**
     * @return Custom terrain loaders
     */
    public static List<IPhysicsTerrainLoader> getCustomTerrainLoaders() {
        return customTerrainLoaders;
    }

    /**
     * Adds customs collisions to certain block. The first added have the bigger priority, so take care of incompatibilities !
     *
     * @param behavior The collisions behavior
     */
    public static void addCustomBlockBehavior(IBlockCollisionBehavior behavior) {
        TerrainCollisionsCalculator.addCustomBlockBehavior(behavior);
    }
}
