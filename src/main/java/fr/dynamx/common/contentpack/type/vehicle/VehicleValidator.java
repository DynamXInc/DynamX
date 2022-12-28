package fr.dynamx.common.contentpack.type.vehicle;

import fr.aym.acslib.api.services.error.ErrorLevel;
import fr.dynamx.common.contentpack.parts.PartWheel;
import fr.dynamx.utils.errors.DynamXErrorManager;

public interface VehicleValidator {
    void validate(ModularVehicleInfo info);

    VehicleValidator CAR_VALIDATOR = info -> {
        EngineInfo engine = info.getSubPropertyByType(EngineInfo.class);
        if (engine == null) //This will prevent any crash when spawning the vehicle
            throw new IllegalArgumentException("Car " + info.getFullName() + " has no engine");
        if (engine.getEngineSounds() == null)
            DynamXErrorManager.addPackError(info.getPackName(), "config_error", ErrorLevel.FATAL, info.getName(), "This car has no sounds !");
        if (info.getPartsByType(PartWheel.class).isEmpty())
            DynamXErrorManager.addPackError(info.getPackName(), "config_error", ErrorLevel.FATAL, info.getName(), "This car has no wheels !");
    };
    VehicleValidator TRAILER_VALIDATOR = info -> {
        if (info.getPartsByType(PartWheel.class).isEmpty())
            DynamXErrorManager.addPackError(info.getPackName(), "config_error", ErrorLevel.FATAL, info.getName(), "This trailer has no wheels !");
        if (info.getSubPropertyByType(TrailerAttachInfo.class) == null)
            DynamXErrorManager.addPackError(info.getPackName(), "config_error", ErrorLevel.FATAL, info.getName(), "Missing trailer config !");
    };
    VehicleValidator BOAT_VALIDATOR = info -> {
        EngineInfo engine = info.getSubPropertyByType(EngineInfo.class);
        if (engine == null)
            throw new IllegalArgumentException("Boat " + info.getFullName() + " has no engine");
    };
}
