package fr.dynamx.api.contentpack.object.part;

import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import fr.aym.acslib.api.services.error.ErrorLevel;
import fr.dynamx.api.contentpack.object.ICollisionsContainer;
import fr.dynamx.api.contentpack.object.INamedObject;
import fr.dynamx.api.contentpack.object.IPartContainer;
import fr.dynamx.api.contentpack.object.subinfo.ISubInfoTypeOwner;
import fr.dynamx.api.contentpack.object.subinfo.SubInfoType;
import fr.dynamx.api.contentpack.registry.DefinitionType;
import fr.dynamx.api.contentpack.registry.IPackFilePropertyFixer;
import fr.dynamx.api.contentpack.registry.PackFileProperty;
import fr.dynamx.api.contentpack.registry.SubInfoTypeRegistries;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.objloader.data.DxModelData;
import fr.dynamx.utils.DynamXUtils;
import fr.dynamx.utils.debug.DynamXDebugOption;
import fr.dynamx.utils.errors.DynamXErrorManager;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.util.ResourceLocation;

/**
 * The base of a part <br>
 * A part is a part of a vehicle, like a wheel, a seat, a door, etc... <br>
 * It has a position and a shape, it can generally be rendered and is sometimes interactive <br>
 * It isn't a simple {@link fr.dynamx.api.contentpack.object.subinfo.ISubInfoType} containing properties about your entity, a BasePart represents a real shape.
 *
 * @param <T> The owner of this part. Should implement ISubInfoTypeOwner<?>.
 */
@Getter
public abstract class BasePart<T extends ISubInfoTypeOwner<?>> extends SubInfoType<T> {
    /**
     * Deprecated properties of BasePart: <br>
     * - ShapePosition -> Position <br>
     * - Size/ShapeScale/BoxDim -> Scale
     */
    @IPackFilePropertyFixer.PackFilePropertyFixer(registries = {SubInfoTypeRegistries.WHEELED_VEHICLES, SubInfoTypeRegistries.BLOCKS, SubInfoTypeRegistries.PROPS})
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
     * Unique id for this part (among the group of parts with the same id class). <br>
     * Used internally, don't modify the id except you are an expert.
     *
     * @see BasePart#getIdClass()
     */
    @Setter
    private byte id;
    /**
     * The name of this part, given when creating it in the pack. <br>
     * This is <strong>NOT</strong> the object name used by some part to design the corresponding object in the 3D model.
     */
    private final String partName;

    /**
     * The position of this part, relative to the 3D model. <br>
     * If null, it will be read from the 3D model (if possible, see readPositionFromModel method).
     */
    @Setter
    @PackFileProperty(configNames = "Position", type = DefinitionType.DynamXDefinitionTypes.VECTOR3F_INVERSED_Y, description = "common.position", required = false, defaultValue = "From model")
    protected Vector3f position;
    /**
     * Indicates if the position was read from the 3D model, or set by the user. <br>
     * Changes the behavior of the rendering.
     */
    @Setter
    protected boolean isAutomaticPosition;
    /**
     * The scale (size) of this part, relative to the 3D model.
     */
    @PackFileProperty(configNames = "Scale", type = DefinitionType.DynamXDefinitionTypes.VECTOR3F_INVERSED, required = false, description = "common.scale", defaultValue = "0.5 1 0.5")
    protected Vector3f scale = new Vector3f(0.5f, 1, 0.5f);

    /**
     * Creates a new part
     *
     * @param owner    The owner of this part
     * @param partName The name of this part
     */
    public BasePart(ISubInfoTypeOwner<T> owner, String partName) {
        super(owner);
        this.partName = partName;
    }

    /**
     * Creates a new part with a predefined size (scale)
     *
     * @param owner    The owner of this part
     * @param partName The name of this part
     * @param scale    The size of this part
     */
    public BasePart(ISubInfoTypeOwner<T> owner, String partName, Vector3f scale) {
        this(owner, partName);
        this.scale = scale;
    }

    /**
     * @param owner The owner of the part
     * @return The scale modifier of the owner
     */
    public Vector3f getScaleModifier(T owner) {
        return ((ICollisionsContainer) owner).getScaleModifier();
    }

    /**
     * @return The debug option of this part, or null if there is none
     */
    public DynamXDebugOption getDebugOption() {
        return null;
    }

    /**
     * If the configured position is null, this method reads the position from the 3D model owning this part.
     *
     * @param model The 3D model owning this part
     * @param objectName  The name of the object in the 3D model
     * @param allowPartCenter If true, the center of the object will be used as position for obj models (and the translation for gltf models) <br>
     *                        If false, the position can only be read from gltf models
     * @param getRotation If true, the rotation of the object in the model will be returned (only for gltf models)
     * @return The rotation of the object in the model, or null if getRotation is false or if the model is an obj model
     */
    public Quaternion readPositionFromModel(ResourceLocation model, String objectName, boolean allowPartCenter, boolean getRotation) {
        Quaternion rotation = null;
        if (getPosition() == null) {
            DxModelData modelData = DynamXContext.getDxModelDataFromCache(DynamXUtils.getModelPath(getPackName(), model));
            if (modelData != null) {
                setPosition(DynamXUtils.readPartPosition(modelData, objectName, allowPartCenter));
                if (getRotation && getPosition() != null)
                    rotation = DynamXUtils.readPartRotation(modelData, objectName);
            }
            if (getPosition() == null) {
                INamedObject parent = getRootOwner();
                DynamXErrorManager.addPackError(getPackName(), "position_not_found_in_model", ErrorLevel.HIGH, parent.getName(), "3D object '" + objectName + "' of part " + getName()+ " (for property 'Position')");
                setPosition(new Vector3f());
            } else {
                isAutomaticPosition = true;
            }
        }
        return rotation;
    }

    @Override
    public void appendTo(T owner) {
        if (scale == null) {
            INamedObject parent = getRootOwner();
            DynamXErrorManager.addPackError(getPackName(), "required_property", ErrorLevel.HIGH, parent.getName(), "Scale in " + getName());
        } else {
            scale.multLocal(getScaleModifier(this.owner));
        }
        if (position == null) {
            INamedObject parent = getRootOwner();
            DynamXErrorManager.addPackError(getPackName(), "required_property", ErrorLevel.HIGH, parent.getName(), "Position in " + getName());
            position = new Vector3f();
        } else {
            position.multLocal(getScaleModifier(this.owner));
        }
        ((IPartContainer<T>) this.owner).addPart(this);
    }

    /**
     * @return The class used to generate the unique id of this part.
     */
    public Class<?> getIdClass() {
        return getClass();
    }
}
