This page provides specific configurations for helicopters. Make sure you have already configured [the base](ModularVehicleInfo.md) of the helicopter.

### Prerequisites

You must have [exported](Exportation.md) the obj models and [configured](ModularVehicleInfo.md) the entire base of the helicopter.

### Helicopter Configuration

To configure a helicopter, you will need a rotor, physics, and a joystick. Here's how to configure everything:

#### Rotor Configuration

Here is an example of rotor configuration:

```
Rotor_Main{
	Position: 0 0.432414 3.99325
	RotationAxis: 0 1 0
	RotationSpeed: 70
	PartName: main_rotor
}
```

`Position` defines the position of the rotor on the helicopter. `RotationAxis` defines its rotation axis: with `0 1 0`, it will be vertical, like the main rotor of a helicopter, and with `1 0 0`, it will be horizontal, like the tail rotor. `RotationSpeed` defines the rotor's rotation speed at the maximum power of the helicopter.

`PartName` defines the part of the 3D model to be drawn. It must be separate from the chassis and positioned at `0 0 0` when exporting the model, similar to the steering wheel of a car.

Here is the list of all configurable parameters:

${PartRotor.md}

You can add as many rotors as you want, as long as their names start with "Rotor".

#### Physics Configuration

Here, we will define the physics of the helicopter. There are many parameters, but it's not so complicated. Here's an example:
```
HelicopterPhysics{
    MinPower: 0.4
    InclinedGravityFactor: 1.8
    ThrustForce: 3000
    VerticalThrustCompensation: 2000
    BrakeForce: 500
    MouseYawForce: 2600
    MousePitchForce: 2000
    MouseRollForce: 400
    RollForce: 6000
}
```

All of these parameters are explained here:

${HelicopterPhysicsInfo.md}

#### Flight Controls Configuration

Coming soon.

${PartHandle.md}

### Conclusion

Congratulations! If you've followed all the steps, you've just added your first helicopter to the game! If you have any further questions, feel free to visit the DynamX Discord server.
You can also add functional [doors](../Doors.md) and [lights](../Lights.md) to it.
