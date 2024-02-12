package fr.dynamx.common.contentpack.parts.lights;

import fr.dynamx.api.contentpack.object.subinfo.ISubInfoType;
import fr.dynamx.api.contentpack.object.subinfo.ISubInfoTypeOwner;
import fr.dynamx.api.contentpack.registry.PackFileProperty;
import fr.dynamx.api.contentpack.registry.RegisteredSubInfoType;
import fr.dynamx.api.contentpack.registry.SubInfoTypeRegistries;
import lombok.Getter;

import javax.annotation.Nullable;

@RegisteredSubInfoType(name = "VolumetricLight", registries = {SubInfoTypeRegistries.LIGHTS, SubInfoTypeRegistries.WHEELED_VEHICLES, SubInfoTypeRegistries.HELICOPTER, SubInfoTypeRegistries.BLOCKS, SubInfoTypeRegistries.PROPS, SubInfoTypeRegistries.ITEMS, SubInfoTypeRegistries.ARMORS}, strictName = false)
public class VolumetricLightObject implements ISubInfoType<SpotLightObject> {
    public SpotLightObject owner;

    @Getter
    @PackFileProperty(configNames = "SampleCount", defaultValue = "20", required = false)
    protected int sampleCount = 20;

    @Getter
    @PackFileProperty(configNames = "Scattering", defaultValue = "0.1", required = false)
    protected float scattering = 0.5f;

    @Getter
    @PackFileProperty(configNames = "Intensity", defaultValue = "20", required = false)
    protected float intensity = 20f;


    public VolumetricLightObject(ISubInfoTypeOwner<SpotLightObject> owner) {
        this.owner = (SpotLightObject) owner;
    }

    @Override
    public void appendTo(SpotLightObject owner) {
        owner.addVolumetricLight(this);
    }

    @Override
    public String getName() {
        return "VolumetricLight in " + owner.getName();
    }

    @Override
    public String getPackName() {
        return owner.getPackName();
    }

    @Nullable
    @Override
    public SpotLightObject getOwner() {
        return owner;
    }
}