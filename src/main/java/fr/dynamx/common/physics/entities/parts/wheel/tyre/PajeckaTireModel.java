package fr.dynamx.common.physics.entities.parts.wheel.tyre;

public class PajeckaTireModel {

    private String name;

    private TyreSettings lateral;
    private TyreSettings longitudinal;
    private TyreSettings alignMoment;

    // the maximun load the tire can handle.
    private float maxLoad;
    // the amount of load on the tire.
    private float load;

    private float lateralValue;
    private float longitudinalValue;
    private float momentValue;

    // friction circle result.
    // a method of combining Fx and Fy together.
    private float frictionCircle;


    public PajeckaTireModel(String name,
                            TyreSettings lateral, TyreSettings longitudinal, TyreSettings alignMoment,
                            float maxLoad) {

        this.name = name;
        this.lateral = lateral;
        this.longitudinal = longitudinal;
        this.alignMoment = alignMoment;
        this.maxLoad = maxLoad;

    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public TyreSettings getLateral() {
        return lateral;
    }

    public void setLateral(TyreSettings lateral) {
        this.lateral = lateral;
    }

    public TyreSettings getLongitudinal() {
        return longitudinal;
    }

    public void setLongitudinal(TyreSettings longitudinal) {
        this.longitudinal = longitudinal;
    }

    public TyreSettings getAlignMoment() {
        return alignMoment;
    }

    public void setAlignMoment(TyreSettings alignMoment) {
        this.alignMoment = alignMoment;
    }

    public float getMaxLoad() {
        return maxLoad;
    }

    public void setMaxLoad(float maxLoad) {
        this.maxLoad = maxLoad;
    }

    // slipAngle is in RADIANS
    private float calcSlipAngleFactor(float slipAngle, TyreSettings settings) {
        // float x = slipAngle * FastMath.DEG_TO_RAD;
        // float x = slipAngle;

        return (float) Math.sin(settings.getSlipAngleCoefficientC()
                * Math.atan(settings.getSlipAngleCoefficientB() * slipAngle - settings.getSlipAngleCoefficientE()
                * (settings.getSlipAngleCoefficientB() * slipAngle - Math.atan(settings.getSlipAngleCoefficientB() * slipAngle))));
    }

    private float calcLoadForce(float load, TyreSettings settings) {
        return settings.getLoadCoefficientKA() * (1 - settings.getLoadCoefficientKB() * load) * load;
    }

    /**
     * Calculates the lateral cornering force for this tire in N.<br>
     * <br>
     * <b>CAUTION:</b> this function returns a value in Newton N!
     *
     * @param slipAngle
     *            - the slip angle in degrees (°).
     *
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

    public float getLateralValue() {
        return this.lateralValue;
    }

    public float getLongitudinalValue() {
        return longitudinalValue;
    }

    public float getMomentValue() {
        return momentValue;
    }

    public float getFrictionCircle() {
        return frictionCircle;
    }

    @Override
    public String toString() {

        String format = "%s: \"%s\" : %s (C=%.2f, B=%.2f, E=%.2f, KA=%.2f, KB=%.6f)";

        String lat = String.format(format, this.getClass().toString(),
                name, "Lateral",
                lateral.getSlipAngleCoefficientC(), lateral.getSlipAngleCoefficientB(), lateral.getSlipAngleCoefficientE(),
                lateral.getLoadCoefficientKA(), lateral.getLoadCoefficientKB());

        String lng = String.format(format, this.getClass().toString(),
                name, "Longitudinal",
                longitudinal.getSlipAngleCoefficientC(), longitudinal.getSlipAngleCoefficientB(), longitudinal.getSlipAngleCoefficientE(),
                longitudinal.getLoadCoefficientKA(), longitudinal.getLoadCoefficientKB());

        String mnt = String.format(format, this.getClass().toString(),
                name, "Align Moment",
                alignMoment.getSlipAngleCoefficientC(), alignMoment.getSlipAngleCoefficientB(), alignMoment.getSlipAngleCoefficientE(),
                alignMoment.getLoadCoefficientKA(), alignMoment.getLoadCoefficientKB());

        return lat + System.lineSeparator() + lng + System.lineSeparator() + mnt;
    }


    public float getLoad() {
        return load;
    }

    public void setLoad(float load) {
        this.load = load;
    }

}
