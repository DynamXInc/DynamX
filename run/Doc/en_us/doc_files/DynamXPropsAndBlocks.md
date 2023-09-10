### Adding Blocks and Props with .obj Models in DynamX

DynamX allows you to easily add blocks and props with .obj models to Minecraft. Here's a complete tutorial on how to add them to the game.

!!!info
	A prop is a block subject to physics.

!!!warning
	It is highly recommended to export models using Blender. Models exported, for example, via BlockBench are not optimized and may cause lag.

### Model Preparation

DynamX offers the ability to automatically create a collision box for each `object` in the .obj file. This collision box will be used for collisions with all entities and other physical objects.

For example, if you cut your model like this (table model):

![table](table.png)

The algorithm will create 2 collision boxes, one for the legs and one for the top of the table. Keep this in mind when creating your model (and also avoid adding too many unnecessary objects that could make the block/prop heavier).

### Exporting Models

Export the model in .obj format, accompanied by the .mtl and .png files, then place them in a folder `assets/dynamxmod/models/obj/your_block_or_prop_name/` in your pack.

### Block (or Prop) Configuration

- ##### Common Part for Blocks and Props

!!!info
	If you only want to create a prop, you must first create the corresponding block, and then you can remove it from the creative tabs.

Create a folder `blocks` at the root of your pack, then create a file `block_your_block.dynx` in it.

!!!warning
	To be recognized, the file name must start with `block_`, whether it's for creating a block and/or a prop!
!!!danger
	Pay attention to the two formats of positions used: in Blender, the vertical axis is the Z-axis, while in Minecraft, it's the Y-axis, and Minecraft's Z-axis is in the opposite direction (coordinates multiplied by -1). We will specify which format to use whenever necessary.

    Blender Format: `X Y Z`. Minecraft Format: `X Z -Y`.

Here is the list of variables for configuring a block.

!!!info
	The translation of the block/prop name is automatically created with the value of "Name."

- ##### Prop Configuration

Add the following code in the block's file:
```
Prop_NomDuProp{
    //Configuration du prop ici
}
```

!!!info
	The prop is a sub-property of the block, and its name must start with "Prop."

Here is the list of variables for configuring a prop.

!!!info
The properties already specified in the block are optional; the prop will use the block's configuration (except for the creative tab, which should also be configured in the prop if you don't want to use the default creative tab).

- ##### Texture Variant Configuration

This part is explained in [multi-texture configuration](MultiTextures.md).

- ##### Finishing Configuration

To modify the item's rendering of the block/prop, go here: [item configuration](https://dynamx.fr/wiki/dynamx/DynamXItems/).

### Conclusion

There you have it, you've added your block and/or prop to the game. If you have any other questions, head over to the DynamX Discord! You can also add [functional lights](Lights.md) to your block.
