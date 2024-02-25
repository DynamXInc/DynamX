package fr.dynamx.common.contentpack.parts.lights;

import com.jme3.math.Vector3f;
import fr.dynamx.api.contentpack.object.subinfo.ISubInfoType;
import fr.dynamx.api.contentpack.object.subinfo.ISubInfoTypeOwner;
import fr.dynamx.api.contentpack.object.subinfo.SubInfoType;
import fr.dynamx.api.contentpack.registry.DefinitionType;
import fr.dynamx.api.contentpack.registry.PackFileProperty;
import fr.dynamx.api.contentpack.registry.RegisteredSubInfoType;
import fr.dynamx.api.contentpack.registry.SubInfoTypeRegistries;
import fr.dynamx.common.contentpack.parts.LightObject;
import fr.dynamx.common.contentpack.parts.PartLightSource;
import lombok.Getter;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@RegisteredSubInfoType(name = "SpotLight", registries = {SubInfoTypeRegistries.LIGHTS, SubInfoTypeRegistries.WHEELED_VEHICLES, SubInfoTypeRegistries.HELICOPTER, SubInfoTypeRegistries.BLOCKS,SubInfoTypeRegistries.PROPS, SubInfoTypeRegistries.ITEMS, SubInfoTypeRegistries.ARMORS}, strictName = false)
public class SpotLightObject extends SubInfoType<PartLightSource> implements ISubInfoTypeOwner<SpotLightObject>
{
    @Getter
    @PackFileProperty(configNames = "Offset", type = DefinitionType.DynamXDefinitionTypes.VECTOR3F_INVERSED_Y, description = "common.position")
    protected Vector3f offset;
    @Getter
    @PackFileProperty(configNames = "Rotation", type = DefinitionType.DynamXDefinitionTypes.VECTOR3F, description = "todo")
    protected Vector3f rotation = new Vector3f();
    @Getter
    @PackFileProperty(configNames = "InnerAngle", defaultValue = "10")
    protected int innerAngle = 10;
    @Getter
    @PackFileProperty(configNames = "OuterAngle", defaultValue = "20")
    protected int outerAngle = 20;

    @Getter
    @PackFileProperty(configNames = "Distance", defaultValue = "20", required = false)
    protected float distance = 20;
    @Getter
    @PackFileProperty(configNames = "Intensity", defaultValue = "20", required = false)
    protected float intensity = 20;
    @Getter
    @PackFileProperty(configNames = "Falloff", defaultValue = "0.1f", required = false)
    protected float falloff = 0.1f;

    @Getter
    @PackFileProperty(configNames = "SpotLightColor", required = false)
    protected Vector3f spotLightColor = new Vector3f(1,1,1);

    @Getter
    private final List<VolumetricLightObject> volumetricLightObjects = new ArrayList<>();

    public SpotLightObject(ISubInfoTypeOwner<PartLightSource> owner) {
        super(owner);
    }

    @Override
    public String getName() {
        return "SpotLight in " + owner.getName();
    }

    @Override
    public String getPackName() {
        return owner.getPackName();
    }

    @Override
    public void appendTo(PartLightSource owner) {
        owner.addSpotLight(this);
    }


    public void addVolumetricLight(VolumetricLightObject object) {
        volumetricLightObjects.add(object);
    }

    @Override
    public void addSubProperty(ISubInfoType<SpotLightObject> property) {

    }

    @Override
    public List<ISubInfoType<SpotLightObject>> getSubProperties() {
        return null;
    }

    public List<LightObject> getLightObjects() {
        return owner.getSources();
    }
}