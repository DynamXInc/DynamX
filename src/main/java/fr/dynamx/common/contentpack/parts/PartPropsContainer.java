package fr.dynamx.common.contentpack.parts;

import com.jme3.math.Vector3f;
import fr.dynamx.api.contentpack.object.part.BasePart;
import fr.dynamx.api.contentpack.object.part.IShapeInfo;
import fr.dynamx.api.entities.modules.ModuleListBuilder;
import fr.dynamx.common.contentpack.loader.ModularVehicleInfoBuilder;
import fr.dynamx.common.entities.BaseVehicleEntity;
import fr.dynamx.common.entities.modules.PropsContainerModule;
import fr.dynamx.utils.optimization.MutableBoundingBox;

public class PartPropsContainer extends BasePart<ModularVehicleInfoBuilder> implements IShapeInfo {
    protected MutableBoundingBox box;

    public PartPropsContainer(ModularVehicleInfoBuilder owner, String partName) {
        super(owner, partName);
    }

    @Override
    public void appendTo(ModularVehicleInfoBuilder vehicleInfo) {
        super.appendTo(vehicleInfo);
        Vector3f min = getPosition().subtract(getScale());
        Vector3f max = getPosition().add(getScale());
        this.box = new MutableBoundingBox(
                min.x, min.y, min.z,
                max.x, max.y, max.z);
    }

    @Override
    public void addModules(BaseVehicleEntity<?> entity, ModuleListBuilder modules) {
        if (!modules.hasModuleOfClass(PropsContainerModule.class)) {
            modules.add(new PropsContainerModule(entity));
        }
    }

    @Override
    public String getName() {
        return "PartPropsContainer named " + getPartName() + " in " + getOwner().getName();
    }

    @Override
    public Vector3f getSize() {
        return getScale();
    }

    public MutableBoundingBox getBox() {
        return box;
    }
}
