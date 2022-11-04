package fr.dynamx.common.contentpack.parts;

import com.jme3.math.Vector3f;
import fr.dynamx.api.contentpack.object.part.BasePart;
import fr.dynamx.api.contentpack.registry.RegisteredSubInfoType;
import fr.dynamx.api.contentpack.registry.SubInfoTypeRegistries;
import fr.dynamx.common.contentpack.type.vehicle.ModularVehicleInfo;
import fr.dynamx.utils.debug.DynamXDebugOption;
import fr.dynamx.utils.debug.DynamXDebugOptions;
import net.minecraft.util.math.AxisAlignedBB;

import java.util.ArrayList;
import java.util.List;

@RegisteredSubInfoType(name = "float", registries = SubInfoTypeRegistries.WHEELED_VEHICLES, strictName = false)
public class PartFloat extends BasePart<ModularVehicleInfo> {
    public AxisAlignedBB box;
    public List<Vector3f> listFloaters = new ArrayList<>();
    public float size;

    public PartFloat(ModularVehicleInfo owner, String partName) {
        super(owner, partName);
    }

    @Override
    public void appendTo(ModularVehicleInfo vehicleInfo) {
        super.appendTo(vehicleInfo);
        Vector3f min = getPosition().subtract(getScale());
        Vector3f max = getPosition().add(getScale());
        this.box = new AxisAlignedBB(
                min.x, min.y, min.z,
                max.x, max.y, max.z);
    }
    @Override
    public DynamXDebugOption getDebugOption() {
        return DynamXDebugOptions.WHEELS;
    }

    @Override
    public String getName() {
        return "PartFloat named " + getPartName();
    }
}
