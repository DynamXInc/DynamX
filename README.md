<p align="center">
    <img src="https://files.dynamx.fr/img/banner_dynamx_2_crop.png" alt="DynamX Banner" width="60%">
    <br>
    <a href="https://www.curseforge.com/minecraft/mc-mods/dynamx">
        <img src="https://cf.way2muchnoise.eu/full_dynamx_downloads.svg" alt="Downloads">
        <img src="https://cf.way2muchnoise.eu/versions/dynamx.svg" alt="Versions">
    </a>
</p>

<h1 align="center">DynamX - Realistic Physics for Minecraft</h1>

<p align="center">
    Welcome to the official repository of DynamX, a Minecraft mod that adds realistic physics into the game. With DynamX, you can add vehicles, props, ragdolls, blocks, and armor into your game, all of which follow the laws of physics and come with highly detailed models.
</p>

## 🛠 Creating an Add-on

Interested in creating an add-on for DynamX? You can find a step-by-step guide on how to make add-ons [here](https://dynamx.fr/wiki/installation/devsetup/).

Include the following in your `build.gradle` to set up your workspace:

```gradle
repositories {
    maven {
        url 'https://maven.dynamx.fr/artifactory/DynamXRepo'
    }
    maven {
        url 'https://maven.dynamx.fr/artifactory/ACsGuisRepo'
    }
}

dependencies {
    implementation "fr.dynamx:DynamX:[VERSION]"
    implementation "fr.aym.acsguis:ACsGuis:1.2.9"
}
```

After this, add DynamX to your `mods` folder.

## 🌟 Contributing to DynamX

To contribute to DynamX, reload the Gradle project to set up the workspace. To run your game with the DynamX core mod, add the following line to the VM options of the run configuration in your IDE:

```
-Dfml.coreMods.load=fr.dynamx.common.core.DynamXCoreMod
```

## 📄 License

DynamX is licensed under the Creative Commons Attribution-NonCommercial-NoDerivatives 4.0 License.

## 🙏 Acknowledgments

The DynamX team would like to express our heartfelt gratitude to:

+ Stephen Gold, the creator of [Libbulletjme](https://github.com/stephengold/Libbulletjme), our physics engine, for his unwavering support and kindness.
+ Sylano, for supplying us with 3D models for DynamX.
+ [DrawLife](https://discord.gg/tEWfWmASn6) and [StateMC](https://www.statemc.de/) for their diligent bug reports and extensive testing.
+ Blacknite, Ertinox, and MK for their extensive use of DynamX and their valuable suggestions.
+ All the content creators (addons & content packs) for using DynamX and creating amazing content with it. We appreciate your support! ❤️

## 🔗 Links

+ **Website:** [DynamX](https://dynamx.fr)
+ **Wiki:** [DynamX Wiki](https://dynamx.fr/wiki/)
+ **CurseForge:** [DynamX on CurseForge](https://www.curseforge.com/minecraft/mc-mods/dynamx)
+ **Discord:** [Join us on Discord](https://discord.gg/y53KGzD)
