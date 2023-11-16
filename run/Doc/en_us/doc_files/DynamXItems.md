### Adding Items with .obj Models

This page explains how to use DynamX to add items with a .obj model.

If you want to modify the rendering of an item for a vehicle, block, prop, or armor, please refer to the second part of this page.

#### Adding a Simple Item

##### Model Preparation

In your modeling software, place the item's center at 0 0 0, where the hand will hold it.

##### Model Export

Export the model in .obj format, along with the .mtl and associated .png files, and place them in a folder `assets/dynamxmod/models/obj/your_item_name/` in your pack.

##### Item Configuration

Create an `items` folder in your pack, then create a file `item_your_item.dynx` in it.

!!!warning
    To be recognized, the file name must start with `item_`!

Here are the different properties to fill in:

${ItemObject.md}

!!!info
    The translation of the item name is automatically created with the value of "Name".

#### Modifying the In-Game Rendering of an Item

By default, all items are rendered in 3D with their .obj model.

It is possible to modify this rendering with the `ItemScale` and `Item3DRenderLocation` (optional) options.

`Item3DRenderLocation` indicates where your item is rendered in 3D. Here are the 3 possible values:

- `all`: The item is rendered in 3D everywhere.
- `world`: The item is rendered in 3D everywhere except in menus (GUIs), where its 2D texture is used.
- `none`: The rendering is always 2D, and the item's texture is used.

#### If Item3DRenderLocation is different from `all`:

Upon the first launch, a JSON file for the item, `assets/dynamxmod/models/item/your_item.json`, will be automatically created in your pack, pointing to a texture `assets/dynamxmod/textures/item/your_item.png` (not created). You can later add the texture and/or modify the JSON.

### Conclusion

You have added your item to the game. If you have further questions, please visit the DynamX Discord!
