package fr.dynamx.utils;

import fr.aym.acslib.ACsLib;
import fr.aym.acslib.api.services.StatsReportingService;
import net.minecraftforge.common.config.Configuration;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class DynamXConfig
{
    public static boolean syncPacks;

    public static boolean useUdp = true;
    public static int udpPort = 25575;
    public static boolean usingProxy = false, udpDebug = false;

    public static int vehiclesSyncTickRate = 1;
    public static int mountedVehiclesSyncTickRate = 1;
    public static int propsSyncTickRate = 2;

    public static int maxZoomOut = 20;
    public static int gearChangeDelay = 5;
    public static int blockCollisionRadius = 3;
    public static int maxComplexBlockBoxes = 8;

    public static int networkChunkComputeWarnTime = 40;

    public static boolean allowPlayersToMoveObjects = true;
    public static int[] allowedWrenchModes;

    public static List<VerticalChunkPos> chunkDebugPoses;
    public static boolean enableDebugTerrainManager, ignoreDangerousTerrainErrors;

    public static int ragdollSpawnMinForce;

    public static boolean disableSSLCertification;

    public static void load(File file) {
        Configuration cfg = new Configuration(file);
        cfg.load();
        syncPacks = cfg.getBoolean("SyncContentPacks", "Multiplayer", false, "If enabled, the server will send all content pack objects to the clients (only where there are differences)");
        allowedWrenchModes = cfg.get("Multiplayer", "AllowedWrenchModes", new int[]{0, 2, 5}).getIntList();
        useUdp = cfg.getBoolean("UseUdpServer", "UDP", true, "True to use (faster) UDP networking, false to use vanilla networking (TCP)");
        udpPort = cfg.getInt("UdpPort", "UDP", 25575, 2000, 65535, "A port for the udp server, if enabled");
        usingProxy = cfg.getBoolean("HasProxy", "UDP", false, "If you have a proxy in front of your server");
        udpDebug = cfg.getBoolean("PrintUdpDebug", "UDP", false, "True to print debug for UDP connections");
        maxZoomOut = cfg.getInt("MaxZoomOut", "Visuals", 20, 0, 200, "Max de-zoom in F5 view");
        allowPlayersToMoveObjects = cfg.getBoolean("AllowPlayersToMoveObjects", "Physics", true, "Allow player in survival to move ");
        ragdollSpawnMinForce = cfg.getInt("RagdollSpawnMinForce", "Physics", -1, -1, Integer.MAX_VALUE, "The minimum force of collision to spawn player ragdolls. Set to -1 to disable it.");
        //todo update doc and see new impact on perfs
        blockCollisionRadius = cfg.getInt("BlockCollisionRadius2", "Physics", 3, 0, 16, "The radius of collision checking with DynamX blocks around players. Has an impact on game performance. NOTE : Renamed with a '2' to replace the old default value and stay below 30 for stable performance");
        maxComplexBlockBoxes = cfg.getInt("MaxComplexBoxes", "Physics", 8, 0, 100, "The amount of detailed collisions per each complex block. If the block has more collisions (e.g. Decocraft), it will be a cube containing all collisions. Has an impact on game performance.");

        if (cfg.hasKey("Statistics", "CollectData")) {
            if (!cfg.getBoolean("CollectData", "Statistics", true, "Enables automatic reporting of your computer info (GPU, memory, OS) and useful crash-reports"))
                ACsLib.getPlatform().provideService(StatsReportingService.class).disable();
        } else {
            cfg.getBoolean("CollectData", "Statistics", true, "Enables automatic reporting of your computer info (GPU, memory, OS) and useful crash-reports");
            /*DynamXErrorTracker.addError(ErrorType.INIT, "Data collection notice", "Improving DynamX :", "We collect data about your computer (GPU, memory, OS) \n" +
                    "and crash-reports to improve DynamX. \n" +
                    "You can disable this in the configuration file of DynamX \n " +
                    "(under 'config' directory)", DynamXErrorTracker.ErrorLevel.ADVICE);*/
        }
        int[] x, y, z;
        x = cfg.get("Debug", "ChunkDebugX", new int[3], "X poses of chunks to debug", Integer.MIN_VALUE, Integer.MAX_VALUE).getIntList();
        y = cfg.get("Debug", "ChunkDebugY", new int[3], "Y poses of chunks to debug", Integer.MIN_VALUE, Integer.MAX_VALUE).getIntList();
        z = cfg.get("Debug", "ChunkDebugZ", new int[3], "Z poses of chunks to debug", Integer.MIN_VALUE, Integer.MAX_VALUE).getIntList();
        int s = x.length;
        if (s != y.length || s != z.length)
            throw new IllegalArgumentException("Invalid chunk debug config, wrong config of positions");
        chunkDebugPoses = new ArrayList<>();
        for (int i = 0; i < s; i++) {
            chunkDebugPoses.add(new VerticalChunkPos(x[i], y[i], z[i]));
        }

        enableDebugTerrainManager = cfg.get("Debug", "UseDebugTerrainManager", false, "Permits to debug terrain loading issues but may produce lag and instabilities").getBoolean();
        ignoreDangerousTerrainErrors = cfg.get("Debug", "IgnoreDangerousTerrainErrors", false, "Will try to prevent the game from crashing when there is a weird error in the terrain. Only enable this if you want server stability.").getBoolean();
        disableSSLCertification = cfg.get("Debug", "DisableSSLVerification", false, "Disables ssl certificates for dynamx.fr, may be a security breach for your computer, DO NOT disable it if you don't know what you are doing").getBoolean();
        cfg.save();

        System.out.println("CONFIG IS " + useUdp + " " + udpDebug);
    }
}
