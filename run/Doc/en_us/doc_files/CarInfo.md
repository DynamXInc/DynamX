### Vehicle Specific Configurations (Cars, Trucks, Two-Wheelers, Trailers)

This page provides specific configurations for cars, trucks, two-wheelers, and trailers. Make sure you have already configured [the base](ModularVehicleInfo.md) for your vehicle.

### Prerequisites

You must have [exported](Exportation.md) the .obj models and [configured](ModularVehicleInfo.md) the entire base of the vehicle.

### Vehicle Configuration

#### Steering Wheel Configuration

To add a steering wheel, add the following to the vehicle's configuration:

```
SteeringWheel{
    PartName: SteeringWheel
    Position: x y z
    BaseRotationQuat: w x y z
}
```
Here are all the possible properties:

${SteeringWheelInfo.md}

#### Trailer Configuration:

If your vehicle is a trailer (has no engine), or if a trailer can be attached to this vehicle, add:
```
Trailer{
    AttachPoint: x y z
    AttachStrength: maximum_force
}
```

Here are all the possible properties:

${TrailerAttachInfo.md}

!!!warning
    If you configure a trailer, make sure your file is named `trailer_your_vehicle.dynx`!

### Conclusion

You have completed the car configuration. The next steps will be to configure [its engine](EngineInfo.md) (and its sounds) and then [its wheels](WheelInfo.md)! If you like, you can also add [functional doors](../Doors.md) and [working headlights](../Lights.md).
