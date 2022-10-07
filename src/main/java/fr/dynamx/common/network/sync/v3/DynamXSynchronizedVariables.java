package fr.dynamx.common.network.sync.v3;

import fr.dynamx.utils.DynamXConstants;
import net.minecraft.util.ResourceLocation;

public class DynamXSynchronizedVariables
{
    public static final ResourceLocation POS = new ResourceLocation(DynamXConstants.ID, "pos");

    public static final ResourceLocation CONTROLS = new ResourceLocation(DynamXConstants.ID, "controls");
    public static final ResourceLocation SPEED_LIMIT = new ResourceLocation(DynamXConstants.ID, "speed_limit");
    public static final ResourceLocation ENGINE_PROPERTIES = new ResourceLocation(DynamXConstants.ID, "engine_properties");

    public static final ResourceLocation WHEEL_INFOS = new ResourceLocation(DynamXConstants.ID, "wheel_infos");
    public static final ResourceLocation WHEEL_STATES = new ResourceLocation(DynamXConstants.ID, "wheel_states");
    public static final ResourceLocation WHEEL_PROPERTIES = new ResourceLocation(DynamXConstants.ID, "wheel_properties");
    public static final ResourceLocation WHEEL_VISUALS = new ResourceLocation(DynamXConstants.ID, "wheel_visuals");

    public static final ResourceLocation MOVABLE_MOVER = new ResourceLocation(DynamXConstants.ID, "MOVABLE_MOVER");
    public static final ResourceLocation MOVABLE_PICK_DISTANCE = new ResourceLocation(DynamXConstants.ID, "MOVABLE_PICK_DISTANCE");
    public static final ResourceLocation MOVABLE_PICK_POSITION = new ResourceLocation(DynamXConstants.ID, "MOVABLE_PICK_POSITION");
    public static final ResourceLocation MOVABLE_PICKER = new ResourceLocation(DynamXConstants.ID, "MOVABLE_PICKER");
    public static final ResourceLocation MOVABLE_PICKED_ENTITY = new ResourceLocation(DynamXConstants.ID, "MOVABLE_PICKED_ENTITY");
    public static final ResourceLocation MOVABLE_IS_PICKED = new ResourceLocation(DynamXConstants.ID, "MOVABLE_IS_PICKED");

    public static final ResourceLocation DOORS_STATES = new ResourceLocation(DynamXConstants.ID, "DOORS_STATES");
}
