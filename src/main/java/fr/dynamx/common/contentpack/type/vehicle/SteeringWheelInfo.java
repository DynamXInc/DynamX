package fr.dynamx.common.contentpack.type.vehicle;

import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import fr.aym.acslib.api.services.ErrorTrackingService;
import fr.dynamx.api.contentpack.object.subinfo.ISubInfoTypeOwner;
import fr.dynamx.api.contentpack.object.subinfo.SubInfoType;
import fr.dynamx.api.contentpack.registry.DefinitionType;
import fr.dynamx.api.contentpack.registry.PackFileProperty;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.contentpack.loader.ModularVehicleInfoBuilder;
import fr.dynamx.utils.DynamXLoadingTasks;

/**
 * Info of the steering wheel of a {@link ModularVehicleInfoBuilder}
 */
public class SteeringWheelInfo extends SubInfoType<ModularVehicleInfoBuilder>
{
    @PackFileProperty(configNames = "PartName", required = false, defaultValue = "SteeringWheel")
    private String partName = "SteeringWheel";
    @PackFileProperty(configNames = "BaseRotation", required = false, newConfigName = "BaseRotationQuat")
    private float[] deprecatedBaseRotation;
    @PackFileProperty(configNames = "BaseRotationQuat", required = false, defaultValue = "0 0 0 1")
    private Quaternion steeringWheelBaseRotation = null;
    @PackFileProperty(configNames = "Position", type = DefinitionType.DynamXDefinitionTypes.VECTOR3F_INVERSED_Y)
    private Vector3f position = new Vector3f(0.5f, 1.1f, 1);

    public SteeringWheelInfo(ISubInfoTypeOwner<ModularVehicleInfoBuilder> owner) {
        super(owner);
    }

    @Override
    public void appendTo(ModularVehicleInfoBuilder owner) {
        owner.addSubProperty(this);
        getSteeringWheelPosition().multLocal(owner.getScaleModifier());
        owner.addRenderedParts(getPartName());
    }

    @Override
    public void onComplete(boolean hotReload) {
        if(deprecatedBaseRotation != null)
            deprecatedBaseRotation[0] *= 180f/Math.PI; //Convert to degrees
        if(steeringWheelBaseRotation != null && deprecatedBaseRotation != null) {
            DynamXContext.getErrorTracker().addError(DynamXLoadingTasks.PACK, getPackName(), "Bad steering wheel BaseRotation property", "You should use BaseRotationQuat property and remove BaseRotation usage !", ErrorTrackingService.TrackedErrorLevel.HIGH);
        }
    }

    public String getPartName() {
        return partName;
    }

    public Quaternion getSteeringWheelBaseRotation() {
        return steeringWheelBaseRotation;
    }

    public float[] getDeprecatedBaseRotation() {
        return deprecatedBaseRotation;
    }

    public Vector3f getSteeringWheelPosition() {
        return position;
    }

    @Override
    public String getName() {
        return "SteeringWheel in "+getOwner().getName();
    }
}
