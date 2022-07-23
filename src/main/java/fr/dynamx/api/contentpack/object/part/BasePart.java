package fr.dynamx.api.contentpack.object.part;

import com.jme3.math.Vector3f;
import fr.aym.acslib.api.services.ErrorTrackingService;
import fr.dynamx.api.contentpack.object.IShapeContainer;
import fr.dynamx.api.contentpack.object.subinfo.ISubInfoTypeOwner;
import fr.dynamx.api.contentpack.object.subinfo.SubInfoType;
import fr.dynamx.api.contentpack.registry.DefinitionType;
import fr.dynamx.api.contentpack.registry.PackFileProperty;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.entities.BaseVehicleEntity;
import fr.dynamx.utils.DynamXLoadingTasks;
import fr.dynamx.utils.debug.DynamXDebugOption;

/**
 * @param <T> Should implement ISubInfoTypeOwner<T> and IShapedObject
 */
public abstract class BasePart<T extends ISubInfoTypeOwner<T>> extends SubInfoType<T> {
    private byte id;
    private final String partName;

    @PackFileProperty(configNames = "Position", oldNames = "ShapePosition", type = DefinitionType.DynamXDefinitionTypes.VECTOR3F_INVERSED_Y, description = "common.position")
    private Vector3f position;
    @PackFileProperty(configNames = "Scale", oldNames = {"Size", "ShapeScale", "BoxDim"}, type = DefinitionType.DynamXDefinitionTypes.VECTOR3F_INVERSED, required = false, description = "common.scale")
    private Vector3f scale;

    public BasePart(T owner, String partName) {
        super(owner);
        this.partName = partName;
    }

    public BasePart(T owner, String partName, Vector3f scale) {
        this(owner, partName);
        this.scale = scale;
    }

    public String getPartName() {
        return partName;
    }

    /**
     * Used internally, don't modify the id except you are an expert
     */
    public void setId(byte id) {
        this.id = id;
    }

    /**
     * @return Internal id of the part
     */
    public byte getId() {
        return id;
    }

    public Vector3f getPosition() {
        return position;
    }

    public Vector3f getScale() {
        return scale;
    }

    /**
     * Adds this part to the vehicle (it shouldn't be already added)
     */
    public void addPart(BaseVehicleEntity<?> vehicle) {
    }

    /**
     * Removes this part from the vehicle (it should have been added before)
     */
    public void removePart(BaseVehicleEntity<?> vehicle) {
    }

    public Vector3f getScaleModifier(T vehicleInfo) {
        return ((IShapeContainer) vehicleInfo).getScaleModifier();
    }

    public DynamXDebugOption getDebugOption() {
        return null;
    }

    @Override
    public void appendTo(T vehicleInfo) {
        if (scale == null) {
            DynamXContext.getErrorTracker().addError(DynamXLoadingTasks.PACK, getPackName(), getName(), "The property 'Scale' is required in " + getName() + " !", ErrorTrackingService.TrackedErrorLevel.HIGH);
        }
        ((IShapeContainer) vehicleInfo).addPart(this);
        getPosition().multLocal(getScaleModifier(vehicleInfo));
        getScale().multLocal(getScaleModifier(vehicleInfo));
    }
}
