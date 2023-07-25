package fr.dynamx.common.contentpack.parts;

import com.jme3.math.Vector3f;
import fr.dynamx.api.contentpack.object.part.BasePart;
import fr.dynamx.api.contentpack.registry.PackFileProperty;
import fr.dynamx.api.contentpack.registry.RegisteredSubInfoType;
import fr.dynamx.api.contentpack.registry.SubInfoTypeRegistries;
import fr.dynamx.common.contentpack.type.vehicle.ModularVehicleInfo;
import fr.dynamx.utils.debug.DynamXDebugOption;
import fr.dynamx.utils.debug.DynamXDebugOptions;
import fr.dynamx.utils.optimization.Vector3fPool;
import fr.dynamx.utils.physics.DynamXPhysicsHelper;
import lombok.Getter;
import net.minecraft.util.math.AxisAlignedBB;

import java.util.ArrayList;
import java.util.List;

@RegisteredSubInfoType(name = "float", registries = SubInfoTypeRegistries.WHEELED_VEHICLES, strictName = false)
public class PartFloat extends BasePart<ModularVehicleInfo> {
    @Getter
    protected AxisAlignedBB box;
    @Getter
    protected float size = 1;

    @Getter
    @PackFileProperty(configNames = "BuoyCoefficient", required = false)
    protected float buoyCoefficient = 1f;
    @Getter
    @PackFileProperty(configNames = "DragCoefficient", required = false)
    protected float dragCoefficient = 0.05f;
    @Getter
    @PackFileProperty(configNames = "Axis", required = false)
    protected DynamXPhysicsHelper.EnumPhysicsAxis axis;
    @Getter
    @PackFileProperty(configNames = "Offset", required = false)
    protected Vector3f offset = new Vector3f();
    @Getter
    @PackFileProperty(configNames = "LineSize", required = false)
    protected Vector3f lineSize = new Vector3f();
    @Getter
    @PackFileProperty(configNames = "Spacing", required = false)
    protected Vector3f spacing = new Vector3f();
    @Getter
    protected List<Vector3f> childrenPositionList = new ArrayList<>();

    public PartFloat(ModularVehicleInfo owner, String partName) {
        super(owner, partName);
    }

    @Override
    public void appendTo(ModularVehicleInfo owner) {
        super.appendTo(owner);
        Vector3f min = getPosition().subtract(getScale());
        Vector3f max = getPosition().add(getScale());
        this.box = new AxisAlignedBB(
                min.x, min.y, min.z,
                max.x, max.y, max.z);

        childrenPositionList.clear();
        switch (axis) {
            case X:
                for (int i = 0; i < lineSize.x; i++) {
                    float xPos = (float) (box.minX + i * (size + spacing.x) + offset.x);
                    childrenPositionList.add(Vector3fPool.getPermanentVector(getPosition()).addLocal(xPos + size / 2, 0, 0));
                }
                break;
            case Y:
                for (int j = 0; j < lineSize.z; j++) {
                    float zPos = (float) (box.minZ + j * (size + spacing.z) + offset.z);
                    childrenPositionList.add(Vector3fPool.getPermanentVector(getPosition()).addLocal(0, 0, zPos + size / 2));
                }
                break;
            case Z:
                for (int i = 0; i < lineSize.x; i++) {
                    for (int j = 0; j < lineSize.z; j++) {
                        float xPos = (float) (box.minX + i * (size + spacing.x) + offset.x);
                        float zPos = (float) (box.minZ + j * (size + spacing.z) + offset.z);
                        childrenPositionList.add(Vector3fPool.getPermanentVector(getPosition()).addLocal(xPos + size / 2, 0, zPos + size / 2));
                    }
                }
                break;
            default:
                childrenPositionList.add(getPosition());
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
