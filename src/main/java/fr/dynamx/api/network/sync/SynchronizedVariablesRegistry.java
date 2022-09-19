package fr.dynamx.api.network.sync;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import fr.dynamx.common.DynamXMain;
import fr.dynamx.common.entities.PhysicsEntity;
import fr.dynamx.common.network.sync.vars.*;
import fr.dynamx.utils.debug.DynamXDebugOptions;
import fr.dynamx.utils.optimization.HashMapPool;
import fr.dynamx.utils.optimization.PooledHashMap;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.function.Predicate;

/**
 * A class to register the variables to sync from server to client bullet entities <br>
 * This system is different from DataManager of vanilla mc because it adds options of interpolation, and artificial lag if the connection is not regular
 */
public class SynchronizedVariablesRegistry {
    private static final BiMap<ResourceLocation, Callable<SynchronizedVariable<?>>> baseSyncVarRegistry = HashBiMap.create();
    private static final BiMap<ResourceLocation, Integer> syncVarRegistry = HashBiMap.create();
    private static final Map<Integer, Callable<SynchronizedVariable<?>>> syncVars = new HashMap<>();
    //private static final Map<Integer, BiFunction<Side, PhysicsEntityNetHandler<?>, Boolean>> useContexts = new HashMap<>();

    /**
     * @param name     A unique identifier for this variable. The modid <strong>MUST</strong> be the modid of the addon : if the mod of this addon isn't loaded on the other side (server or client), the sync var won't be added
     * @param variable A callable creating the instance of the variable to sync
     * @return An unique id to identify this variable, should be used when you register the variable inside of the entity's BulletEntityNetHandler
     */
    public static void addSyncVar(ResourceLocation name, Callable<SynchronizedVariable<?>> variable) {
        if (baseSyncVarRegistry.containsKey(name)) {
            throw new IllegalArgumentException("Duplicate SyncVar " + name);
        }
        baseSyncVarRegistry.put(name, variable);
        try {
            DynamXMain.log.debug("Registered sync var " + variable.call() + " with name " + name);
        } catch (Exception e) {
            e.printStackTrace();
        }
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
        DynamXMain.log.debug("Sorting SynchronizedVariables registry ids...");
        Map<ResourceLocation, Integer> nw = new HashMap<>();
        List<String> buff = new ArrayList<>();
        for (ResourceLocation res : baseSyncVarRegistry.keySet()) {
            if (useMod.test(res.getNamespace())) {
                buff.add(res.toString());
            }
        }
        Collections.sort(buff);
        for (ResourceLocation res : baseSyncVarRegistry.keySet()) {
            if (buff.contains(res.toString())) {
                int index = getIndex(res.toString(), buff);
                nw.put(res, index);
            }
        }
        fixIds(nw);
    }

    private static void fixIds(Map<ResourceLocation, Integer> syncVarRegistry) {
        DynamXMain.log.debug("Fixing SynchronizedVariables registry ids...");
        SynchronizedVariablesRegistry.syncVarRegistry.clear();
        SynchronizedVariablesRegistry.syncVars.clear();
        /*SynchronizedVariablesRegistry.syncVarRegistry.forEach((r, i) -> {
            if(!syncVarRegistry.containsKey(r))
                throw new IllegalStateException("Sync var "+r+" not registered on server !");
        });*/
        syncVarRegistry.forEach((r, i) -> {
            //if (!SynchronizedVariablesRegistry.syncVarRegistry.containsKey(r)) {
            DynamXMain.log.debug("Add : " + r + " = " + i);
            SynchronizedVariablesRegistry.syncVarRegistry.put(r, i);
            SynchronizedVariablesRegistry.syncVars.put(i, baseSyncVarRegistry.get(r));
            // throw new IllegalStateException("Sync var "+r+" not registered on client !");
           /* } else {
                int j = SynchronizedVariablesRegistry.syncVarRegistry.get(r);
                if (j != i) {
                    ResourceLocation y = SynchronizedVariablesRegistry.syncVarRegistry.inverse().remove(i);
                    SynchronizedVariablesRegistry.syncVarRegistry.inverse().remove(j);
                    DynamXMain.log.debug("Fix id mismatch " + i + " (is " + j + ") for " + r + " hurts " + y);
                    Callable<SynchronizedVariable<?>> prev = syncVars.get(i);
                    //BiFunction<Side, PhysicsEntityNetHandler<?>, Boolean> pre = useContexts.get(i);
                    syncVars.put(i, syncVars.get(j));
                    //useContexts.put(i, useContexts.get(j));
                    syncVars.put(j, prev);
                    //useContexts.put(j, pre);
                    SynchronizedVariablesRegistry.syncVarRegistry.put(r, i);
                    SynchronizedVariablesRegistry.syncVarRegistry.put(y, j);
                }
            }*/
        });
    }

    public static Map<ResourceLocation, Integer> getSyncVarRegistry() {
        return syncVarRegistry;
    }

    /**
     * Internally used to create a new variable corresponding to the given id
     */
    public static SynchronizedVariable<?> instantiate(int id) {
        if (!syncVars.containsKey(id))
            throw new IllegalStateException("Sync slot with id " + id + " does not exists ! Check sync var registering order...");
        try {
            return syncVars.get(id).call();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Internal variables, example to add your own variables
     */
    static {
        addSyncVar(PosSynchronizedVariable.NAME, () -> DynamXDebugOptions.FULL_NETWORK_DEBUG.isActive() ? new DebugPosSynchronizedVariable() : new PosSynchronizedVariable());//, (s, e) -> (s.isServer() || e.getSimulationHolder().isMe(s)));
        addSyncVar(VehicleSynchronizedVariables.Visuals.NAME, VehicleSynchronizedVariables.Visuals::new);//, (s, e) -> (s.isServer()));
        addSyncVar(VehicleSynchronizedVariables.Engine.NAME, VehicleSynchronizedVariables.Engine::new);//, (s, e) -> (e.getEntity() instanceof IHaveModule.IHaveEngine && s.isServer())); //Temporary
        addSyncVar(VehicleSynchronizedVariables.WheelVisuals.NAME, VehicleSynchronizedVariables.WheelVisuals::new);//, (s, e) -> (e.getEntity() instanceof IHaveModule.IHavePropulsion && ((IHaveModule.IHavePropulsion<?>)e.getEntity()).getPropulsionType() == PropulsionType.WHEEL && s.isServer()));
        addSyncVar(VehicleSynchronizedVariables.WheelPhysics.NAME, VehicleSynchronizedVariables.WheelPhysics::new);//, (s, e) -> (e.getEntity() instanceof IHaveModule.IHavePropulsion && ((IHaveModule.IHavePropulsion<?>)e.getEntity()).getPropulsionType() == PropulsionType.WHEEL && (s.isServer() || e.getSimulationHolder().isMe(s))));
        addSyncVar(VehicleSynchronizedVariables.Controls.NAME, VehicleSynchronizedVariables.Controls::new);//, (s, e) -> e.getEntity() instanceof IHaveModule.IHaveEngine/* && ((IHaveModule.IHaveEngine)e.getEntity()).getEngine() instanceof EngineModule*/ && (e.getSimulationHolder() == SimulationHolder.SERVER_SP ? s.isClient() : s.isServer() || e.getSimulationHolder().isMe(s)));
        addSyncVar(MovableSynchronizedVariable.NAME, MovableSynchronizedVariable::new);//, (s, e) -> e.getEntity() instanceof IHaveModule.IHaveEngine/* && ((IHaveModule.IHaveEngine)e.getEntity()).getEngine() instanceof EngineModule*/ && (e.getSimulationHolder() == SimulationHolder.SERVER_SP ? s.isClient() : s.isServer() || e.getSimulationHolder().isMe(s)));
        addSyncVar(VehicleSynchronizedVariables.DoorsStatus.NAME, VehicleSynchronizedVariables.DoorsStatus::new);//, (s, e) -> e.getEntity() instanceof IHaveModule.IHaveEngine/* && ((IHaveModule.IHaveEngine)e.getEntity()).getEngine() instanceof EngineModule*/ && (e.getSimulationHolder() == SimulationHolder.SERVER_SP ? s.isClient() : s.isServer() || e.getSimulationHolder().isMe(s)));
        //addSyncVar(DoorsSynchronizedVariable::new, (s, e) -> e.getEntity() instanceof IHaveModule.IHaveSeats && ((IHaveModule.IHaveSeats)e.getEntity()).getSeats().hasDoors() && s.isServer());
        addSyncVar(AttachedDoorsSynchronizedVariable.NAME, AttachedDoorsSynchronizedVariable::new);
        addSyncVar(RagdollPartsSynchronizedVariable.NAME, RagdollPartsSynchronizedVariable::new);
    }

    public static <T extends PhysicsEntity<?>> PooledHashMap<Integer, SynchronizedVariable<T>> retainSyncVars(Map<Integer, SynchronizedVariable<T>> syncVars, Map<Integer, SyncTarget> changes, SyncTarget target) {
        PooledHashMap<Integer, SynchronizedVariable<T>> ret = HashMapPool.get();
        changes.forEach((i, t) -> {
            if (target.isIncluded(t)) {
                syncVars.get(i).validate(target, 2);
                ret.put(i, syncVars.get(i));
            }
        });
        return ret;
    }

    public static <T extends PhysicsEntity<?>> void setSyncVarsForContext(Side side, Map<Integer, SynchronizedVariable<T>> vars, PhysicsEntityNetHandler<T> network) {
        List<ResourceLocation> varsToUse = network.entity.getSynchronizedVariables(side, network.getSimulationHolder());
        syncVarRegistry.forEach((r, i) -> {
            boolean b = varsToUse.contains(r);
            if (vars.containsKey(i) && !b) {
                vars.remove(i);
            } else if (b) {
                vars.put(i, (SynchronizedVariable<T>) instantiate(i));
            }
        });
        /*for (int i = 0; i < syncVars.size(); i++) {
            if(syncVars.containsKey(i))
            {
                try {
                    System.out.println("At "+i+" :" +syncVars.get(i).call());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            else
                System.out.println("WTF NOT FOUND "+i);
        }*/
    }
}
