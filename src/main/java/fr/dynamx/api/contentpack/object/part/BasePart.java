package fr.dynamx.api.contentpack.object.part;

import com.jme3.math.Vector3f;
import fr.aym.acslib.api.services.error.ErrorLevel;
import fr.dynamx.api.contentpack.object.INamedObject;
import fr.dynamx.api.contentpack.object.IPartContainer;
import fr.dynamx.api.contentpack.object.ICollisionsContainer;
import fr.dynamx.api.contentpack.object.subinfo.ISubInfoTypeOwner;
import fr.dynamx.api.contentpack.object.subinfo.SubInfoType;
import fr.dynamx.api.contentpack.registry.DefinitionType;
import fr.dynamx.api.contentpack.registry.IPackFilePropertyFixer;
import fr.dynamx.api.contentpack.registry.PackFileProperty;
import fr.dynamx.api.contentpack.registry.SubInfoTypeRegistries;
import fr.dynamx.utils.debug.DynamXDebugOption;
import fr.dynamx.utils.errors.DynamXErrorManager;
import lombok.Getter;
import lombok.Setter;

/**
 * @param <T> Should implement ISubInfoTypeOwner<T> and IShapedObject
 */
public abstract class BasePart<T extends ISubInfoTypeOwner<?>> extends SubInfoType<T> {
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

    /**
     * Used internally, don't modify the id except you are an expert
     */
    @Getter
    @Setter
    private byte id;
    @Getter
    private final String partName;

    @PackFileProperty(configNames = "Position", type = DefinitionType.DynamXDefinitionTypes.VECTOR3F_INVERSED_Y, description = "common.position")
    @Getter
    private Vector3f position = new Vector3f();
    @PackFileProperty(configNames = "Scale", type = DefinitionType.DynamXDefinitionTypes.VECTOR3F_INVERSED, required = false, description = "common.scale")
    @Getter
    private Vector3f scale = new Vector3f();
    @PackFileProperty(configNames = "DependsOn", required = false, description = "common.unused")
    private String partDependingOnName;

    @Getter
    private BasePart<?> partDependingOn;

    public BasePart(ISubInfoTypeOwner<T> owner, String partName) {
        super(owner);
        this.partName = partName;
    }

    public BasePart(ISubInfoTypeOwner<T> owner, String partName, Vector3f scale) {
        this(owner, partName);
        this.scale = scale;
    }

    public Vector3f getScaleModifier(T vehicleInfo) {
        return ((ICollisionsContainer) vehicleInfo).getScaleModifier();
    }

    public DynamXDebugOption getDebugOption() {
        return null;
    }

    @Override
    public void appendTo(T owner) {
        if (scale == null) {
            INamedObject parent = getRootOwner();
            DynamXErrorManager.addPackError(getPackName(), "required_property", ErrorLevel.HIGH, parent.getName(), "Scale in " + getName());
        }
        ((IPartContainer<T>) this.owner).addPart(this);
        position.multLocal(getScaleModifier(this.owner));
        scale.multLocal(getScaleModifier(this.owner));
    }
}
