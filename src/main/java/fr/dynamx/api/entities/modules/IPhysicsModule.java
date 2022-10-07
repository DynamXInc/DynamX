package fr.dynamx.api.entities.modules;

import fr.dynamx.api.network.sync.SimulationHolder;
import fr.dynamx.client.renders.RenderPhysicsEntity;
import fr.dynamx.common.entities.ModularPhysicsEntity;
import fr.dynamx.common.physics.entities.AbstractEntityPhysicsHandler;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;

/**
 * Base implementation of a {@link ModularPhysicsEntity} module <br>
 * There is 3 built-in module types, but you can create others <br>
 * <h2>Supported modules :</h2>
 * - {@link ISeatsModule} <br>
 * - {@link IPropulsionModule} <br>
 * - {@link IEngineModule}
 */
public interface IPhysicsModule<P extends AbstractEntityPhysicsHandler<?, ?>> {
    /**
     * Called when reading the entity nbt
     */
    default void readFromNBT(NBTTagCompound tag) {
    }

    /**
     * Called when writing the entity nbt
     */
    default void writeToNBT(NBTTagCompound tag) {
    }

    /**
     * Called when a passenger is added to the entity
     */
    default void addPassenger(Entity passenger) {
    }

    /**
     * Called when a passenger is removed from the entity
     */
    default void removePassenger(Entity passenger) {
    }

    /**
     * Called when the {@link AbstractEntityPhysicsHandler} of the entity is created <br>
     * Use this function to create the physics handler of your module, if required <br>
     * This is called even when the entity doesn't use physics, like the server entity in single player
     *
     * @param handler The physics handler of the entity, or null if physics are not simulated on this side
     */
    default void initPhysicsEntity(@Nullable P handler) {
    }

    /**
     * Called when the entity pack info was just loaded
     */
    default void initEntityProperties() {
    }

    /**
     * Called when the entity is set dead <br>
     * <strong>Use onRemovedFromWorld to remove physics objects !</strong>
     */
    default void onSetDead() {
    }

    /**
     * Called when the entity is removed from the world (death, chunk unload...)
     */
    default void onRemovedFromWorld() {
    }

    /**
     * If this module has a controller (an {@link IVehicleController} listening for key inputs), then it
     * should return a new controller for this module <br>
     * Called when a driver mounts <br>
     * Client side only
     *
     * @return the controller to use until the driver dismounts, or null
     */
    @Nullable
    @SideOnly(Side.CLIENT)
    default IVehicleController createNewController() {
        return null;
    }

    /**
     * Adds the {@link fr.dynamx.api.network.sync.SynchronizedVariable} used to synchronize this module <br>
     * The variables must only be added on the side which has the authority over the data (typically the server) <br>
     * Fired on modules initialization and on {@link fr.dynamx.api.network.sync.SimulationHolder} changes
     * todo doc
     *  @param side             The current side
     *
     * @param simulationHolder The new holder of the simulation of the entity (see {@link SimulationHolder})
     */
    default void addSynchronizedVariables(Side side, SimulationHolder simulationHolder) {
    }

    /**
     * Fired when the {@link SimulationHolder} of this entity changes
     *
     * @param simulationHolder The new {@link SimulationHolder}
     * @param changeContext    The context of this update
     */
    default void onSetSimulationHolder(SimulationHolder simulationHolder, SimulationHolder.UpdateContext changeContext) {
    }

    /**
     * Implement this on you module to listen entity updates
     */
    interface IEntityUpdateListener {
        /**
         * @return True to listen this update on this side (default is true on all sides)
         */
        default boolean listenEntityUpdates(Side side) {
            return true;
        }

        /**
         * Called when updating the entity
         */
        default void updateEntity() {
        }
    }

    /**
     * Implement this on you module to listen entity vanilla and render pos updates <br>
     * This function permits to memorize prev values of your positions for render interpolation
     */
    interface IEntityPosUpdateListener {
        /**
         * @return True to listen this update on this side (default is true on client side)
         */
        default boolean listenEntityPosUpdates(Side side) {
            return side.isClient();
        }

        /**
         * Called when updating the entity vanilla and render pos
         */
        default void updateEntityPos() {
        }
    }

    /**
     * Implement this on you module to listen entity physics updates
     */
    interface IPhysicsUpdateListener {
        /**
         * Called before ticking the physics world (can be in an external thread) <br>
         * Here you can give the "input" to the physics world, i.e. your controls, your forces, etc
         *
         * @param simulatingPhysics If physics should be simulated in this update <br> If false, the physics handler may be null
         */
        default void preUpdatePhysics(boolean simulatingPhysics) {
        }

        /**
         * Called after ticking the physics world (can be in an external thread) <br>
         * Here you can get the results of your "input" : the new position, the new rotation, etc
         *
         * @param simulatingPhysics If physics should be simulated in this update <br> If false, the physics handler may be null
         */
        default void postUpdatePhysics(boolean simulatingPhysics) {
        }
    }

    interface IDrawableModule<T extends ModularPhysicsEntity<?>> {
        /**
         * Called to update textures of this module (egg for wheels) according to the new entity's metadata
         *
         * @param metadata The metadata of the entity
         * @param entity   The info of the entity, giving textures corresponding to the metadata
         */
        @SideOnly(Side.CLIENT)
        default void handleTextureID(byte metadata, T entity) {
        }

        @SideOnly(Side.CLIENT)
        void drawParts(RenderPhysicsEntity<?> render, float partialTicks, T entity);
    }
}