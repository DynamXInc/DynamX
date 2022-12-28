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

        childFloatsPos.clear();
        AxisAlignedBB floaterBoundingBox = box;
        Vector3f pos;
        switch (axis) {
            case 0:
                for (int i = 0; i < lineSize.x; i++) {
                    pos = new Vector3f().set(getPosition());
                    float xPos = (float) (floaterBoundingBox.minX + i * (size + spacing.x) + offset.x);
                    pos.addLocal(xPos + size / 2, 0, 0);
                    childFloatsPos.add(pos);
                }
                break;
            case 2:
                for (int j = 0; j < lineSize.z; j++) {
                    pos = new Vector3f().set(getPosition());
                    float zPos = (float) (floaterBoundingBox.minZ + j * (size + spacing.z) + offset.z);
                    pos.addLocal(0, 0, -zPos - size / 2);
                    childFloatsPos.add(pos);
                }
                break;
            case 3:
                for (int i = 0; i < lineSize.x; i++) {
                    for (int j = 0; j < lineSize.z; j++) {
                        pos = new Vector3f().set(getPosition());
                        float xPos = (float) (floaterBoundingBox.minX + i * (size + spacing.x) + offset.x);
                        float zPos = (float) (floaterBoundingBox.minZ + j * (size + spacing.z) + offset.z);
                        pos.addLocal(xPos + size / 2, 0, zPos + size / 2);
                        childFloatsPos.add(pos);
                    }
                }
                break;
            default:
                pos = getPosition();
                childFloatsPos.add(pos);
                break;
        }
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
