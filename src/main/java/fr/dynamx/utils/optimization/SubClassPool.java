package fr.dynamx.utils.optimization;

import lombok.Getter;

public class SubClassPool<T> {
    public static final String DEFAULT_DEFAULT = "default_default";
    public static final String VECTOR3F_DEFAULT = "vec_default";
    public static final String QUATERNION_DEFAULT = "quat_default";
    public static final String BOUNDING_BOX_DEFAULT = "bbp_default";
    public static final String GL_QUATERNION_DEFAULT = "gl_quat_default";
    public static final String PACK_MODEL_LOAD = "pack_model_load";

    public static final String ENTITY_RENDER = "ent_re";
    public static final String ENTITY_RENDER_NODE = "ent_re_node";
    public static final String ITEM_RENDER_NODE = "itm_re";

    public static final String CURSOR_HIT = "cursor_hit";
    public static final String PLAYER_COLL = "player_coll";
    public static final String ROTATED_COLLS_HANDLER_0 = "rchi_0";
    public static final String ROTATED_COLLS_HANDLER_1 = "rchi_1";
    public static final String ROTATED_COLLS_HANDLER_2 = "rchi_2";
    public static final String CHUNK_COLLISIONS_LOAD = "coll_load";
    public static final String CHUNK_COLLISIONS_COMPUTE = "tcc";

    public static final String TICK_ENTITY_PHY_PRE = "pre_phy_ent_tick";
    public static final String TICK_ENTITY_PHY_POST = "post_phy_ent_tick";
    public static final String TICK_ENTITY_MC = "mc_ent_tick";

    public static final String TICK_CLIENT = "client_tick";
    public static final String TICK_SERVER = "srv_tick";
    public static final String TICK_PHYSICS_WORLD = "phy_world_tick";
    public static final String PHYSICS_WORLD_STEP = "phy_world_step";

    public static final String CAMERA_UPDATE = "camera";
    public static final String DX_SOUND_HANDLER = "dxsoundhand";

    @Getter
    private final SubClassPool<T> parent;
    @Getter
    private final int startIndex;
    @Getter
    private int affectedObjectsCount;

    private final String identifier;
    private boolean isUseful;

    public SubClassPool(SubClassPool<T> parent, int startIndex, String identifier) {
        this.parent = parent;
        this.startIndex = startIndex;
        this.identifier = identifier;
    }

    /*public void onClose() {
        if(!isUseful) {
            //TODO CHECK "re", "ftc_load", "asyn_rcv", "ent_re", "tcc"
            List<String> ignoreUseless = Arrays.asList("tcc", "pre_phy_ent_tick", "camera", "po_phy_ent_u", "coll_load", "ent_re", "ftc_load", "asyn_rcv", "re", "bveu", "dxsoundhand", "mov_line", "phy_step", "phy_world_tick", "phy_step_builtin", "srv_tick", "client_tick", "player_coll", "cursor_hit", "slope_terrain_loader");
            if(!ignoreUseless.contains(identifier)) {
                System.out.println("I'm useless :'c " + this);
            }
        }
    }*/

    public void affectObject(T obj) {
        affectedObjectsCount++;
        isUseful = true;
    }

    public int getDepth() {
        SubClassPool<T> pool = this;
        int c = 0;
        while (pool != null) {
            pool = pool.getParent();
            c++;
        }
        return c;
    }

    @Override
    public String toString() {
        return "SubClassPool{" +
                "startIndex=" + startIndex +
                ", affectedObjectsCount=" + affectedObjectsCount +
                ", identifier='" + identifier + '\'' +
                ", isUseful=" + isUseful +
                ", depth=" + getDepth() +
                '}';
    }
}
