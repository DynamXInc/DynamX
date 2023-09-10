### Introduction

This page will teach you how to add functional doors with physics to vehicles.

!!!warning
    Doors may sometimes have synchronization issues, especially in multiplayer, which will be improved in future versions of DynamX ;)

!!!info
    Support for doors on blocks and props is planned and will arrive later.

### Prerequisites
You must have configured a vehicle.

!!!danger
    This version of the tutorial only works with DynamX versions 4.0.0 and later.

### Model Preparation

In your 3D model, separate each door into a separate object and export the model with the doors at the origin (point 0 0 0) of the model, just like the steering wheel, for example.

Note that only doors with rotation are supported; you cannot create a door that follows a translation (like the door of a van, for example).

### Configuration

Here is an example configuration for a door:
```
leftfrontdoor{
    Position: 1.0403 -0.2035 0.8
    Scale: 0.102 0.509 0.436
    LocalCarAttachPoint: 1.1038 -0.95709 0.93769
    LocalDoorAttachPoint: 0 -0.78 0

    OpenedDoorAngleLimit: 0 0.78
    ClosedDoorAngleLimit: 0 0
    DoorOpenForce: 1 200
    DoorCloseForce: -1.5 300
}
```
The values should be adapted to your model and the direction in which the door should open.

And here is an explanation of each of the properties:
${PartDoor.md}

To help you configure them, you can use the 'Door attach points' debug option:
![image-1](images/debug_doors.png){ width="90%" }

### Conclusion

You can now add doors to DynamX vehicles! If you encounter difficulties or find this tutorial unclear, you can seek help on the DynamX Discord.
