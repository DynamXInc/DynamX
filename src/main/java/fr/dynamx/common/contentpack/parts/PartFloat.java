package fr.dynamx.common.contentpack.parts;

import com.jme3.math.Vector3f;
import fr.dynamx.api.contentpack.object.part.BasePart;
import fr.dynamx.api.contentpack.object.subinfo.ISubInfoTypeOwner;
import fr.dynamx.api.contentpack.registry.PackFileProperty;
import fr.dynamx.api.contentpack.registry.RegisteredSubInfoType;
import fr.dynamx.api.contentpack.registry.SubInfoTypeRegistries;
import fr.dynamx.utils.debug.DynamXDebugOption;
import fr.dynamx.utils.debug.DynamXDebugOptions;
import fr.dynamx.utils.optimization.Vector3fPool;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.util.math.AxisAlignedBB;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@RegisteredSubInfoType(name = "float", registries = {SubInfoTypeRegistries.WHEELED_VEHICLES, SubInfoTypeRegistries.PROPS}, strictName = false)
public class PartFloat<T extends ISubInfoTypeOwner<T>> extends BasePart<T> {
    protected AxisAlignedBB box;
    protected float size = 1;

    @PackFileProperty(configNames = "BuoyCoefficient", required = false, defaultValue = "1")
    protected float buoyCoefficient = 1f;
    @PackFileProperty(configNames = "DragCoefficient", required = false, defaultValue = "0.05")
    protected float dragCoefficient = 0.05f;
    @PackFileProperty(configNames = "Offset", required = false)
    protected Vector3f offset = new Vector3f();
    @PackFileProperty(configNames = "LineSize", required = false)
    protected Vector3f lineSize = new Vector3f();
    @PackFileProperty(configNames = "Spacing", required = false)
    protected Vector3f spacing = new Vector3f();
    protected List<Vector3f> childrenPositionList = new ArrayList<>();

    public PartFloat(ISubInfoTypeOwner<T> owner, String partName) {
        super(owner, partName);
    }

    @Override
    public void appendTo(T owner) {
        super.appendTo(owner);
        Vector3f min = getPosition().subtract(getScale());
        Vector3f max = getPosition().add(getScale());
        this.box = new AxisAlignedBB(
                min.x, min.y, min.z,
                max.x, max.y, max.z);

        childrenPositionList.clear();
        int lSizeX = (int) Math.max(1, lineSize.x);
        int lSizeZ = (int) Math.max(1, lineSize.z);
        for (int i = 0; i < lSizeX; i++) {
            for (int j = 0; j < lSizeZ; j++) {
                float xPos = (float) (box.minX + i * (size + spacing.x) + offset.x);
                float zPos = (float) (box.minZ + j * (size + spacing.z) + offset.z);
                childrenPositionList.add(Vector3fPool.getPermanentVector(getPosition()).addLocal(xPos + size / 2, 0, zPos + size / 2));
            }
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
