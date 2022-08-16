package fr.dynamx.api.contentpack.object.part;

import com.jme3.math.Vector3f;
import fr.aym.acslib.api.services.error.ErrorLevel;
import fr.dynamx.api.contentpack.object.IShapedObject;
import fr.dynamx.api.contentpack.object.subinfo.ISubInfoTypeOwner;
import fr.dynamx.api.contentpack.object.subinfo.SubInfoType;
import fr.dynamx.api.contentpack.registry.DefinitionType;
import fr.dynamx.api.contentpack.registry.IPackFilePropertyFixer;
import fr.dynamx.api.contentpack.registry.PackFileProperty;
import fr.dynamx.api.contentpack.registry.SubInfoTypeRegistries;
import fr.dynamx.common.entities.BaseVehicleEntity;
import fr.dynamx.utils.errors.DynamXErrorManager;

/**
 * @param <T> Should implement ISubInfoTypeOwner<T> and IShapedObject
 */
public abstract class BasePart<T extends ISubInfoTypeOwner<T>> extends SubInfoType<T> {
    @IPackFilePropertyFixer.PackFilePropertyFixer(registries = {SubInfoTypeRegistries.WHEELED_VEHICLES, SubInfoTypeRegistries.BLOCKS_AND_PROPS})
    public static final IPackFilePropertyFixer PROPERTY_FIXER = (object, key, value) -> {
        switch (key) {
            case "ShapePosition":
                return new IPackFilePropertyFixer.FixResult("Position", true);
            case "Size":
            case "ShapeScale":
            case "BoxDim":
                return new IPackFilePropertyFixer.FixResult("Scale", true);
        }
        return null;
    };

    private byte id;
    private final String partName;

    @PackFileProperty(configNames = "Position", type = DefinitionType.DynamXDefinitionTypes.VECTOR3F_INVERSED_Y, description = "common.position")
    private Vector3f position;
    @PackFileProperty(configNames = "Scale", type = DefinitionType.DynamXDefinitionTypes.VECTOR3F_INVERSED, required = false, description = "common.scale")
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
        return ((IShapedObject) vehicleInfo).getScaleModifier();
    }

    @Override
    public void appendTo(T owner) {
        if (scale == null)
            DynamXErrorManager.addError(getPackName(), "required_property", ErrorLevel.HIGH, getName(), "Scale");
        ((IShapedObject) owner).addPart(this);
        getPosition().multLocal(getScaleModifier(owner));
        getScale().multLocal(getScaleModifier(owner));
    }
}
