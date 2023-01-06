package fr.dynamx.common.contentpack.type.vehicle;

import fr.aym.acslib.api.services.error.ErrorLevel;
import fr.dynamx.common.contentpack.parts.PartRotor;
import fr.dynamx.common.contentpack.parts.PartWheel;
import fr.dynamx.utils.errors.DynamXErrorManager;

import java.util.List;

public interface VehicleValidator {
    void validate(ModularVehicleInfo info);

    VehicleValidator CAR_VALIDATOR = info -> {
        CarEngineInfo engine = info.getSubPropertyByType(CarEngineInfo.class);
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
        CarEngineInfo engine = info.getSubPropertyByType(CarEngineInfo.class);
        if (engine == null)
            throw new IllegalArgumentException("Boat " + info.getFullName() + " has no engine");
    };
    VehicleValidator HELICOPTER_VALIDATOR = info -> {
        HelicopterPhysicsInfo physicsInfo = info.getSubPropertyByType(HelicopterPhysicsInfo.class);
        if (physicsInfo == null)
            throw new IllegalArgumentException("Boat " + info.getFullName() + " has no HelicopterPhysics");
        List<PartRotor> rotors = info.getPartsByType(PartRotor.class);
        if (rotors.isEmpty())
            throw new IllegalArgumentException("Helicopter " + info.getFullName() + " has no rotors");
    };
}
