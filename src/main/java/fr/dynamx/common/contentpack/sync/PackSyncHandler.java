package fr.dynamx.common.contentpack.sync;

import fr.dynamx.api.contentpack.object.INamedObject;
import fr.dynamx.common.contentpack.DynamXObjectLoaders;
import fr.dynamx.common.contentpack.loader.PackFilePropertyData;
import fr.dynamx.common.contentpack.loader.SubInfoTypeAnnotationCache;
import fr.hermes.forge1122.dynamx.DynamXConfig;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/**
 * Responsible to keep same object properties on client and server side, if the config option is enabled
 */
public class PackSyncHandler {
    private static final Map<String, Map<String, byte[]>> objects = new HashMap<>();

    public static void computeAll() {
        //objects.clear();
        if (DynamXConfig.syncPacks) {
            DynamXObjectLoaders.getInfoLoaders().forEach((i) -> {
                Map<String, byte[]> objs = new HashMap<>();
                PackSyncHandler hacheur = new PackSyncHandler();
                i.hashObjects(hacheur, objs);
                objects.put(i.getPrefix(), objs);
            });
        }
    }

    public static Map<String, Map<String, byte[]>> getObjects() {
        return objects;
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
        getObjects().forEach((s, m) -> {
            List<String> dt = getDelta(m, with.getOrDefault(s, new HashMap<>()));
            delta.put(s, dt);
        });
        with.forEach((s, m) -> {
            if (!getObjects().containsKey(s)) {
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
