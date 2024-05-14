package fr.dynamx.common.contentpack.type.vehicle;

import fr.aym.acslib.api.services.error.ErrorLevel;
import fr.dynamx.common.contentpack.parts.PartRotor;
import fr.dynamx.common.contentpack.parts.PartWheel;
import fr.dynamx.common.items.DynamXItemSpawner;
import fr.dynamx.common.items.vehicle.*;
import fr.dynamx.utils.errors.DynamXErrorManager;

import java.util.List;

public interface VehicleValidator {
    default void initProperties(ModularVehicleInfo info) {
    }

    DynamXItemSpawner<ModularVehicleInfo> getSpawnItem(ModularVehicleInfo info);

    void validate(ModularVehicleInfo info);

    default Class<? extends BaseEngineInfo> getEngineClass() {
        return CarEngineInfo.class;
    }

    VehicleValidator CAR_VALIDATOR = new VehicleValidator() {
        @Override
        public DynamXItemSpawner<ModularVehicleInfo> getSpawnItem(ModularVehicleInfo info) {
            return ItemCar.getItemForCar(info);
        }

        @Override
        public void validate(ModularVehicleInfo info) {
            CarEngineInfo engine = info.getSubPropertyByType(CarEngineInfo.class);
            if (engine == null) //This will prevent any crash when spawning the vehicle
                throw new IllegalArgumentException("Car " + info.getFullName() + " has no engine");
            if (engine.getEngineSounds() == null)
                DynamXErrorManager.addPackError(info.getPackName(), "config_error", ErrorLevel.FATAL, info.getName(), "This car has no sounds !");
            if (info.getPartsByType(PartWheel.class).isEmpty())
                DynamXErrorManager.addPackError(info.getPackName(), "config_error", ErrorLevel.FATAL, info.getName(), "This car has no wheels !");
        }
    };
    VehicleValidator TRAILER_VALIDATOR = new VehicleValidator() {
        @Override
        public DynamXItemSpawner<ModularVehicleInfo> getSpawnItem(ModularVehicleInfo info) {
            return new ItemTrailer(info);
        }

        @Override
        public void validate(ModularVehicleInfo info) {
            if (info.getPartsByType(PartWheel.class).isEmpty())
                DynamXErrorManager.addPackError(info.getPackName(), "config_error", ErrorLevel.FATAL, info.getName(), "This trailer has no wheels !");
            if (info.getSubPropertyByType(TrailerAttachInfo.class) == null)
                DynamXErrorManager.addPackError(info.getPackName(), "config_error", ErrorLevel.FATAL, info.getName(), "Missing trailer config !");
        }
    };
    VehicleValidator BOAT_VALIDATOR = new VehicleValidator() {
        @Override
        public void initProperties(ModularVehicleInfo info) {
            info.angularDamping = 0.5f;
        }

        @Override
        public DynamXItemSpawner<ModularVehicleInfo> getSpawnItem(ModularVehicleInfo info) {
            return new ItemBoat(info);
        }

        @Override
        public void validate(ModularVehicleInfo info) {
            BoatEngineInfo engine = info.getSubPropertyByType(BoatEngineInfo.class);
            if (engine != null && engine.getEngineSounds() == null) {
                DynamXErrorManager.addPackError(info.getPackName(), "config_error", ErrorLevel.FATAL, info.getName(), "The boat engine has no sounds !");
            }
            if (info.getSubPropertyByType(BoatPropellerInfo.class) == null)
                throw new IllegalArgumentException("Boat " + info.getFullName() + " has no propeller");
        }

        @Override
        public Class<? extends BaseEngineInfo> getEngineClass() {
            return BoatEngineInfo.class;
        }
    };
    VehicleValidator HELICOPTER_VALIDATOR = new VehicleValidator() {
        @Override
        public void initProperties(ModularVehicleInfo info) {
            info.linearDamping = 0.5f;
            info.angularDamping = 0.9f;
            info.inWaterAngularDamping = 0.9f;
        }

        @Override
        public DynamXItemSpawner<ModularVehicleInfo> getSpawnItem(ModularVehicleInfo info) {
            return new ItemHelicopter(info);
        }

        @Override
        public void validate(ModularVehicleInfo info) {
            HelicopterPhysicsInfo physicsInfo = info.getSubPropertyByType(HelicopterPhysicsInfo.class);
            if (physicsInfo == null)
                throw new IllegalArgumentException("Helicopter " + info.getFullName() + " has no HelicopterPhysics");
            BaseEngineInfo engine = info.getSubPropertyByType(BaseEngineInfo.class);
            if (engine == null)
                throw new IllegalArgumentException("Helicopter " + info.getFullName() + " has no engine");
            List<PartRotor> rotors = info.getPartsByType(PartRotor.class);
            if (rotors.isEmpty())
                throw new IllegalArgumentException("Helicopter " + info.getFullName() + " has no rotors");
        }

        @Override
        public Class<? extends BaseEngineInfo> getEngineClass() {
            return BaseEngineInfo.class;
        }
    };
    VehicleValidator PLANE_VALIDATOR = new VehicleValidator() {
        @Override
        public void initProperties(ModularVehicleInfo info) {
            info.linearDamping = 0.5f;
            info.angularDamping = 0.9f;
            info.inWaterAngularDamping = 0.9f;
        }

        @Override
        public DynamXItemSpawner<ModularVehicleInfo> getSpawnItem(ModularVehicleInfo info) {
            return new ItemPlane(info);
        }

        @Override
        public void validate(ModularVehicleInfo info) {
            HelicopterPhysicsInfo physicsInfo = info.getSubPropertyByType(HelicopterPhysicsInfo.class);
            if (physicsInfo == null)
                throw new IllegalArgumentException("Plane " + info.getFullName() + " has no HelicopterPhysics");
            BaseEngineInfo engine = info.getSubPropertyByType(BaseEngineInfo.class);
            if (engine == null)
                throw new IllegalArgumentException("Plane " + info.getFullName() + " has no engine");
        }

        @Override
        public Class<? extends BaseEngineInfo> getEngineClass() {
            return BaseEngineInfo.class;
        }
    };
}
