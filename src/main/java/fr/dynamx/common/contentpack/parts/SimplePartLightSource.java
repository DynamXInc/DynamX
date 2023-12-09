package fr.dynamx.common.contentpack.parts;

import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import fr.aym.acslib.api.services.error.ErrorLevel;
import fr.dynamx.api.contentpack.object.subinfo.ISubInfoType;
import fr.dynamx.api.contentpack.object.subinfo.ISubInfoTypeOwner;
import fr.dynamx.api.contentpack.registry.*;
import fr.dynamx.utils.errors.DynamXErrorManager;
import lombok.Getter;
import lombok.Setter;

import javax.annotation.Nullable;

/**
 * Shorthand to create a {@link PartLightSource} with only one {@link LightObject} <br>
 * A warning will be thrown if you add multiple simple lights on the same 3D model object
 */
@RegisteredSubInfoType(name = "light", registries = {SubInfoTypeRegistries.WHEELED_VEHICLES, SubInfoTypeRegistries.HELICOPTER, SubInfoTypeRegistries.BLOCKS, SubInfoTypeRegistries.PROPS}, strictName = false)
public class SimplePartLightSource extends LightObject implements ISubInfoType<ILightOwner<?>> {
    @IPackFilePropertyFixer.PackFilePropertyFixer(registries = {SubInfoTypeRegistries.WHEELED_VEHICLES, SubInfoTypeRegistries.HELICOPTER})
    public static final IPackFilePropertyFixer PROPERTY_FIXER = (object, key, value) -> {
        if ("ShapePosition".equals(key))
            return new IPackFilePropertyFixer.FixResult("Position", true);
        if (key.equals("PartName"))
            return new IPackFilePropertyFixer.FixResult("ObjectName", false);
        return null;
    };

    private final ILightOwner<?> owner;
    private final String name;

    @Getter
    @Setter
    @PackFileProperty(configNames = "ObjectName", description = "PartLightSource.PartName")
    protected String objectName;
    @Getter
    @Setter
    @PackFileProperty(configNames = "BaseMaterial", required = false, description = "PartLightSource.BaseMaterial")
    protected String baseMaterial = "default";
    @Getter
    @Setter
    @PackFileProperty(configNames = "Position", type = DefinitionType.DynamXDefinitionTypes.VECTOR3F_INVERSED_Y, description = "common.position", required = false)
    protected Vector3f position;
    @Getter
    @Setter
    @PackFileProperty(configNames = "Rotation", required = false, defaultValue = "none", description = "PartLightSource.Rotation")
    protected Quaternion rotation = new Quaternion();
    @Getter
    @Setter
    @PackFileProperty(configNames = "DependsOnNode", required = false, description = "PartLightSource.DependsOnNode")
    protected String nodeDependingOnName;

    public SimplePartLightSource(ISubInfoTypeOwner<ILightOwner<?>> owner, String name) {
        this.owner = (ILightOwner<?>) owner;
        this.name = name;
    }

    @Override
    public void appendTo(ILightOwner<?> owner) {
        hashLightId();
        PartLightSource existing = owner.getLightSource(objectName);
        if (existing != null)
            DynamXErrorManager.addPackError(getPackName(), "deprecated_light_format", ErrorLevel.LOW, owner.getName(), "Light named " + name);
        boolean add = false;
        if (existing == null) {
            existing = new PartLightSource((ISubInfoTypeOwner<ILightOwner<?>>) owner, this.name);
            existing.objectName = objectName;
            existing.baseMaterial = baseMaterial;
            existing.position = position;
            existing.rotation = rotation;
            existing.nodeDependingOnName = nodeDependingOnName;
            add = true;
        }
        existing.addLightSource(this);
        if (add)
            existing.appendTo(owner);
    }

    @Nullable
    @Override
    public ILightOwner<?> getOwner() {
        return owner;
    }

    @Override
    public String getName() {
        return "LightSource_" + name;
    }

    @Override
    public String getPackName() {
        return owner.getPackName();
    }
}
