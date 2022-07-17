package fr.dynamx.utils.debug;

import java.util.ArrayList;
import java.util.List;

/**
 * All {@link DynamXDebugOption} <br>
 * Feel free to create yours
 */
public class DynamXDebugOptions {
    public static final DynamXDebugOption DEBUG_RENDER = DynamXDebugOption.newServerDependantOption(DebugCategories.GENERAL, "Debug renderer").withSubCategory("DynamX"),
            PROFILING = DynamXDebugOption.newServerDependantOption(DebugCategories.GENERAL, "Profiling").withDescription("Find why DynamX is lagging - prints timings in the logs. May produce lag").withSubCategory("DynamX"),
            PHYSICS_DEBUG = DynamXDebugOption.newOption(DebugCategories.GENERAL, "Bullet physics debug").withDescription("Shows complete rigid bodies and joints debug, may produce lag").withSubCategory("Physics"),
            PLAYER_TO_OBJECT_COLLISION_DEBUG = DynamXDebugOption.newOption(DebugCategories.GENERAL, "Player <-> DynamX Object collision debug").withDescription("Shows the collision boxes used for the player <-> DynamX Objects collisions").withSubCategory("Collisions"),
            DYNX_OBJECTS_COLLISION_DEBUG = DynamXDebugOption.newOption(DebugCategories.GENERAL, "DynamX Object collision debug").withDescription("Shows the collision boxes used in the physics engine for the Objects <-> Objects and Objects <-> Terrain collisions").withSubCategory("Collisions");

    public static final DynamXDebugOption.TerrainDebugOption CHUNK_BOXES = (DynamXDebugOption.TerrainDebugOption) DynamXDebugOption.newTerrainOption("Server chunk boxes", true, 1, 2, 4, 8).withSubCategory("Server/solo"),
            BLOCK_BOXES = (DynamXDebugOption.TerrainDebugOption) DynamXDebugOption.newTerrainOption("Server block boxes", true, 4, 1, 2, 8).withSubCategory("Server/solo"),
            SLOPE_BOXES = (DynamXDebugOption.TerrainDebugOption) DynamXDebugOption.newTerrainOption("Server slopes", true, 16, 32).withSubCategory("Server/solo"),
            CLIENT_CHUNK_BOXES = (DynamXDebugOption.TerrainDebugOption) DynamXDebugOption.newTerrainOption("Client chunk boxes", true, 2, 1, 4, 8).withDescription("Not available in solo").withSubCategory("Client"),
            CLIENT_BLOCK_BOXES = (DynamXDebugOption.TerrainDebugOption) DynamXDebugOption.newTerrainOption("Client block boxes", true, 8, 1, 2, 4).withDescription("Not available in solo").withSubCategory("Client"),
            CLIENT_SLOPE_BOXES = (DynamXDebugOption.TerrainDebugOption) DynamXDebugOption.newTerrainOption("Client slopes", true, 32, 16).withDescription("Not available in solo").withSubCategory("Client");

    public static final DynamXDebugOption CENTER_OF_MASS = DynamXDebugOption.newOption(DebugCategories.VEHICLES, "Center of mass").withSubCategory(VehicleDebugTypes.GENERAL.title),
            SEATS = DynamXDebugOption.newOption(DebugCategories.VEHICLES, "Seats").withSubCategory(VehicleDebugTypes.GENERAL.title),
            WHEELS = DynamXDebugOption.newOption(DebugCategories.VEHICLES, "Wheels").withSubCategory(VehicleDebugTypes.GENERAL.title),
            FRICTION_POINTS = DynamXDebugOption.newOption(DebugCategories.VEHICLES, "Force points").withSubCategory(VehicleDebugTypes.GENERAL.title),
            PLAYER_COLLISIONS = DynamXDebugOption.newOption(DebugCategories.VEHICLES, "Player collisions").withDescription("Debug for collisions between players and vehicles").withSubCategory(VehicleDebugTypes.COLLISIONS.title),
            TRAILER_ATTACH_POINTS = DynamXDebugOption.newOption(DebugCategories.VEHICLES, "Trailer attach points").withSubCategory(VehicleDebugTypes.ATTACH_POINTS.title),
            PROPS_CONTAINERS = DynamXDebugOption.newOption(DebugCategories.VEHICLES, "Props container boxes").withSubCategory(VehicleDebugTypes.OTHER.title),
            DOOR_ATTACH_POINTS = DynamXDebugOption.newOption(DebugCategories.VEHICLES, "Door attach points").withSubCategory(VehicleDebugTypes.ATTACH_POINTS.title),
            LATE_NETWORK = DynamXDebugOption.newOption(DebugCategories.VEHICLES, "Network sync").withDescription("Renders past server and client positions of the vehicle. Requires Full network debug.").withSubCategory(VehicleDebugTypes.OTHER.title),
            CAMERA_RAYCAST = DynamXDebugOption.newOption(DebugCategories.VEHICLES, "Camera").withDescription("Debug for the camera raycast").withSubCategory(VehicleDebugTypes.OTHER.title),
            FULL_NETWORK_DEBUG = DynamXDebugOption.newServerDependantOption(DebugCategories.VEHICLES, "Full network debug").withDescription("Enables network debug functions, may produce lag").withSubCategory(VehicleDebugTypes.OTHER.title),
            WHEEL_ADVANCED_DATA = DynamXDebugOption.newServerDependantOption(DebugCategories.VEHICLES, "Sync wheel advanced data").withDescription("WIP - Has no effects except more network usage").withSubCategory(VehicleDebugTypes.OTHER.title);

    /**
     * DynamX debug categories
     */
    public enum DebugCategories {
        GENERAL, TERRAIN, VEHICLES;

        private int state;
        private final List<DynamXDebugOption> options = new ArrayList<>();

        public void setState(int state) {
            this.state = state;
        }

        public int getState() {
            return state;
        }

        public DynamXDebugOption addOption(DynamXDebugOption option) {
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
        GENERAL("Main"), COLLISIONS("Collisions"), ATTACH_POINTS("Attach points"), OTHER("Other");

        public final String title;

        VehicleDebugTypes(String title) {
            this.title = title;
        }
    }
}
