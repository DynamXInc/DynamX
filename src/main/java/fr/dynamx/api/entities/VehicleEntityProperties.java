package fr.dynamx.api.entities;

import fr.dynamx.common.entities.BaseVehicleEntity;
import net.minecraftforge.common.util.EnumHelper;

/**
 * References to all {@link BaseVehicleEntity} basic properties, adds methods to add custom properties
 */
public class VehicleEntityProperties {
    /**
     * Float properties stored in all {@link fr.dynamx.common.entities.modules.WheelsModule} <br>
     * One wheel has all of these properties
     */
    public enum EnumVisualProperties {
        STEERANGLE(1),
        ROTATIONANGLE(1),
        SUSPENSIONLENGTH(0),
        COLLISIONX(0),
        COLLISIONY(0),
        COLLISIONZ(0);

        /**
         * Determines the interpolation type when synced <br>
         * 0 = linear interpolation, 1 = angular interpolation
         */
        public final byte type;

        EnumVisualProperties(int type) {
            this.type = (byte) type;
        }
    }

    /**
     * Float properties stored in all {@link fr.dynamx.common.entities.modules.WheelsModule}s <br>
     * One wheel has all of these properties <br>
     * Currently used for debug except for SKIDINFO
     */
    public enum EnumWheelProperties {
        SKIDINFO,
        FRICTION,
        PACEJKALONGITUDINAL,
        PACEJKALATERAL,
        ROTATIONDELTA
    }

    /**
     * Float properties stored in all {@link fr.dynamx.common.entities.modules.EngineModule}s <br>
     * Describes the engine state
     */
    public enum EnumEngineProperties {
        /**
         * Vehicle speed
         */
        SPEED,
        /**
         * Engine rpm
         */
        REVS,
        /**
         * The activer gear of the gearbox
         */
        ACTIVE_GEAR
    }

    /**
     * Returns the index of a wheel visual property in the visualProperties array, for {@link fr.dynamx.common.entities.modules.WheelsModule}
     *
     * @param partIndex            The wheel id
     * @param enumVisualProperties The visual property
     * @return The index of the visual property in the array containing all wheel's properties
     */
    public static int getPropertyIndex(int partIndex, EnumVisualProperties enumVisualProperties) {
        return EnumVisualProperties.values().length * partIndex + enumVisualProperties.ordinal();
    }

    /**
     * Returns a property from its index in the visualProperties array, not depending on the wheel, for {@link fr.dynamx.common.entities.modules.WheelsModule}
     *
     * @param index The property index in the visualProperties array
     * @return The property
     */
    public static EnumVisualProperties getPropertyByIndex(int index) {
        return EnumVisualProperties.values()[index % EnumVisualProperties.values().length];
    }

    /**
     * Returns the index of a wheel property in the wheelProperties array, for {@link fr.dynamx.common.entities.modules.WheelsModule}
     *
     * @param partIndex           The wheel id
     * @param enumWheelProperties The wheel property
     * @return The index of the property in the array containing all wheel's properties
     */
    public static int getPropertyIndex(int partIndex, EnumWheelProperties enumWheelProperties) {
        return EnumWheelProperties.values().length * partIndex + enumWheelProperties.ordinal();
    }

    /**
     * Adds a visual property, each wheel of each {@link fr.dynamx.common.entities.modules.WheelsModule} will contain this property, and it will be automatically synced over the network
     *
     * @param name              The property name, should be unique (add your modid)
     * @param interpolationType The interpolation type for sync : 0 = linear interpolation, 1 = angular interpolation
     * @return The property instance
     */
    public static EnumVisualProperties addVisualProperty(String name, byte interpolationType) {
        for (EnumVisualProperties prop : EnumVisualProperties.values()) {
            if (prop.name().equalsIgnoreCase(name))
                throw new IllegalArgumentException("Visual property with name " + name + " already exists !");
        }
        return EnumHelper.addEnum(EnumVisualProperties.class, name, new Class[]{byte.class}, interpolationType);
    }

    /**
     * Adds a wheel property, each wheel of each {@link fr.dynamx.common.entities.modules.WheelsModule} will contain this property, and it will be automatically synced over the network
     *
     * @param name The property name, should be unique (add your modid)
     * @return The property instance
     */
    public static EnumWheelProperties addWheelProperty(String name) {
        for (EnumWheelProperties prop : EnumWheelProperties.values()) {
            if (prop.name().equalsIgnoreCase(name))
                throw new IllegalArgumentException("Wheel property with name " + name + " already exists !");
        }
        return EnumHelper.addEnum(EnumWheelProperties.class, name, new Class[0]);
    }

    /**
     * Adds an engine property, each {@link fr.dynamx.common.entities.modules.EngineModule} will contain this property, and it will be automatically synced over the network
     *
     * @param name The property name, should be unique (add your modid)
     * @return The property instance
     */
    public static EnumEngineProperties addEngineProperty(String name) {
        for (EnumEngineProperties prop : EnumEngineProperties.values()) {
            if (prop.name().equalsIgnoreCase(name))
                throw new IllegalArgumentException("Engine property with name " + name + " already exists !");
        }
        return EnumHelper.addEnum(EnumEngineProperties.class, name, new Class[0]);
    }
}
