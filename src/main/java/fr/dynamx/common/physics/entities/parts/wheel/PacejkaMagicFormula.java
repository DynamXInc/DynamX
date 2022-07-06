package fr.dynamx.common.physics.entities.parts.wheel;

import fr.dynamx.common.physics.entities.modules.WheelsPhysicsHandler;

public class PacejkaMagicFormula
{
    private WheelsPhysicsHandler car;
    public float[] lateral = new float[10];
    public float[] longitudinal= new float[10];

    public PacejkaMagicFormula(WheelsPhysicsHandler car) {
        this.car = car;
    }

    public void update() {
        for (int i = 0; i < car.getNumWheels(); i++) {

            WheelPhysicsHandler wheelPhysicsHandler = car.getWheel(i);

            // the angle between the dir of the wheel and the dir the vehicle is travelling.
            float lateralSlip = wheelPhysicsHandler.calculateLateralSlipAngle();

            float load = 10000;

            wheelPhysicsHandler.getTireModel().setLoad(load);

            // returns the amount of force in N on the tyre.
            // this model allows max 10,000 (this is determined by the tyre).
            lateral[i] = wheelPhysicsHandler.getTireModel().calcLateralTireForce(lateralSlip);

            // the slip angle for this is how much force is being applied to the tyre (acceleration force).
            float longSlip = wheelPhysicsHandler.calculateLongitudinalSlipAngle();
            longitudinal[i] = wheelPhysicsHandler.getTireModel().calcLongtitudeTireForce(longSlip);

            float friction = 1.0f - ((lateral[i] / 10000) - (longitudinal[i] / 10000));
            friction *= 2.0;
            friction = wheelPhysicsHandler.getGrip() * friction;
            wheelPhysicsHandler.setFriction(friction);
        }
    }
}
