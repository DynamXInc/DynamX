This page will teach you how to add wheels to your cars, motorcycles, and other trailers.

### Prerequisites

You must have [configured](CarInfo.md) your first land vehicle.

### Wheel Configuration

For each type of wheel present on your vehicle, create a file `wheel_your_wheel.dynx` (preferably in the same folder as your vehicle).

The default wheel name to be indicated in the vehicle configuration will be `AttachedWheel: YourPack.your_wheel`.

!!!warning
    To be recognized, the file name must start with "wheel_"!

Here are the different properties to fill in:

*The recommended values are given as indicative, but you will need to test for hours to achieve good handling.*

${PartWheelInfo.md}

!!!tip
    The same wheel can be used on multiple vehicles!

!!!tip
    Suspension can be tedious to configure because many of these properties influence each other, so be patient and vigilant! You can also copy recommended values or those from existing packs...

!!!info
    The configuration of the wheel's width and radius may be simplified in the future.

##### Making a Motorcycle

To make a motorcycle, add two wheels on the sides, making them invisible by setting `Model: disable_rendering` in their config, placing them slightly above the ground, and not applying any braking force. After that, it's quite time-consuming to balance, just like the suspensions... Make sure to place the center of gravity slightly above 0 for a realistic motorcycle behavior.

### Conclusion

Congratulations, if you've done everything in order, you've just put your first car into the game! If you have any further questions, visit the DynamX Discord! You can also add [functional doors](../Doors.md) and [headlights](../Lights.md) to it.
