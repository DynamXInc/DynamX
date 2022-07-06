package fr.dynamx.common.physics.entities.parts.wheel;

import com.jme3.bullet.objects.VehicleWheel;
import fr.dynamx.common.contentpack.parts.PartWheel;

public class SuspensionPhysicsHandler {

    private float stiffness;
    private float compression;
    private float dampness;
    private float maxForce;
    private float restLength;

    private final VehicleWheel vehicleWheel;

    public SuspensionPhysicsHandler(VehicleWheel vehicleWheel, PartWheel partWheel) {
        this.vehicleWheel = vehicleWheel;

        setStiffness(partWheel.getDefaultWheelInfo().getSuspensionStiffness());
        setCompression(partWheel.getDefaultWheelInfo().getWheelsDampingCompression());
        setDampness(partWheel.getDefaultWheelInfo().getWheelsDampingRelaxation());
        setMaxForce(partWheel.getDefaultWheelInfo().getSuspensionMaxForce());
        setRestLength(partWheel.getDefaultWheelInfo().getSuspensionRestLength());
    }

    public float getStiffness() {
        return stiffness;
    }

    public void setStiffness(float stiffness) {
        this.stiffness = stiffness;
        this.vehicleWheel.setSuspensionStiffness(stiffness);
    }

    public float getCompression() {
        return compression;
    }

    public void setCompression(float compression) {
        this.compression = compression;
        this.vehicleWheel.setWheelsDampingCompression((float) (this.compression * 2.0f * Math.sqrt(stiffness)));
    }

    public float getDampness() {
        return dampness;
    }

    public void setDampness(float dampness) {
        this.dampness = dampness;
        this.vehicleWheel.setWheelsDampingRelaxation((float) (this.dampness * 2.0f * Math.sqrt(stiffness)));
    }

    public float getMaxForce() {
        return maxForce;
    }

    public void setMaxForce(float maxForce) {
        this.maxForce = maxForce;
        vehicleWheel.setMaxSuspensionForce(this.maxForce);
    }

    public float getRestLength() {
        return restLength;
    }

    public void setRestLength(float restLength) {
        this.restLength = restLength;
        vehicleWheel.setRestLength(restLength);
    }
}
