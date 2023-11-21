### Pack Creation in DynamX

DynamX allows you to easily add various types of content (vehicles, armor, items, blocks, and props) to Minecraft. You will learn how to create a pack here.

### How Packs Work

Packs can either be folders or compressed files (zip) renamed with the .dnxpack extension.

To create a pack, it's easier to work with a folder, which should be placed in the **DynamX folder** created when you first launch Minecraft with the mod.

Create a folder called `MyPack` with the name of your pack, and it will contain all the files for your pack.

Inside this pack, you will need to create a file named `pack_info.dynx`, which will contain basic information about your pack.

!!!warning
    We recommend familiarizing yourself with the [pack syntax](../DynamXPackConfigsSyntax.md) before filling out this file.

### pack_info.dynx File

This file is mandatory in every pack. Here are the properties that can be provided:

${PackInfo.md}

!!!info
    The PackName property contains your pack's name and allows you to rename the folder without breaking anything, as long as PackName remains the same. It will be reused in several other places.

Here's a perfect example for a pack using DynamX version 4.0.0 or later:
```
PackName: MyPack
PackVersion: 1.0.0
DcFileVersion: 12.5.0
CompatibleWithLoaderVersions: [1.1.0,)
```

If your pack depends on addons, you can add multiple RequiredAddon blocks with the following properties:

${RequiredAddonInfo.md}

### Example Pack

You can download example packs [here](https://files.dynamx.fr/addons/) and use them as a starting point to create your own!

!!!tip
    .dnxpack files are renamed .zip files: you can open them with software like WinRAR.

### Debugging a Pack

To help you create packs, DynamX displays all loading errors in the "Errors" submenu of the DynamX debug menu, accessible with the command `/dynamx debug_gui`.

### What's Next

You can now proceed to the next step: (re)viewing the syntax of pack configuration files and start adding content to DynamX! [Go here](../DynamXPackConfigsSyntax.md).
