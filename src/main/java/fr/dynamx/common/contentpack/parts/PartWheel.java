package fr.dynamx.common.contentpack.parts;

import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import fr.dynamx.api.contentpack.object.part.InteractivePart;
import fr.dynamx.api.contentpack.registry.*;
import fr.dynamx.common.contentpack.type.vehicle.ModularVehicleInfo;
import fr.dynamx.common.contentpack.type.vehicle.PartWheelInfo;
import fr.dynamx.common.entities.BaseVehicleEntity;
import fr.dynamx.utils.debug.DynamXDebugOption;
import fr.dynamx.utils.debug.DynamXDebugOptions;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.AxisAlignedBB;

@RegisteredSubInfoType(name = "wheel", registries = SubInfoTypeRegistries.WHEELED_VEHICLES, strictName = false)
public class PartWheel extends InteractivePart<BaseVehicleEntity<?>, ModularVehicleInfo> {
    @IPackFilePropertyFixer.PackFilePropertyFixer(registries = SubInfoTypeRegistries.WHEELED_VEHICLES)
    public static final IPackFilePropertyFixer PROPERTY_FIXER = (object, key, value) -> {
        if ("isRight".equals(key))
            return new IPackFilePropertyFixer.FixResult("IsRight", true);
        return null;
    };

    @PackFileProperty(configNames = "IsRight")
    private boolean isRight;
    @PackFileProperty(configNames = "IsSteerable")
    private boolean wheelIsSteerable;
    @PackFileProperty(configNames = "MaxTurn")
    private float wheelMaxTurn;
    @PackFileProperty(configNames = "DrivingWheel")
    private boolean drivingWheel;
    @PackFileProperty(configNames = "HandBrakingWheel", required = false)
    private boolean handBrakingWheel;
    @PackFileProperty(configNames = "AttachedWheel")
    private String defaultWheelName;
    @PackFileProperty(configNames = "MudGuard", required = false)
    private String mudGuardPartName;
    @PackFileProperty(configNames = "RotationPoint", required = false, type = DefinitionType.DynamXDefinitionTypes.VECTOR3F_INVERSED_Y)
    private Vector3f rotationPoint;
    @PackFileProperty(configNames = "SuspensionAxis", required = false)
    private Quaternion suspensionAxis = new Quaternion();

    private PartWheelInfo defaultWheelInfo;

    public PartWheel(ModularVehicleInfo owner, String partName) {
        super(owner, partName, 0.75f, 0.75f);
    }

    @Override
    public void appendTo(ModularVehicleInfo owner) {
        super.appendTo(owner);
        if (getRotationPoint() == null)
            rotationPoint = getPosition();
        else
            getRotationPoint().multLocal(getScaleModifier(owner));
        owner.arrangeWheelID(this);
        if (getMudGuardPartName() != null)
            owner.addRenderedParts(getMudGuardPartName());
    }

    @Override
    public DynamXDebugOption getDebugOption() {
        return DynamXDebugOptions.WHEELS;
    }

    @Override
    public boolean interact(BaseVehicleEntity<?> entity, EntityPlayer with) {
        return false;
    }

    public void setDefaultWheelInfo(PartWheelInfo partWheelInfo) {
        if (partWheelInfo == null) {
            throw new IllegalArgumentException("Attached wheel info " + getDefaultWheelName() + " was not found !");
        } else {
            defaultWheelInfo = partWheelInfo;
            setBox(new AxisAlignedBB(-partWheelInfo.getWheelWidth(), -partWheelInfo.getWheelRadius(), -partWheelInfo.getWheelRadius(),
                    partWheelInfo.getWheelWidth(), partWheelInfo.getWheelRadius(), partWheelInfo.getWheelRadius()));
        }
    }

    public PartWheelInfo getDefaultWheelInfo() {
        return defaultWheelInfo;
    }

    public boolean isRight() {
        return isRight;
    }

    public boolean isWheelIsSteerable() {
        return wheelIsSteerable;
    }

    public float getWheelMaxTurn() {
        return wheelMaxTurn;
    }

    public boolean isDrivingWheel() {
        return drivingWheel;
    }

    public String getDefaultWheelName() {
        return defaultWheelName;
    }

    public String getMudGuardPartName() {
        return mudGuardPartName;
    }

    public Vector3f getRotationPoint() {
        return rotationPoint;
    }

    public Quaternion getSuspensionAxis() {
        return suspensionAxis;
    }

    public boolean isHandBrakingWheel() {
        return handBrakingWheel;
    }

    public void setHandBrakingWheel(boolean handBrakingWheel) {
        this.handBrakingWheel = handBrakingWheel;
    }

    @Override
    public String getName() {
        return "PartWheel named " + getPartName();
    }
}
