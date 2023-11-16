package fr.dynamx.utils.debug;

import java.util.ArrayList;
import java.util.List;

/**
 * All {@link DynamXDebugOption} <br>
 * Feel free to create yours
 */
public class DynamXDebugOptions {

    private static final List<DynamXDebugOption> ALL_OPTIONS = new ArrayList<>();
    public static final DynamXDebugOption DEBUG_RENDER = DynamXDebugOption.newServerDependantOption(DebugCategories.GENERAL, "Enable debug renderer").withSubCategory("DynamX"),
            PROFILING = DynamXDebugOption.newServerDependantOption(DebugCategories.HOME, "Profiling").withDescription("Find why DynamX is lagging - prints timings in the logs. May produce lag.").withSubCategory("DynamX debug"),
            PHYSICS_DEBUG = DynamXDebugOption.newOption(DebugCategories.GENERAL, "Bullet shape debug").withDescription("Shows complete rigid bodies and joints debug, may produce lag.").withSubCategory("Physics"),
            RENDER_WIREFRAME = DynamXDebugOption.newOption(DebugCategories.GENERAL, "Should render wireframe").withDescription("Shows the bullet shape debug in wireframe.").withSubCategory("Physics").enable(),
            PLAYER_TO_OBJECT_COLLISION_DEBUG = DynamXDebugOption.newOption(DebugCategories.GENERAL, "Player <-> DynamX collisions").withDescription("Shows the collision boxes used for the player <-> DynamX objects collisions.").withSubCategory("Collisions");

    public static final DynamXDebugOption.TerrainDebugOption BLOCK_BOXES = (DynamXDebugOption.TerrainDebugOption) DynamXDebugOption.newTerrainOption("Server block boxes", true, 4, 1, 2, 8).withDescription("Not available in solo").withSubCategory("Server"),
            SLOPE_BOXES = (DynamXDebugOption.TerrainDebugOption) DynamXDebugOption.newTerrainOption("Server slopes", true, 16, 32).withDescription("Not available in solo").withSubCategory("Server"),
            CLIENT_BLOCK_BOXES = (DynamXDebugOption.TerrainDebugOption) DynamXDebugOption.newTerrainOption("Client block boxes", true, 8, 1, 2, 4).withSubCategory("Client"),
            CLIENT_SLOPE_BOXES = (DynamXDebugOption.TerrainDebugOption) DynamXDebugOption.newTerrainOption("Client slopes", true, 32, 16).withSubCategory("Client");

    public static final DynamXDebugOption CENTER_OF_MASS = DynamXDebugOption.newOption(DebugCategories.VEHICLES, "Center of mass").withSubCategory(VehicleDebugTypes.GENERAL.title),
            SEATS_AND_STORAGE = DynamXDebugOption.newOption(DebugCategories.VEHICLES, "Seats and storage").withSubCategory(VehicleDebugTypes.GENERAL.title),
            WHEELS = DynamXDebugOption.newOption(DebugCategories.VEHICLES, "Wheels and floats").withSubCategory(VehicleDebugTypes.GENERAL.title),
            ROTORS = DynamXDebugOption.newOption(DebugCategories.VEHICLES, "Rotors").withSubCategory(VehicleDebugTypes.GENERAL.title),
            HANDLES = DynamXDebugOption.newOption(DebugCategories.VEHICLES, "Handles").withSubCategory(VehicleDebugTypes.GENERAL.title),
            FRICTION_POINTS = DynamXDebugOption.newOption(DebugCategories.VEHICLES, "Force points").withSubCategory(VehicleDebugTypes.GENERAL.title),
            PLAYER_COLLISIONS = DynamXDebugOption.newOption(DebugCategories.GENERAL, "Advanced player collisions").withDescription("Debug for collisions between players and vehicles").withSubCategory(VehicleDebugTypes.COLLISIONS.title),
            TRAILER_ATTACH_POINTS = DynamXDebugOption.newOption(DebugCategories.VEHICLES, "Trailer attach points").withSubCategory(VehicleDebugTypes.ATTACH_POINTS.title),
            DOOR_ATTACH_POINTS = DynamXDebugOption.newOption(DebugCategories.VEHICLES, "Door attach points").withSubCategory(VehicleDebugTypes.ATTACH_POINTS.title),
            //LATE_NETWORK = DynamXDebugOption.newOption(DebugCategories.VEHICLES, "Network sync").withDescription("Renders past server and client positions of the vehicle. Requires Full network debug.").withSubCategory(VehicleDebugTypes.OTHER.title),
            PROPS_CONTAINERS = DynamXDebugOption.newOption(DebugCategories.VEHICLES, "Props container boxes").withSubCategory(VehicleDebugTypes.OTHER.title),
            CAMERA_RAYCAST = DynamXDebugOption.newOption(DebugCategories.VEHICLES, "Camera").withDescription("Debug for the camera raycast").withSubCategory(VehicleDebugTypes.OTHER.title),
            FULL_NETWORK_DEBUG = DynamXDebugOption.newServerDependantOption(DebugCategories.VEHICLES, "Full network debug").withDescription("Enables network debug functions, may produce lag").withSubCategory(VehicleDebugTypes.OTHER.title),
            WHEEL_ADVANCED_DATA = DynamXDebugOption.newServerDependantOption(DebugCategories.VEHICLES, "Sync wheel advanced data").withDescription("WIP - Has no effects except more network usage").withSubCategory(VehicleDebugTypes.OTHER.title);


    public static List<DynamXDebugOption> getAllOptions() {
        return ALL_OPTIONS;
    }

    /**
     * DynamX debug categories
     */
    public enum DebugCategories {
        HOME, GENERAL, TERRAIN, VEHICLES;

        private int state;
        private final List<DynamXDebugOption> options = new ArrayList<>();

        public void setState(int state) {
            this.state = state;
        }

        public int getState() {
            return state;
        }

        public DynamXDebugOption addOption(DynamXDebugOption option) {
            ALL_OPTIONS.add(option);
            options.add(option);
            return option;
        }

        public int getOptionCount() {
            return options.size();
        }

        public List<DynamXDebugOption> getOptions() {
            return options;
        }
    }

    public enum VehicleDebugTypes {
        GENERAL("Vehicles"), COLLISIONS("Collisions"), ATTACH_POINTS("Attach points"), OTHER("Other");

        public final String title;

        VehicleDebugTypes(String title) {
            this.title = title;
        }
    }
}
