This page will guide you through the creation of your vehicle, ranging from cars to helicopters, including motorcycles, trailers, and boats.

### Prerequisites

You must have [exported](Exportation.md) the obj models.

### Vehicle Configuration

Whether you want to create a car, a motorcycle, a trailer, a boat, or a helicopter, this part is common to all types of vehicles.

Create a folder with the name of your vehicle in your pack, and then create a file with one of the following names, depending on what you want to add:

| Vehicle Type    | Prefix        | Example                                                |
| -------- | -------------- | ------------------------------------------------------------ |
| Car, truck, or motorcycle | vehicle_ | `vehicle_your_vehicle.dynx` |
| Trailer     | trailer_          | `trailer_your_vehicle.dynx` |
| Boat   | boat_         | `boat_your_vehicle.dynx` |
| Helicopter     | helicopter_         | `helicopter_your_vehicle.dynx` |

!!!warning
    It's the prefix that determines the type of your vehicle! If you put something different, it will be ignored.

!!!danger
    Pay attention to the two formats of positions used: in Blender, the vertical axis is the Z-axis, while in Minecraft, it's the Y-axis, and Minecraft's Z-axis is in the reverse direction (coordinates multiplied by -1). We will specify which format to use each time it's necessary.
	Blender Format: `X Y Z`. Minecraft Format: `X Z -Y`.

Here are the various properties to fill in:

${ModularVehicleInfo.md}

!!!info
    The translation of the vehicle name is automatically created with the value of "Name."

To modify the item rendering, go here: [item configuration](https://dynamx.fr/wiki/dynamx/DynamXItems/).

#### Configuration of collision boxes:

These collision boxes are used for collisions with other vehicles *if the `UseComplexCollisions` option is disabled*. In any case, they are used for collisions with players and must be configured.

Cut your models into several cubes that represent the collisions to be done with players, and then, for each cube, add to the vehicle's configuration:
```
Shape_NomDuCube{
	Position: x y z
	Scale: x y z
}
```
Position is the cube's position, and Scale is its size. The coordinates are in Blender format! Be careful: CubeName must be unique.

Here's an example with the trophy truck (three collision cubes):

![collision_shapes](collision_shapes.png)

!!!info
    The configuration of these collisions may be simplified in the future.

#### Wheel Configuration:

For each wheel on the vehicle (you can add as many as you want), add to the vehicle's configuration:
```
Wheel_NomDeLaRoue{
    AttachedWheel: YourPack.default_wheel
    IsRight: false
	Position: x y z
	IsSteerable: True
	MaxTurn: 0.7
	DrivingWheel: False
}
```
Here are all the possible properties:

${PartWheel.md}

#### Seat Configuration:

For each seat on the vehicle, add to the vehicle's configuration:
```
Seat_NomDuSiege{
    Position: x y z
    Driver: True
}
```
Where Position is the seat's position in Blender format, and Driver indicates whether it's the driver's seat (true) or not (false).

Here are all the possible properties:

${PartSeat.md}

#### Configuration of texture variants:

This part is explained in [multi-texture configuration](../MultiTextures.md).

#### Configuration of air friction points:

You can configure points where air friction forces will be applied, greatly improving your vehicles' handling, at the cost of longer configuration time.

These points are configured "by eye," but it's best to place one near the front of the vehicle, centered between the two wheels.

Example configuration, for the trophy truck from the base pack:
```
ForcePointNom{
    Position: 0 -0.89534 0
    Intensity: 0.4 1 0.4
}
```
Here are all the possible properties:

${FrictionPoint.md}

The "Name" can be replaced with any name (so you can have multiple friction points). These points are visible in the "Friction points" debug, and you will also see the applied forces.

#### Other Configurations:

- [Headlight Configuration](../Lights.md).
- [Door Configuration](../Doors.md).

### Car or Trailer Configuration

Go [here](CarInfo.md)!
This section is also valid for motorcycles and other two-wheelers.

### Boat Configuration

Go [here](BoatInfo.md)!

### Helicopter Configuration

Go [here](HelicopterInfo.md)!

### Aircraft Configuration

*Unfortunately, it's not possible yet :'(*.

### Conclusion

Congratulations, you have put your vehicle into the game! If you have any more questions, visit the DynamX Discord!
