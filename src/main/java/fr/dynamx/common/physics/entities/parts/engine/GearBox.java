package fr.dynamx.common.physics.entities.parts.engine;

import com.jme3.math.Vector3f;
import fr.dynamx.common.physics.entities.BaseVehiclePhysicsHandler;
import fr.dynamx.utils.DynamXConfig;
import fr.dynamx.utils.maths.DynamXGeometry;
import fr.dynamx.utils.maths.DynamXMath;
import fr.dynamx.utils.optimization.Vector3fPool;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.minecraft.util.math.MathHelper;

public class GearBox {
    /**
     * -1 : reverse
     * 0 : neutral
     * 1,2,3,4,5,6 : other speeds
     */
    private int activeGear;
    private final GearData[] gears;
    private int gearChangeCounter;

    public GearBox(int gearCount) {
        this.gears = new GearData[gearCount];

        for (int i = 0; i < gearCount; i++) {
            this.gears[i] = new GearData();
        }
    }

    public void setGear(float maxRPM, int gearNum, float start, float end, float rpmStart, float rpmEnd, float gearRatio) {
        GearData gear = this.gears[gearNum];
        gear.setStart(start);
        gear.setEnd(end);
        gear.setRpmStart(rpmStart / maxRPM);
        gear.setRpmEnd(rpmEnd / maxRPM);
        gear.setGearRatio(gearRatio);
        gear.setGearChangeThreshold(100 / maxRPM);
    }

    public GearData getActiveGear() {
        return this.gears[activeGear + 1];
    }

    public boolean increaseGear() {
        if (getActiveGearNum() >= 1 && getActiveGearNum() + 2 < getGearCount()) //on est en marche avant
        {
            setActiveGearNum(getActiveGearNum() + 1);
            return true;
        }
        return false;
    }

    public boolean decreaseGear() {
        if (getActiveGearNum() >= 1) //on est en marche avant
        {
            setActiveGearNum(getActiveGearNum() - 1);
            return true;
        } else if (getActiveGearNum() < 0) {
            setActiveGearNum(getActiveGearNum() + 1);
            return true;
        }
        return false;
    }


    /**
     * -1 : reverse
     * 0 : neutral
     * 1,2,3,4,5,6 : other speeds
     */
    public int getActiveGearNum() {
        return activeGear;
    }

    /**
     * -1 : reverse
     * 0 : neutral
     * 1,2,3,4,5,6 : other speeds
     */
    public void setActiveGearNum(int activeGear) {
        if (activeGear != 0)
            gearChangeCounter = getGearChangeTime();
        else
            gearChangeCounter = 0;
        this.activeGear = activeGear;
    }

    public void syncActiveGearNum(int activeGear) {
        this.activeGear = activeGear;
    }

    public int getGearCount() {
        return this.gears.length;
    }

    public int updateGearChangeCounter() {
        if (gearChangeCounter > 0)
            gearChangeCounter--;
        return gearChangeCounter;
    }

    public int getGearChangeTime() {
        return getActiveGearNum() == 0 ? 0 : DynamXConfig.gearChangeDelay;
    }

    public float getRPM(BaseVehiclePhysicsHandler<?> vehicle, Engine engine, float speed) {
        //TODO WORK HERE
        GearData gear = getActiveGear();
        float revs = DynamXMath.normalize(speed, gear.getStart(), gear.getEnd());
        revs = MathHelper.clamp(revs, 0, 1);
        revs *= (gear.getRpmEnd() - gear.getRpmStart());
        revs += gear.getRpmStart(); //on ajoute les tours moteurs minimaux (irl si on tombe dessous on cale donc avec une boite auto pas possible)

        Vector3f rotatedForwardDirection = Vector3fPool.get();
        rotatedForwardDirection = vehicle.getRotation().mult(DynamXGeometry.FORWARD_DIRECTION, rotatedForwardDirection);
        float rotationPitch = DynamXGeometry.getPitchFromRotationVector(rotatedForwardDirection) % 360;
        rotationPitch = (float) (rotationPitch * Math.PI / 180);
        float requiredTorque = (float) -(vehicle.getCollisionObject().getMass() * vehicle.getCollisionObject().getGravity(Vector3fPool.get()).y * Math.sin(rotationPitch));
        requiredTorque = requiredTorque;
        float currentTorque = engine.getTorqueOutput(gear, revs);
        float loadFactor = requiredTorque / currentTorque;
        System.out.println("LOD " + loadFactor + " " + requiredTorque + "/" + currentTorque + " // " + rotationPitch + " RPM " + engine.getRevs() + " " + revs);
        if(loadFactor > 1) {
            float adjust = 0.1f;
            revs = revs + (loadFactor - 1) * adjust;
        }
        return revs;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class GearData {
        private float start, end;
        private float rpmStart, rpmEnd;
        private float gearChangeThreshold;
        private float gearRatio;
    }
}
