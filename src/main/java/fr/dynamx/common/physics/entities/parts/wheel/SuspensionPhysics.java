package fr.dynamx.common.physics.entities.parts.wheel;

import com.jme3.bullet.objects.VehicleWheel;
import fr.dynamx.common.contentpack.parts.PartWheel;
import lombok.Getter;

public class SuspensionPhysics {
    @Getter
    private float stiffness;
    @Getter
    private float compression;
    @Getter
    private float dampness;
    @Getter
    private float maxForce;
    @Getter
    private float restLength;

    private final VehicleWheel vehicleWheel;

    public SuspensionPhysics(VehicleWheel vehicleWheel, PartWheel partWheel) {
        this.vehicleWheel = vehicleWheel;

        setStiffness(partWheel.getDefaultWheelInfo().getSuspensionStiffness());
        setCompression(partWheel.getDefaultWheelInfo().getWheelsDampingCompression());
        setDampness(partWheel.getDefaultWheelInfo().getWheelsDampingRelaxation());
        setMaxForce(partWheel.getDefaultWheelInfo().getSuspensionMaxForce());
        setRestLength(partWheel.getDefaultWheelInfo().getSuspensionRestLength());
    }

    public void setStiffness(float stiffness) {
        this.stiffness = stiffness;
        this.vehicleWheel.setSuspensionStiffness(stiffness);
    }

    public void setCompression(float compression) {
        this.compression = compression;
        this.vehicleWheel.setWheelsDampingCompression((float) (this.compression * 2.0f * Math.sqrt(stiffness)));
    }

    public void setDampness(float dampness) {
        this.dampness = dampness;
        this.vehicleWheel.setWheelsDampingRelaxation((float) (this.dampness * 2.0f * Math.sqrt(stiffness)));
    }

    public void setMaxForce(float maxForce) {
        this.maxForce = maxForce;
        vehicleWheel.setMaxSuspensionForce(this.maxForce);
    }

    public void setRestLength(float restLength) {
        this.restLength = restLength;
        vehicleWheel.setRestLength(restLength);
    }
}
