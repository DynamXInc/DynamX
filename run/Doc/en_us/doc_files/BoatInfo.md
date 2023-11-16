This page provides you with specific configurations for boats. Make sure you have already set up [the base](ModularVehicleInfo.md) for it.

### Prerequisites

You must have [exported](Exportation.md) the .obj models and [configured](ModularVehicleInfo.md) the boat's entire base.

### Boat Configuration

A boat is quite simple; you just need to add floaters and a propeller (which simulates both engine and sail effects).

#### Floater Configuration

A floater looks like this (example of the central floaters for the base pack's Zodiac):
```
Float_Core{
    Position: 0 0 -0.6
    Scale: 1 2.5 1.7
    Offset: 0 0 -0.9
    LineSize: 2 0 7
    BuoyCoefficient: 1.2
}
```

!!!info
    Wow, that looks complicated!
    Don't panic; we will go through all these parameters.

The first two lines define the position (the center) and size of the floater, like many other elements in DynamX.

The `Offset` allows you to easily shift the floater if the boat tilts to one side or the other (it's an equivalent of position).

Next, let's get into the details of how a floater works. To calculate buoyancy, the floater you are configuring will be divided into several "sub-cubes," where Archimedes' thrust will be calculated (it's what makes boats float). `LineSize` corresponds to the number of cubes on each axis, here 2 on the X-axis and 7 on the Z-axis. The Y-axis is ignored by DynamX, and 0 means setting it to 1.

Finally, `BuoyCoefficient` corresponds to a buoyancy coefficient: increase it to make the boat float better or decrease it if it doesn't float enough.

!!!info
    To help you with the configuration, floaters are visible by enabling the 'FLOATS' option in the DynamX debug menu.
    You can, for example, look at the floaters of the Zodiac (base DynamX pack).

And here is, in case you need it, the list of configurable parameters:

${PartFloat.md}

You can add as many floaters as you want, as long as their name starts with "Float."

#### Propulsion Configuration

This block allows you to configure the point where propulsion and steering forces are applied. Even if you use a sailboat, this is important! Here's an example used for the base pack's Zodiac:

```
BoatPropeller{
    Position: 0 2.60954 -0.53935
    AccelerationForce: 9000
    BrakeForce: 7000
    SteerForce: 200
}
```

Here is the list of configurable parameters:

${BoatPropellerInfo.md}

### Conclusion

You have finished configuring your first boat! If necessary, you can continue by configuring [its engine](EngineInfo.md) (and its sounds).
If you wish, you can also add [functional doors](../Doors.md) that can be opened and operational [lights](../Lights.md) to it.
