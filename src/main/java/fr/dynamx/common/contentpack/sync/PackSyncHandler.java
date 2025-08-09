package fr.dynamx.common.contentpack.sync;

import fr.dynamx.api.contentpack.object.INamedObject;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.DynamXMain;
import fr.dynamx.common.contentpack.DynamXObjectLoaders;
import fr.dynamx.common.contentpack.loader.PackFilePropertyData;
import fr.dynamx.common.contentpack.loader.SubInfoTypeAnnotationCache;
import fr.dynamx.utils.DynamXConfig;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/**
 * Responsible to keep same object properties on client and server side, if the SyncPacks config option is enabled
 */
public class PackSyncHandler {
    private static final Map<String, Map<String, byte[]>> objects = new HashMap<>();

    public static void computeAll() {
        if (!DynamXConfig.syncPacks) {
            return;
        }
        DynamXMain.log.debug("[PackSync] Computing pack files hash.");
        objects.clear();
        PackSyncHandler harsher = new PackSyncHandler();
        DynamXObjectLoaders.getInfoLoaders().forEach((i) -> {
            Map<String, byte[]> objs = new HashMap<>();
            i.hashObjects(harsher, objs);
            objects.put(i.getPrefix(), objs);
        });
    }

    public static void requestPackSync() {
        if (!DynamXConfig.syncPacks) {
            DynamXMain.log.debug("[PackSync] Not enabled.");
            return;
        }
        computeAll();
        DynamXMain.log.debug("[PackSync] Requesting pack sync...");
        DynamXContext.getNetwork().sendToServer(new MessagePacksHashs(objects));
    }

    public byte[] hash(INamedObject object) {
        StringBuilder sdata = new StringBuilder();
        Map<String, PackFilePropertyData<?>> data = SubInfoTypeAnnotationCache.getOrLoadData(object.getClass());
        data.forEach((n, p) ->
        {
            try {
                p.getField().setAccessible(true);
                Object e = p.getField().get(object);
                if (e != null)
                    sdata.append(p.getType().toValue(e));
                p.getField().setAccessible(false);
            } catch (Exception e) {
                throw new RuntimeException("Cannot hash  " + object.getFullName() + " : failed on " + n, e);
            }
        });
        return getHash(sdata.toString());
    }

    private byte[] getHash(String password) {
        MessageDigest digest = null;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e1) {
            e1.printStackTrace();
        }
        digest.reset();
        return digest.digest(password.getBytes());
    }

    public static List<String> getDelta(Map<String, byte[]> objs1, Map<String, byte[]> objs2) {
        List<String> delta = new ArrayList<>();
        objs1.forEach((s, k) -> {
            if (!objs2.containsKey(s))
                delta.add("-" + s);
            else if (!Arrays.equals(k, objs2.get(s)))
                delta.add("*" + s);
        });
        objs2.forEach((s, k) -> {
            if (!objs1.containsKey(s))
                delta.add("+" + s);
        });
        return delta;
    }

    public static Map<String, List<String>> getFullDelta(Map<String, Map<String, byte[]>> with) {
        Map<String, List<String>> delta = new HashMap<>();
        objects.forEach((s, m) -> {
            List<String> dt = getDelta(m, with.getOrDefault(s, new HashMap<>()));
            delta.put(s, dt);
        });
        with.forEach((s, m) -> {
            if (!objects.containsKey(s)) {
                List<String> dt = new ArrayList<>();
                m.forEach((o, d) -> {
                    dt.add("+" + o);
                });
                delta.put(s, dt);
            }
        });
        return delta;
    }
}
