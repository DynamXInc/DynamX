package fr.dynamx.common.contentpack.parts;

import com.jme3.math.Vector3f;
import fr.dynamx.api.contentpack.object.part.BasePart;
import fr.dynamx.api.contentpack.registry.PackFileProperty;
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
    public float size = 1;

    @PackFileProperty(configNames = "DragCoefficient", required = false)
    public float dragCoefficient = 0.05f;
    @PackFileProperty(configNames = "Axis", required = false)
    public int axis;
    @PackFileProperty(configNames = "Offset", required = false)
    public Vector3f offset = new Vector3f();
    @PackFileProperty(configNames = "LineSize", required = false)
    public Vector3f lineSize = new Vector3f();
    @PackFileProperty(configNames = "Spacing", required = false)
    public Vector3f spacing = new Vector3f();
    public List<Vector3f> childFloatsPos = new ArrayList<>();

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
