### Introduction

This page explains how to use DynamX to add armor with a .obj model.

### Model Preparation

Once you have modeled your armor in Blender, you need to separate the different parts and give them the following names:

| Armor Part        | Model Object Name        | Blender Position (x y z) |
| ------------------ | ------------------------ | ------------------------- |
| Body               | bodyModel                | 0 0 0                     |
| Head               | headModel                | 0 0 0                     |
| Left Arm           | leftArmModel             | -0.3125 0 -0.125          |
| Right Arm          | rightArmModel            | 0.3125 0 -0.125           |
| Left Leg           | leftLegModel             | 0.11875 0 -0.75           |
| Right Leg          | rightLegModel            | 0.11875 0 -0.75           |
| Left Foot          | leftFeetModel            | -                         |
| Right Foot         | rightFeetModel           | -                         |

Here is the recommended structure you should have:

![table](armor_model_structure.png)

!!!info
  This is a recommended structure; you will see later that you can use fewer objects (e.g., only a helmet).

!!!tip
  Display Steve in Blender to properly size the different parts of the armor.

### Model Export

Export the created model in .obj format, accompanied by the .mtl file and associated .png files, and place them in a folder `assets/dynamxmod/models/obj/your_armor_name/` in your pack.

!!!warning
  Pay attention to the object names provided by Blender by default (open the exported .obj file with a text editor and make sure you have `o bodyModel` for the body), do the same for the other objects in the model...

### Armor Configuration

Create an `armors` folder in your pack, then create a file `armor_your_armor_name.dynx` in it.

!!!warning
  To be recognized, the file name must start with "armor_"!

Here are the different properties to fill in:

${ArmorObject.md}

To modify the item's appearance, go here: [item configuration](DynamXItems.md).

You have configured the base of the armor; now you need to configure how it will be displayed:

- Head Configuration:

  Add `ArmorHead: headModel` to add a helmet item that draws the headModel object from the model.

- Body Configuration:

  To add a chestplate item, add `ArmorBody: bodyModel` to draw the bodyModel object from the model, and add `ArmorArms: leftArmModel rightArmModel` to draw the leftArmModel and rightArmModel objects from the model.

- Leg Configuration:

  Add `ArmorLegs: leftLegModel rightLegModel` to add legging items that draw the leftLegModel and rightLegModel objects from the model.

- Foot Configuration:

  Add `ArmorFoot: leftFeetModel rightFeetModel` to add boot items that draw the leftFeetModel and rightFeetModel objects from the model.

- #### Texture Variant Configuration

This part is explained in [multi-texture configuration](MultiTextures.md).

!!!info
  If you put multiple textures on your armor, it cannot have a durability bar.

### Conclusion

There you have it; you have added your armor to the game. If you have any further questions, head to the DynamX Discord!
