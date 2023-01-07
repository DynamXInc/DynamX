package fr.dynamx.utils.debug;

import fr.dynamx.common.DynamXMain;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A simple home-made thread safe profiler
 */
public class Profiler {
    private static boolean isProfilingOn;
    private static final ThreadLocal<Profiler> LOCAL_PROFILER = ThreadLocal.withInitial(Profiler::new);

    public static Profiler get() {
        return LOCAL_PROFILER.get();
    }

    public static void setIsProfilingOn(boolean isProfilingOn) {
        Profiler.isProfilingOn = isProfilingOn;
    }

    public static boolean isProfilingOn() {
        return isProfilingOn;
    }

    private final Map<Profiles, ProfilingData> data = new HashMap<>();
    private boolean isActive;

    public boolean isActive() {
        return isActive;
    }

    public void start(Profiles profile) {
        if (isActive) {
            if (!data.containsKey(profile))
                data.put(profile, new ProfilingData(profile));
            data.get(profile).start();
        }
    }

    public void end(Profiles profile) {
        if (isActive && data.containsKey(profile))
            data.get(profile).end();
    }

    public void update() {
        if (isActive)
            data.values().forEach(ProfilingData::update);
        if (isActive && !isProfilingOn)
            reset();
        isActive = isProfilingOn;
    }

    public List<String> getData() {
        List<String> result = new ArrayList<>();
        data.forEach((p, d) -> {
            if (!d.isEmpty())
                result.add(d.toString());
        });
        return result;
    }

    public void printData(String displayName) {
        List<String> st = getData();
        if (!st.isEmpty()) {
            DynamXMain.log.info("==== " + displayName + " profiling data ====");
            for (String s : st) {
                DynamXMain.log.info(s);
            }
            DynamXMain.log.info("========================");
        }
    }

    public void reset() {
        data.values().forEach(ProfilingData::reset);
    }

    private static int lastId;

    @Nullable
    public ProfilingData getData(Profiles profile) {
        return data.get(profile);
    }

    public enum Profiles {
        TICK(),
        STEP_SIMULATION(),
        LOAD_SHAPES(),
        DELTA_COMPUTE,
        MARK_LOAD,
        GET_T0,
        GET_T1,
        GET_T2,
        ADD_T3,
        KEEP_T4,
        LOAD_NOW,
        ADD_USED,
        RCV_ASYNC,
        REFRESH_CHUNKS(),
        LOAD_CHUNKS_FOR_ENTITY(),
        EMERGENCY_CHUNK_LOAD(),
        CHUNK_BLOCK_COLLS_COMPUTE(),
        CHUNK_SHAPE_COMPUTE(),
        CHUNK_COLLS_LOAD_FROM_FILE(),
        ADD_DEBUG_TO_WORLD(),
        ADD_REMOVE_BODIES(),
        BULLET_STEP_SIM(),
        BULLET_STEP_SIM_S1(),
        BULLET_STEP_SIM_E1(),
        BULLET_STEP_SIM_S2(),
        BULLET_STEP_SIM_E2(),
        TICK_TERRAIN(),
        PHY1(),
        PHY2(),
        PHY2P(),
        PKTSEND2(),
        UPDATE_VEHICLE_PHYSICS, TERRAIN_LOADER_TICK, PHYSICS_TICK_ENTITIES_PRE, PHYSICS_TICK_ENTITIES_POST, TICK_ENTITIES,
        SYNC_BUFFER_UPDATE,
        SLOPE_CALCULUS,
        ENTITY_COLLISION;
        public final int id;

        Profiles() {
            this.id = lastId;
            lastId++;
        }
    }
}
