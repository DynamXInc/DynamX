### Introduction

This page will teach you how to add lights to your blocks and vehicles. In DynamX, by default, a light is a part that remains illuminated at night but does not emit light itself. Here are two examples:

![image-1](images/ford_explorer.png){ width="49%" }
![image-2](images/firetruck.png){ width="49%" }

This allows you to create headlights, turn signals, beacons, and much more.

!!!info
    Support for lights on items and armor is planned and will be available in the future.

By adding BetterLights, which is currently in development (not yet public), you will soon be able to have parts that actually emit light, illuminating blocks and entities, and creating real headlights, flashlights, and more.

### Prerequisites
You must have configured at least one block or vehicle.

!!!danger
    This version of the tutorial only works with DynamX versions 4.0.0 and later.

### Model Preparation

In your 3D model, make sure to separate each object that can be individually lit and name them in a clear manner. Then, add at least one texture for the lit state, following the same process as in the "Model Configuration" step of MultiTextures: [here](../MultiTextures/#configuration-du-modele).

### Configuration

#### Adding the lit state to the light part

Lights are configured using the "MultiLight" block. Here's an example for taillights:

```
MultiLight_rear{
    PartName: rearlight
    LightObject_rearlights{
        LightId: 4
        Textures: on
    }
    LightObject_stop{
        LightId: 6
        Textures: on
    }
}
```

`MultiLight_front` creates a new light source, and you can use any suffix in place of 'front'. `PartName: headlight` refers to the part in the obj model. In this case, the left front headlight and the right front headlight are in the same part, making it easier to configure. For each part of the model, you should have only one `MultiLight`.

The sub-blocks `LightObject_headlights` and `LightObject_lowbeam` create two new types of lights: the car's headlights when the nighttime lights are on, and the headlights on when braking. For nighttime lights, `LightId: 4` specifies the light's ID, and multiple `LightObjects` can share the same ID. When you want to turn on the headlight, using the [BasicsAddon](https://github.com/DynamXInc/BasicsAddon#lights-and-sounds), for example, you will need to provide this ID. Finally, `Textures: on` specifies the lit texture of the headlight, as configured in the mtl file (see above).

To complete the headlights, you can similarly add the rear lights:
```
MultiLight_front{
    PartName: light
    LightObject {
        LightId: 4
        Textures: on
    }
}
```
Note that the `LightObject` here does not have a name: there is no need for it since there is only one, and it shares the same `LightId` as `LightObject_rearlights`: both will turn on and off at the same time.

#### Making the Lights Blink

Here's an example of left turn signals. For a simpler configuration, in the obj model, the front and rear turn signals are in the same part.
```
MultiLight_turnleft {
    PartName: left
    LightObject{
        LightId: 2
        Textures: on
        BlinkSequenceTicks: 8 15
    }
}
```
Compared to before, we added the line `BlinkSequenceTicks: 8 15`. This is a sequence of ticks (20 ticks correspond to one second), indicating alternately when the light turns on and off: with `8 15`, the light will turn on after 8 ticks, turn off after 15 ticks, and then (starting from 0) it will turn on again after 8 ticks. With `BlinkSequenceTicks: -1 0 8 15`, it will start in the on state, then turn off after 8 ticks, and turn back on after 15 ticks. You can also create sequences of other lengths and more complicated ones. For example, with `BlinkSequenceTicks: -1 0 2 4 6 18`, the light will start on, turn off after 2 ticks, turn back on after 4 ticks, turn off after 6 ticks, and turn back on after 18 ticks.

#### Adding a Spotlight (with the BetterLights Mod)

This section explains how to add a spotlight: the light source will truly illuminate blocks and entities in front of it, using shaders and the BetterLights mod.

!!!warning
    The BetterLights mod is not yet available!

Modify the previous rear light as follows:
```
MultiLight_rear{
    PartName: rearlight
    LightObject_headlights{
        LightId: 4
        Textures: on
        SpotLightColor: 0.5 0.1 0.1 1
    }
    LightObject_stop{
        LightId: 6
        Textures: on
        SpotLightColor: 0.9 0.1 0.1 1
    }
    SpotLight_left {
        Offset: 0.9 -0.1 -0.8
        Direction: 0 -0.3 -1
        Angle: 15
        Distance: 20
    }
    SpotLight_right {
        Offset: -0.9 -0.1 -0.8
        Direction: 0 -0.3 -1
        Angle: 15
        Distance: 20
    }
}
```
We have added `SpotLightColor` to the LightObjects: this will be the color of the light projected when the source is on. If both LightObjects are on, the one listed last will be applied.
The `SpotLight_left` and `SpotLight_right` blocks define the left and right spotlights.

- `Offset` corresponds to their position relative to the origin of the 3D model.
- `Direction` corresponds to the direction of the light in the model's coordinate system.
- `Angle` corresponds to the "width" of the light, in degrees.
- `Distance` corresponds to the range of the light, in meters.

#### List of Configurable Properties

##### MultiLight
${PartLightSource.md}

##### LightObject
${LightObject.md}

##### SpotLightObject (BetterLights)
Coming soon: ${SpotLightObject.md}

### Conclusion

You now know how to add lights to DynamX objects! If you encounter difficulties or if this tutorial is not clear enough, you can seek help on the DynamX Discord!
