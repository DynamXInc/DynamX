package fr.dynamx.api.entities;

import fr.dynamx.api.entities.modules.IEngineModule;
import fr.dynamx.api.entities.modules.IPropulsionModule;
import fr.dynamx.api.entities.modules.ISeatsModule;
import fr.dynamx.common.entities.BaseVehicleEntity;
import fr.dynamx.common.entities.modules.DoorsModule;

import javax.annotation.Nonnull;
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
    BaseVehicleEntity<?> cast();

    interface IEngineContainer extends IModuleContainer {
        @Nonnull
        IEngineModule<?> getEngine();
    }

    interface IPropulsionContainer<T extends IPropulsionModule<?>> extends IModuleContainer {
        @Nonnull
        T getPropulsion();
    }

    interface ISeatsContainer extends IModuleContainer {
        @Nonnull
        ISeatsModule getSeats();
    }

    interface IDoorContainer extends IModuleContainer {
        @Nullable
        DoorsModule getDoors();
    }
}
