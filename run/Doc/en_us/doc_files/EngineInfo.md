This page will teach you how to add an engine to your car, boat, or helicopter.

### Prerequisites

You must have [configured](ModularVehicleInfo.md) your first vehicle.

### Engine Configuration:

Create a file `engine_your_engine.dynx` (preferably in the same folder as your vehicle).

The default engine name to be specified in the vehicle configuration will then be `DefaultEngine: YourPack.your_engine`.

!!!warning
    To be recognized, the filename must start with "engine_"!

Here are the different properties to fill in:

${CarEngineInfo.md}

##### Power Curve Configuration:

!!!info
    This configuration is only useful for cars and boats. It is unnecessary for helicopters.

The power of an engine varies according to its revolutions per minute (RPM). Here's how to configure it, using its points:

```
Point_0{
    RPMPower: rpm ratio
}
Point_1{
    RPMPower: rpm ratio
}
Point_2{
    RPMPower: rpm ratio
}
Point_3{
    RPMPower: rpm ratio
}
```

Where ratio is the power ratio (between 0 and 1) and rpm is the corresponding revolutions per minute. You can add as many points as you want.

!!!danger
    The number of rpm in the last point must be greater than or equal to the engine's MaxRPM (defined above).

##### Speed Configuration:

!!!info
    This configuration is only useful for cars and boats. It is unnecessary for helicopters.

Speeds are automatically switched according to the levels defined in the engine configuration.

Example of the speed configuration for the ds7:

```
Gear_0{
    SpeedRange: 0 -30
    RPMRange: 800 5500
}
Gear_1{
    SpeedRange: -1000000 1000000
    RPMRange: 0 5500
}
Gear_2{
    SpeedRange: 0 20
    RPMRange: 800 2750
}
Gear_3{
    SpeedRange: 15 40
    RPMRange: 900 2750
}
Gear_4{
    SpeedRange: 35 60
    RPMRange: 900 2750
}
Gear_5{
    SpeedRange: 55 80
    RPMRange: 900 2750
}
Gear_6{
    SpeedRange: 75 110
    RPMRange: 900 2900
}
Gear_7{
    SpeedRange: 90 200
    RPMRange: 1050 5500
}
```

Explanations:

Gear_0 is the reverse gear, Gear_1 is neutral, and Gear_2, ..., Gear_7 are forward speeds (you can add as many as you want).

Here is the definition of the properties:

${GearInfo.md}

### Engine Sound Configuration:

Create a file `sounds_your_sounds.dynx` (preferably in the same folder as your vehicle).

The default sound names to indicate in the vehicle configuration will then be `DefaultSounds: YourPack.your_sounds`.

!!!warning
    To be recognized, the file name must start with "sounds_".

Here is the structure to follow (example of the ds7):

```
Engine{
    Interior{
        Starting{
            Sound: tt_start
        }
        0-1500{
            Sound: tt_int_idle
			PitchRange: 0.5 2.0
        }
        1400-5000{
            Sound: tt_int_low
			PitchRange: 0.0 0.7
        }
        4900-9000{
            Sound: tt_int_high
			PitchRange: 0.0 0.65
        }
    }
    Exterior{
        Starting{
            Sound: tt_start
        }
        0-1500{
            Sound: tt_ex_idle
			PitchRange: 0.5 2.0
        }
        1400-5000{
            Sound: tt_ex_low
			PitchRange: 0.0 0.7
        }
        4900-9000{
            Sound: tt_ex_high
			PitchRange: 0.0 0.65
        }
    }
}
```

Explanations:

Interior and Exterior sounds are separated into two distinct sections.

For each sound, you must specify the sound file name in the "Sound" property in such a way that for a sound named "test" placed in `assets/dynamxmod/sounds/test.ogg`, you would have `Sound: test`.

!!!warning
    Like in vanilla Minecraft, only .ogg sounds are supported.

The "Starting" sound plays when the engine is started and does not require a PitchRange.

Other sounds depend on the engine's RPM (Revolutions Per Minute). For example, the "0-1500" sound will play between 0 and 1500 RPM. In this case, "PitchRange" defines the pitch of the sound at 0 RPM and at 1500 RPM (if the pitch value is 1, the sound will be the same as in the .ogg file, if the pitch value is 0.5, it will be very low, and conversely, it will be very high if the pitch value is 2). A transition is made between each sound by the mod.

!!!info
    For helicopters, the RPM is defined based on the power given by the player. This directly corresponds to the rotor's rotation speed.

Here is the definition of the properties:

${EngineSound.md}

!!!tip
    The behavior of sounds can be modified using the mod's API events (see the dedicated documentation).

### Conclusion

If you have followed all the steps, you may still need to configure [the wheels](WheelInfo.md) ;)
