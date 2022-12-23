package fr.dynamx.common.contentpack.type.vehicle;

import com.jme3.math.Vector3f;
import fr.dynamx.api.contentpack.object.INamedObject;
import fr.dynamx.api.contentpack.object.render.IObjPackObject;
import fr.dynamx.api.contentpack.object.subinfo.ISubInfoType;
import fr.dynamx.api.contentpack.object.subinfo.SubInfoTypeOwner;
import fr.dynamx.api.contentpack.registry.DefinitionType;
import fr.dynamx.api.contentpack.registry.IPackFilePropertyFixer;
import fr.dynamx.api.contentpack.registry.PackFileProperty;
import fr.dynamx.api.contentpack.registry.SubInfoTypeRegistries;
import fr.dynamx.api.obj.IModelTextureVariantsSupplier;
import fr.dynamx.client.renders.model.renderer.ObjObjectRenderer;
import fr.dynamx.client.renders.model.texture.TextureVariantData;
import fr.dynamx.common.contentpack.type.MaterialVariantsInfo;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nullable;
import java.util.Map;

/**
 * Wheel contained in a wheel file
 */
public class PartWheelInfo extends SubInfoTypeOwner<PartWheelInfo> implements ISubInfoType<PartWheelInfo>, IObjPackObject, INamedObject {
    @IPackFilePropertyFixer.PackFilePropertyFixer(registries = SubInfoTypeRegistries.WHEELS)
    public static final IPackFilePropertyFixer PROPERTY_FIXER = (object, key, value) -> {
        if ("WheelRadius".equals(key))
            return new IPackFilePropertyFixer.FixResult("Radius", true);
        if ("Textures".equals(key))
            return new IPackFilePropertyFixer.FixResult("MaterialVariants", true, true);
        return null;
    };

    private final String packName;
    private final String partName;

    @PackFileProperty(configNames = "Model", description = "common.model", type = DefinitionType.DynamXDefinitionTypes.DYNX_RESOURCE_LOCATION, defaultValue = "obj/nom_du_vehicule/nom_du_modele.obj")
    private ResourceLocation model;
    @PackFileProperty(configNames = "Width")
    private float wheelWidth;
    @PackFileProperty(configNames = "Radius")
    private float wheelRadius;
    @PackFileProperty(configNames = "RimRadius")
    private float rimRadius;
    @PackFileProperty(configNames = "Friction")
    private float wheelFriction;
    @PackFileProperty(configNames = "BrakeForce")
    private float wheelBrakeForce;
    @PackFileProperty(configNames = "HandBrakeForce", required = false, defaultValue = "2*BrakeForce")
    private float handBrakeForce = -1;
    @PackFileProperty(configNames = "RollInInfluence")
    private float wheelRollInInfluence;
    @PackFileProperty(configNames = "SuspensionRestLength")
    private float suspensionRestLength;
    @PackFileProperty(configNames = "SuspensionStiffness")
    private float suspensionStiffness;
    @PackFileProperty(configNames = "SuspensionMaxForce")
    private float suspensionMaxForce;
    @PackFileProperty(configNames = "WheelDampingRelaxation")
    private float wheelsDampingRelaxation;
    @PackFileProperty(configNames = "WheelsDampingCompression")
    private float wheelsDampingCompression;
    @PackFileProperty(configNames = "SkidParticle", required = false)
    private EnumParticleTypes skidParticle = EnumParticleTypes.SMOKE_NORMAL;

    @PackFileProperty(configNames = "ScaleModifier", type = DefinitionType.DynamXDefinitionTypes.VECTOR3F, required = false)
    private Vector3f scaleModifier = new Vector3f(1, 1, 1);

    @Deprecated
    @PackFileProperty(configNames = "Textures", required = false, type = DefinitionType.DynamXDefinitionTypes.STRING_ARRAY_2D, defaultValue = "\"Textures: DynamX\"")
    private String[][] texturesArray;

    public PartWheelInfo(String packName, String partName) {
        this.packName = packName;
        this.partName = partName;
    }

    @Override
    public String getName() {
        return getPartName();
    }

    @Override
    public String getPackName() {
        return packName;
    }

    @Override
    public String getFullName() {
        return getPackName() + "." + getPartName();
    }

    @Override //A sub info types owner is an info type himself (it is THE root owning all properties)
    public void appendTo(PartWheelInfo owner) {
    }

    @Nullable
    @Override
    public PartWheelInfo getOwner() {
        return null;
    }

    @Override
    public void onComplete(boolean hotReload) {
        if (handBrakeForce == -1)
            handBrakeForce = wheelBrakeForce * 2;
        wheelRadius = getWheelRadius() * getScaleModifier().z;
        wheelWidth = getWheelWidth() * getScaleModifier().x;
        if(texturesArray != null)
            new MaterialVariantsInfo<>(this, texturesArray).appendTo(this);
    }

    public MaterialVariantsInfo<?> getVariants() {
        return getSubPropertyByType(MaterialVariantsInfo.class);
    }

    public byte getIdForVariant(String textureName) {
        MaterialVariantsInfo<?> variantsInfo = getVariants();
        if(variantsInfo != null) {
            for (byte i = 0; i < variantsInfo.getVariantsMap().size(); i++) {
                if (variantsInfo.getVariantsMap().get(i).getName().equalsIgnoreCase(textureName))
                    return i;
            }
        }
        return 0;
    }

    @Override
    public IModelTextureVariantsSupplier.IModelTextureVariants getTextureVariantsFor(ObjObjectRenderer objObjectRenderer) {
        //Here we can make difference between tyre and rim textures
        return getVariants();
    }

    @Override
    public boolean hasVaryingTextures() {
        return getSubPropertyByType(MaterialVariantsInfo.class) != null;
    }

    public String getPartName() {
        return partName;
    }

    @Override
    public ResourceLocation getModel() {
        return model;
    }

    public float getWheelWidth() {
        return wheelWidth;
    }

    public float getWheelRadius() {
        return wheelRadius;
    }

    public float getRimRadius() {
        return rimRadius;
    }

    public float getWheelFriction() {
        return wheelFriction;
    }

    public float getWheelBrakeForce() {
        return wheelBrakeForce;
    }

    public float getHandBrakeForce() {
        return handBrakeForce;
    }

    public float getWheelRollInInfluence() {
        return wheelRollInInfluence;
    }

    public float getSuspensionRestLength() {
        return suspensionRestLength;
    }

    public float getSuspensionStiffness() {
        return suspensionStiffness;
    }

    public float getSuspensionMaxForce() {
        return suspensionMaxForce;
    }

    public float getWheelsDampingRelaxation() {
        return wheelsDampingRelaxation;
    }

    public float getWheelsDampingCompression() {
        return wheelsDampingCompression;
    }

    public Vector3f getScaleModifier() {
        return scaleModifier;
    }

    public EnumParticleTypes getSkidParticle() {
        return skidParticle;
    }

    @Override
    public String toString() {
        return "PartWheelInfo named " + getFullName();
    }
}