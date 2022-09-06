package fr.dynamx.common.contentpack.parts;

import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import fr.dynamx.api.contentpack.object.subinfo.ISubInfoType;
import fr.dynamx.api.contentpack.object.subinfo.ISubInfoTypeOwner;
import fr.dynamx.api.contentpack.registry.*;
import fr.dynamx.api.entities.modules.ModuleListBuilder;
import fr.dynamx.common.contentpack.loader.ModularVehicleInfoBuilder;
import fr.dynamx.common.entities.BaseVehicleEntity;
import fr.dynamx.common.entities.modules.VehicleLightsModule;
import fr.dynamx.common.obj.texture.TextureData;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RegisteredSubInfoType(name = "light", registries = SubInfoTypeRegistries.WHEELED_VEHICLES, strictName = false)
public class PartLightSource implements ISubInfoType<ModularVehicleInfoBuilder> {
    @IPackFilePropertyFixer.PackFilePropertyFixer(registries = SubInfoTypeRegistries.WHEELED_VEHICLES)
    public static final IPackFilePropertyFixer PROPERTY_FIXER = (object, key, value) -> {
        if ("ShapePosition".equals(key))
            return new IPackFilePropertyFixer.FixResult("Position", true);
        return null;
    };

    private final ModularVehicleInfoBuilder owner;
    private final String name;

    @PackFileProperty(configNames = "Position", type = DefinitionType.DynamXDefinitionTypes.VECTOR3F_INVERSED_Y, description = "common.position", required = false)
    private Vector3f position;
    @PackFileProperty(configNames = "Rotation", required = false, defaultValue = "1 0 0 0")
    private Quaternion rotation = new Quaternion();
    @PackFileProperty(configNames = "LightId")
    private int lightId;
    @PackFileProperty(configNames = "PartName")
    private String partName;
    @PackFileProperty(configNames = "Textures", required = false)
    private String[] textures;
    @PackFileProperty(configNames = "Colors", required = false, type = DefinitionType.DynamXDefinitionTypes.VECTOR3F_ARRAY_ORDERED)
    private Vector3f[] colors;
    @PackFileProperty(configNames = "BlinkSequenceTicks", required = false)
    private int[] blinkSequence;
    @PackFileProperty(configNames = "RotateDuration", required = false)
    private int rotateDuration;

    public PartLightSource(ISubInfoTypeOwner<ModularVehicleInfoBuilder> owner, String name) {
        this.owner = (ModularVehicleInfoBuilder) owner;
        this.name = name;
    }

    @Override
    public void appendTo(ModularVehicleInfoBuilder owner) {
        owner.addLightSource(this);
        owner.addRenderedParts(getPartName());
    }

    @Nullable
    @Override
    public ModularVehicleInfoBuilder getOwner() {
        return owner;
    }

    @Override
    public void addModules(BaseVehicleEntity<?> entity, ModuleListBuilder modules) {
        if (!modules.hasModuleOfClass(VehicleLightsModule.class)) {
            modules.add(new VehicleLightsModule(entity));
        }
    }

    public String getLightName() {
        return name;
    }

    @Override
    public String getName() {
        return "LightSource_" + name;
    }

    @Override
    public String getPackName() {
        return owner.getPackName();
    }

    public int getLightId() {
        return lightId;
    }

    public String getPartName() {
        return partName;
    }

    public String[] getTextures() {
        return textures;
    }

    public int[] getBlinkSequence() {
        return blinkSequence;
    }

    public Vector3f getPosition() {
        return position;
    }

    public Quaternion getRotation() {
        return rotation;
    }

    public int getRotateDuration() {
        return rotateDuration;
    }

    private final Map<Integer, TextureData> textureMap = new HashMap<>();

    public void mapTexture(int blinkStep, TextureData textureData) {
        textureMap.put(blinkStep, textureData);
    }

    public Map<Integer, TextureData> getTextureMap() {
        return textureMap;
    }

    public Vector3f[] getColors() {
        return colors;
    }

    public static class CompoundLight {
        private final String partName;
        private final List<PartLightSource> sources = new ArrayList<>();

        public CompoundLight(PartLightSource part) {
            this.partName = part.getPartName();
            addSource(part);
        }

        public void addSource(PartLightSource source) {
            sources.add(source);
        }

        public String getPartName() {
            return partName;
        }

        public List<PartLightSource> getSources() {
            return sources;
        }
    }
}
