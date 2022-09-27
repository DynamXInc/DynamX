package fr.dynamx.common.physics.entities.parts.wheel.tyre;

import fr.dynamx.common.physics.entities.parts.wheel.WheelPhysics;
import lombok.Getter;
import lombok.Setter;

public class PajeckaTireModel {

    @Getter
    @Setter
    private WheelPhysics.TyreSettings lateral, longitudinal, alignMoment;
    // the amount of load on the tire.
    @Getter
    @Setter
    private float load;

    @Getter
    private float lateralValue;
    @Getter
    private float longitudinalValue;
    @Getter
    private float momentValue;

    // friction circle result.
    // a method of combining Fx and Fy together.
    @Getter
    private float frictionCircle;


    public PajeckaTireModel(WheelPhysics.TyreSettings lateral, WheelPhysics.TyreSettings longitudinal, WheelPhysics.TyreSettings alignMoment) {
        this.lateral = lateral;
        this.longitudinal = longitudinal;
        this.alignMoment = alignMoment;
    }

    // slipAngle is in RADIANS
    private float calcSlipAngleFactor(float slipAngle, WheelPhysics.TyreSettings settings) {
        return (float) Math.sin(settings.getSlipAngleCoefficientC()
                * Math.atan(settings.getSlipAngleCoefficientB() * slipAngle - settings.getSlipAngleCoefficientE()
                * (settings.getSlipAngleCoefficientB() * slipAngle - Math.atan(settings.getSlipAngleCoefficientB() * slipAngle))));
    }

    private float calcLoadForce(float load, WheelPhysics.TyreSettings settings) {
        return settings.getLoadCoefficientKA() * (1 - settings.getLoadCoefficientKB() * load) * load;
    }

    /**
     * Calculates the lateral cornering force for this tire in N.<br>
     * <br>
     * <b>CAUTION:</b> this function returns a value in Newton N!
     *
     * @param slipAngle - the slip angle in degrees (°).
     * @return - lateral tire force in N.
     */
    public float calcLateralTireForce(float slipAngle) {
        this.lateralValue = calcSlipAngleFactor(slipAngle, lateral) * calcLoadForce(load, lateral);
        return lateralValue;
    }

    public float calcLongtitudeTireForce(float slipAngle) {
        this.longitudinalValue = calcSlipAngleFactor(slipAngle, longitudinal) * calcLoadForce(load, longitudinal);
        return longitudinalValue;
    }

    public float calcAlignMoment(float slipAngle) {
        this.momentValue = calcSlipAngleFactor(slipAngle, alignMoment) * calcLoadForce(load, alignMoment);
        return momentValue;
    }

    @Override
    public String toString() {

        String format = "%s: \"%s\" : %s (C=%.2f, B=%.2f, E=%.2f, KA=%.2f, KB=%.6f)";

        String lat = String.format(format, this.getClass().toString(),
                "Lateral",
                lateral.getSlipAngleCoefficientC(), lateral.getSlipAngleCoefficientB(), lateral.getSlipAngleCoefficientE(),
                lateral.getLoadCoefficientKA(), lateral.getLoadCoefficientKB());

        String lng = String.format(format, this.getClass(),
                "Longitudinal",
                longitudinal.getSlipAngleCoefficientC(), longitudinal.getSlipAngleCoefficientB(), longitudinal.getSlipAngleCoefficientE(),
                longitudinal.getLoadCoefficientKA(), longitudinal.getLoadCoefficientKB());

        String mnt = String.format(format, this.getClass(),
                "Align Moment",
                alignMoment.getSlipAngleCoefficientC(), alignMoment.getSlipAngleCoefficientB(), alignMoment.getSlipAngleCoefficientE(),
                alignMoment.getLoadCoefficientKA(), alignMoment.getLoadCoefficientKB());

        return lat + System.lineSeparator() + lng + System.lineSeparator() + mnt;
    }

}
