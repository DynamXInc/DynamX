package fr.dynamx.api.physics.terrain;

import fr.dynamx.common.physics.terrain.computing.TerrainCollisionsCalculator;
import lombok.Getter;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

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
     * -- GETTER --
     *
     * @return Custom terrain loaders
     */
    @Getter
    private static final List<IPhysicsTerrainLoader> customTerrainLoaders = new ArrayList<>();

    /**
     * Adds a custom terrain loader that will be loaded by the {@link ITerrainManager}
     */
    public static void addCustomTerrainLoader(IPhysicsTerrainLoader loader) {
        customTerrainLoaders.add(loader);
    }

    /**
     * Adds customs collisions to certain block. The first added have the bigger priority, so take care of incompatibilities !
     *
     * @param behavior The collisions behavior
     */
    public static void addCustomBlockBehavior(IBlockCollisionBehavior behavior) {
        TerrainCollisionsCalculator.addCustomBlockBehavior(behavior);
    }

    /**
     * Custom terrain update behaviors
     * -- GETTER --
     *
     * @return Custom terrain update behaviors
     */
    @Getter
    private static final List<ITerrainUpdateBehavior> customUpdateBehaviors = new ArrayList<>();
    private static final ITerrainUpdateBehavior defaultUpdateBehavior = new ITerrainUpdateBehavior.DefaultUpdateBehavior();

    /**
     * Adds a custom terrain update behavior allowing you to ignore certain block updates
     *
     * @param behavior The update behavior to add
     */
    public static void addCustomUpdateBehavior(ITerrainUpdateBehavior behavior) {
        customUpdateBehaviors.add(behavior);
    }

    public static ITerrainUpdateBehavior.Result getTerrainUpdateBehavior(IBlockAccess world, BlockPos pos, IBlockState oldState, IBlockState newState) {
        if(!customUpdateBehaviors.isEmpty()) {
            for(ITerrainUpdateBehavior behavior : customUpdateBehaviors) {
                ITerrainUpdateBehavior.Result result = behavior.getResult(world, pos, oldState, newState);
                if(result != ITerrainUpdateBehavior.Result.CONTINUE) {
                    return result;
                }
            }
        }
        return defaultUpdateBehavior.getResult(world, pos, oldState, newState);
    }
}
