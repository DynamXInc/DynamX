package fr.dynamx.utils.debug;

import fr.dynamx.common.DynamXMain;
import fr.dynamx.common.entities.PhysicsEntity;
import fr.dynamx.server.command.CmdNetworkConfig;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SyncTracker {
    private static final Map<String, List<String>> changes = new HashMap<>();

    public static void addChange(String syncName, String varName) {
        if (CmdNetworkConfig.TRACK_SYNC) {
            if (!changes.containsKey(syncName))
                changes.put(syncName, new ArrayList<>());
            changes.get(syncName).add(varName);
        }
    }

    public static void printAndClean(PhysicsEntity<?> syncedEntity) {
        if (CmdNetworkConfig.TRACK_SYNC && !changes.isEmpty()) {
            DynamXMain.log.info("==== Sync of " + syncedEntity.getEntityId() + " ====");
            changes.forEach((n, d) -> {
                DynamXMain.log.info("SyncVar " + n + " : " + d);
            });
            changes.clear();
        }
    }

    public static float EPS = 0.00035f;

    public static boolean different(float f1, float f2) {
        return different(f1, f2, EPS);
    }

    public static boolean different(float f1, float f2, float epsilon) {
        return Math.abs(f1 - f2) > epsilon;
    }
}
