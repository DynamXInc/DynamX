package fr.dynamx.common.contentpack.parts;

import com.jme3.math.Vector3f;
import fr.dynamx.api.contentpack.object.part.BasePart;
import fr.dynamx.api.contentpack.object.part.IShapeInfo;
import fr.dynamx.api.contentpack.registry.RegisteredSubInfoType;
import fr.dynamx.api.contentpack.registry.SubInfoTypeRegistries;
import fr.dynamx.api.entities.modules.ModuleListBuilder;
import fr.dynamx.common.contentpack.type.vehicle.ModularVehicleInfo;
import fr.dynamx.common.entities.BaseVehicleEntity;
import fr.dynamx.common.entities.PackPhysicsEntity;
import fr.dynamx.common.entities.modules.PropsContainerModule;
import fr.dynamx.utils.debug.DynamXDebugOption;
import fr.dynamx.utils.debug.DynamXDebugOptions;
import fr.dynamx.utils.optimization.MutableBoundingBox;

@RegisteredSubInfoType(name = "propscontainer", registries = {SubInfoTypeRegistries.WHEELED_VEHICLES, SubInfoTypeRegistries.HELICOPTER})
public class PartPropsContainer extends BasePart<ModularVehicleInfo> implements IShapeInfo {
    protected MutableBoundingBox box;

    public PartPropsContainer(ModularVehicleInfo owner, String partName) {
        super(owner, partName);
    }

    @Override
    public void appendTo(ModularVehicleInfo owner) {
        super.appendTo(owner);
        Vector3f min = getPosition().subtract(getScale());
        Vector3f max = getPosition().add(getScale());
        this.box = new MutableBoundingBox(
                min.x, min.y, min.z,
                max.x, max.y, max.z);
    }

    @Override
    public void addModules(PackPhysicsEntity<?, ?> entity, ModuleListBuilder modules) {
        if (!modules.hasModuleOfClass(PropsContainerModule.class)) {
            modules.add(new PropsContainerModule((BaseVehicleEntity<?>) entity));
        }
    }

    @Override
    public String getName() {
        return "PartPropsContainer named " + getPartName();
    }

    @Override
    public Vector3f getSize() {
        return getScale();
    }

    public MutableBoundingBox getBox() {
        return box;
    }

    @Override
    public DynamXDebugOption getDebugOption() {
        return DynamXDebugOptions.PROPS_CONTAINERS;
    }
}
