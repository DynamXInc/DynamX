package fr.dynamx.common.contentpack.type;

import com.jme3.math.Vector3f;
import fr.aym.acslib.api.services.error.ErrorLevel;
import fr.dynamx.api.contentpack.object.subinfo.ISubInfoTypeOwner;
import fr.dynamx.api.contentpack.object.subinfo.SubInfoType;
import fr.dynamx.api.contentpack.registry.DefinitionType;
import fr.dynamx.api.contentpack.registry.PackFileProperty;
import fr.dynamx.api.contentpack.registry.RegisteredSubInfoType;
import fr.dynamx.api.contentpack.registry.SubInfoTypeRegistries;
import fr.dynamx.utils.errors.DynamXErrorManager;
import fr.dynamx.utils.maths.DynamXMath;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraftforge.fml.relauncher.Side;
import org.joml.Matrix4f;

import java.util.Arrays;

/**
 * Item transforms for a specific {@link net.minecraft.client.renderer.block.model.ItemCameraTransforms.TransformType}
 *
 * @see ItemTransformsInfo
 */
@RegisteredSubInfoType(name = "View", registries = {SubInfoTypeRegistries.WHEELED_VEHICLES, SubInfoTypeRegistries.HELICOPTER,
        SubInfoTypeRegistries.BLOCKS, SubInfoTypeRegistries.ITEMS, SubInfoTypeRegistries.ARMORS}, strictName = false, isClientOnly = true)
public class ViewTransformsInfo extends SubInfoType<ItemTransformsInfo> {
    private final String name;

    @Getter
    private final Matrix4f transformMatrix = new Matrix4f();

    @Getter
    @Setter
    @PackFileProperty(configNames = "Scale", required = false, description = "common.itemscale", defaultValue = "0.2 for vehicles, 0.3 for blocks and props, 0.7 for armors, 0.9 for items")
    protected float itemScale = -1f;
    @Getter
    @PackFileProperty(configNames = "Translate", type = DefinitionType.DynamXDefinitionTypes.VECTOR3F, required = false, defaultValue = "0 0 0")
    protected Vector3f itemTranslate = new Vector3f(0, 0, 0);
    @Getter
    @PackFileProperty(configNames = "Rotate", type = DefinitionType.DynamXDefinitionTypes.VECTOR3F, required = false, defaultValue = "0 0 0")
    protected Vector3f itemRotate = new Vector3f(0, 0, 0);

    public ViewTransformsInfo(ISubInfoTypeOwner<ItemTransformsInfo> owner, String name) {
        super(owner);
        this.name = name;
    }

    public ViewTransformsInfo(ItemTransformsInfo owner, EnumViewType type, float itemScale, Vector3f itemTranslate, Vector3f itemRotate) {
        this(owner, type.names[0]);
        this.itemScale = itemScale;
        if (itemTranslate != null)
            this.itemTranslate.set(itemTranslate);
        if (itemRotate != null)
            this.itemRotate.set(itemRotate);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void appendTo(ItemTransformsInfo owner) {
        String name = this.name.replace("View", "");
        if (name.startsWith("_"))
            name = name.substring(1);
        EnumViewType viewType = EnumViewType.getFromName(name);
        if (viewType == null) {
            DynamXErrorManager.addPackError(getPackName(), "invalid_view_type", ErrorLevel.HIGH, owner.getOwner().getName(),
                    "Valid view types are " + Arrays.toString(EnumViewType.values()) + ", but found " + name);
            return;
        }
        owner.addViewTransforms(viewType.transformType, this);
        transformMatrix.identity();
        com.jme3.math.Vector3f translate = getItemTranslate();
        transformMatrix.translate(translate.x, translate.y, translate.z);
        float scale = itemScale == -1 ? owner.getOwner().getBaseItemScale() : itemScale;
        transformMatrix.scale(scale, scale, scale);
        com.jme3.math.Vector3f rotate = getItemRotate();
        transformMatrix.rotate(rotate.x * DynamXMath.TO_RADIAN, 1, 0, 0);
        transformMatrix.rotate(rotate.y * DynamXMath.TO_RADIAN, 0, 1, 0);
        transformMatrix.rotate(rotate.z * DynamXMath.TO_RADIAN, 0, 0, 1);
    }

    public enum EnumViewType {
        THIRD_PERSON_LEFT_HAND(ItemCameraTransforms.TransformType.THIRD_PERSON_LEFT_HAND, "ThirdPersonLeftHand", "TPLH"),
        THIRD_PERSON_RIGHT_HAND(ItemCameraTransforms.TransformType.THIRD_PERSON_RIGHT_HAND, "ThirdPersonRightHand", "TPRH"),
        FIRST_PERSON_LEFT_HAND(ItemCameraTransforms.TransformType.FIRST_PERSON_LEFT_HAND, "FirstPersonLeftHand", "FPLH"),
        FIRST_PERSON_RIGHT_HAND(ItemCameraTransforms.TransformType.FIRST_PERSON_RIGHT_HAND, "FirstPersonRightHand", "FPRH"),
        HEAD(ItemCameraTransforms.TransformType.HEAD, "Head"),
        GUI(ItemCameraTransforms.TransformType.GUI, "Gui"),
        GROUND(ItemCameraTransforms.TransformType.GROUND, "Ground"),
        FIXED(ItemCameraTransforms.TransformType.FIXED, "Fixed");

        private final ItemCameraTransforms.TransformType transformType;
        private final String[] names;

        EnumViewType(ItemCameraTransforms.TransformType transformType, String... names) {
            this.transformType = transformType;
            this.names = names;
        }

        public static EnumViewType getFromName(String name) {
            for (EnumViewType viewType : values()) {
                for (String s : viewType.names) {
                    if (s.equalsIgnoreCase(name)) {
                        return viewType;
                    }
                }
            }
            return null;
        }
    }
}
