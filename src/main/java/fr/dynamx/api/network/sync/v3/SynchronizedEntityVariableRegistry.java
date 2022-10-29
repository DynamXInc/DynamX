package fr.dynamx.api.network.sync.v3;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import fr.dynamx.api.network.sync.PhysicsEntityNetHandler;
import fr.dynamx.api.network.sync.SyncTarget;
import fr.dynamx.api.network.sync.SynchronizedVariable;
import fr.dynamx.common.DynamXMain;
import fr.dynamx.common.entities.PhysicsEntity;
import fr.dynamx.common.network.sync.v3.DynamXSynchronizedVariables;
import fr.dynamx.common.network.sync.vars.*;
import fr.dynamx.utils.debug.DynamXDebugOptions;
import fr.dynamx.utils.optimization.HashMapPool;
import fr.dynamx.utils.optimization.PooledHashMap;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.function.Predicate;

public class SynchronizedEntityVariableRegistry {
    private static final List<ResourceLocation> baseSyncVarRegistry = new ArrayList<>();
    private static final BiMap<ResourceLocation, Integer> syncVarRegistry = HashBiMap.create();

    //TODO ANNOTATION SYSTEM
    public static void addSyncVar(ResourceLocation name) {
        if (baseSyncVarRegistry.contains(name))
            throw new IllegalArgumentException("Duplicate SyncVar " + name);
        baseSyncVarRegistry.add(name);
    }

    private static int getIndex(String of, List<String> in) {
        for (int i = 0; i < in.size(); i++) {
            if (in.get(i).equals(of)) {
                return i;
            }
        }
        throw new IllegalArgumentException(of + " is not in input list " + in);
    }

    /**
     * Sorts variable ids in alphabetical order
     */
    public static void sortRegistry(Predicate<String> useMod) {
        DynamXMain.log.info("Sorting SynchronizedVariables registry ids...");
        Map<ResourceLocation, Integer> nw = new HashMap<>();
        List<String> buff = new ArrayList<>();
        for (ResourceLocation res : baseSyncVarRegistry) {
            if (useMod.test(res.getNamespace())) {
                buff.add(res.toString());
            }
        }
        Collections.sort(buff);
        for (ResourceLocation res : baseSyncVarRegistry) {
            if (buff.contains(res.toString())) {
                int index = getIndex(res.toString(), buff);
                nw.put(res, index);
            }
        }
        fixIds(nw);
    }

    private static void fixIds(Map<ResourceLocation, Integer> syncVarRegistry) {
        DynamXMain.log.debug("Fixing SynchronizedVariables registry ids...");
        SynchronizedEntityVariableRegistry.syncVarRegistry.clear();
        syncVarRegistry.forEach((r, i) -> {
            DynamXMain.log.debug("Add : " + r + " = " + i);
            SynchronizedEntityVariableRegistry.syncVarRegistry.put(r, i);
        });
    }

    public static Map<ResourceLocation, Integer> getSyncVarRegistry() {
        return syncVarRegistry;
    }

    /**
     * Internal variables, example to add your own variables
     */
    static {
        addSyncVar(DynamXSynchronizedVariables.POS);
        addSyncVar(DynamXSynchronizedVariables.CONTROLS);
        addSyncVar(DynamXSynchronizedVariables.SPEED_LIMIT);
        addSyncVar(DynamXSynchronizedVariables.ENGINE_PROPERTIES);
        addSyncVar(DynamXSynchronizedVariables.WHEEL_INFOS);
        addSyncVar(DynamXSynchronizedVariables.WHEEL_STATES);
        addSyncVar(DynamXSynchronizedVariables.WHEEL_PROPERTIES);
        addSyncVar(DynamXSynchronizedVariables.WHEEL_VISUALS);
        addSyncVar(DynamXSynchronizedVariables.MOVABLE_MOVER);
        addSyncVar(DynamXSynchronizedVariables.MOVABLE_PICK_DISTANCE);
        addSyncVar(DynamXSynchronizedVariables.MOVABLE_PICK_POSITION);
        addSyncVar(DynamXSynchronizedVariables.MOVABLE_PICKER);
        addSyncVar(DynamXSynchronizedVariables.MOVABLE_PICKED_ENTITY);
        addSyncVar(DynamXSynchronizedVariables.MOVABLE_IS_PICKED);
        addSyncVar(DynamXSynchronizedVariables.DOORS_STATES);
    }

    public static PooledHashMap<Integer, SynchronizedEntityVariable<?>> retainSyncVars(Map<Integer, SynchronizedEntityVariable<?>> syncVars, Map<Integer, SyncTarget> changes, SyncTarget target) {
        PooledHashMap<Integer, SynchronizedEntityVariable<?>> ret = HashMapPool.get();
        changes.forEach((i, t) -> {
            if (target.isIncluded(t))
                ret.put(i, syncVars.get(i));
        });
        return ret;
    }
}
