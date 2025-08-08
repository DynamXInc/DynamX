package fr.dynamx.api.entities.modules;

import net.minecraft.nbt.NBTTagCompound;

/**
 * Common interface for all entity/tile entity modules, namely {@link IPhysicsModule} and {@link fr.dynamx.api.blocks.IBlockEntityModule}
 */
public interface IBaseModule {
    /**
     * Called when reading the entity/tile entity nbt
     */
    default void readFromNBT(NBTTagCompound tag) {
    }

    /**
     * Called when writing the entity/tile entity nbt
     */
    default void writeToNBT(NBTTagCompound tag) {
    }

    /**
     * Controls the init priority of this module <br>
     * The higher the priority, the earlier the module will be initialized <br>
     * This can be used to control module interdependencies <br>
     * Default is 0
     * @return The init priority of this module
     */
    default byte getInitPriority() {
        return 0;
    }
}
