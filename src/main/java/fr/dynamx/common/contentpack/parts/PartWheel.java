package fr.dynamx.common.contentpack.parts;

import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import fr.dynamx.api.contentpack.object.part.InteractivePart;
import fr.dynamx.api.contentpack.registry.DefinitionType;
import fr.dynamx.api.contentpack.registry.PackFileProperty;
import fr.dynamx.api.entities.IModuleContainer;
import fr.dynamx.common.contentpack.loader.ModularVehicleInfoBuilder;
import fr.dynamx.common.contentpack.type.PartWheelInfo;
import fr.dynamx.common.entities.BaseVehicleEntity;
import fr.dynamx.common.entities.modules.WheelsModule;
import fr.dynamx.common.physics.entities.modules.WheelsPhysicsHandler;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.AxisAlignedBB;

public class PartWheel extends InteractivePart<BaseVehicleEntity<?>, ModularVehicleInfoBuilder> {
    @PackFileProperty(configNames = "IsRight", oldNames = "isRight")
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
    private final Quaternion suspensionAxis = new Quaternion();

    private PartWheelInfo defaultWheelInfo;

    public PartWheel(ModularVehicleInfoBuilder owner, String partName) {
        super(owner, partName, 0.75f, 0.75f);
    }

    @Override
    public void appendTo(ModularVehicleInfoBuilder owner) {
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
    public void onComplete(boolean hotReload) {
        //suspensionAxis[0] *= 180f/Math.PI; //Convert to degrees
    }

    @Override
    public void addPart(BaseVehicleEntity<?> vehicle) {
        if (!(vehicle instanceof IModuleContainer.IPropulsionContainer) || !(((IModuleContainer.IPropulsionContainer) vehicle).getPropulsion() instanceof WheelsModule))
            throw new IllegalStateException("The entity " + vehicle + " has PartWheels, but does not implement IHavePropulsion or the propulsion is not a WheelsModule !");
        ((WheelsPhysicsHandler) ((IModuleContainer.IPropulsionContainer) vehicle).getPropulsion().getPhysicsHandler()).addWheel(this, getDefaultWheelInfo());
    }

    @Override
    public void removePart(BaseVehicleEntity<?> vehicle) {
        ((WheelsPhysicsHandler) ((IModuleContainer.IPropulsionContainer) vehicle).getPropulsion().getPhysicsHandler()).removeWheel(getId());
    }

    @Override
    public boolean interact(BaseVehicleEntity<?> entity, EntityPlayer with) {
        return false;
    }

    public void setDefaultWheelInfo(ModularVehicleInfoBuilder vehicleInfoBuilder, PartWheelInfo partWheelInfo) {
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
        return "PartWheel named " + getPartName() + " in " + getOwner().getName();
    }
}
