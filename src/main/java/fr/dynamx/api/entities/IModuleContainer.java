package fr.dynamx.api.entities;

import fr.dynamx.common.entities.BaseVehicleEntity;
import fr.dynamx.common.entities.PackPhysicsEntity;
import fr.dynamx.common.entities.modules.DoorsModule;
import fr.dynamx.common.entities.modules.SeatsModule;

import javax.annotation.Nullable;

/**
 * All built-in possible modules <br>
 * The IHaveModule interfaces should be implemented by the {@link BaseVehicleEntity} in order to make the modules work
 *
 * @see fr.dynamx.api.entities.modules.IPhysicsModule
 */
public interface IModuleContainer {
    /**
     * Helper method to cast this IHaveModule to an entity
     */
    PackPhysicsEntity<?, ?> cast();

    interface ISeatsContainer extends IModuleContainer {
        @Nullable
        SeatsModule getSeats();

        /**
         * Can be false if the seats aren't yet loaded
         * @return true if the vehicle has seats and they are loaded
         */
        default boolean hasSeats() {
            return getSeats() != null;
        }
    }

    interface IDoorContainer extends IModuleContainer {
        @Nullable
        DoorsModule getDoors();
    }
}
