# [![](http://cf.way2muchnoise.eu/full_dynamx_downloads.svg)](https://www.curseforge.com/minecraft/mc-mods/dynamx) [![](https://cf.way2muchnoise.eu/versions/dynamx.svg)](https://www.curseforge.com/minecraft/mc-mods/dynamx)
![](https://dynamx.fr/banner_dynamx_2.png)

## Official Repository of DynamX

DynamX is a mod for adding objects with realistic physics such as vehicles, props and ragdolls to Minecraft. DynamX also allows you to add blocks and armor, using highly detailed models.

## Setup workspace to create an addon

You can find a full explanation on how to make addons there : https://dynamx.fr/wiki/installation/devsetup/

In Forge, add the following to your `build.gradle`

```gradle
repositories {
    maven {
        url 'http://maven.dynamx.fr/artifactory/DynamXRepo'
    }
    maven {
        url 'https://maven.dynamx.fr/artifactory/' + 'ACsGuisRepo'
    }
}

dependencies {
    deobfCompile "fr.dynamx:DynamX:[VERSION]"
    compile "fr.aym.acsguis:ACsGuis:1.2.3-2"
}
```

## Contribution

In order to setup DynamX workspace you just need to run 
```gradle
gradlew setupDecompWorkspace
```
and add to the VM options of the run configuration in your IDE
```
-Dfml.coreMods.load=fr.dynamx.common.core.DynamXCoreMod
```

DynamX is distributed under Creative Commons Attribution-NonCommercial-NoDerivatives 4.0 license.

## Acknowledgments

The whole team of DynamX is grateful to :

+ Stephen Gold, the creator of [Libbulletjme](https://github.com/stephengold/Libbulletjme) (the library used by DynamX to handle the physics). For his sympathy and his help
+ GreenPeople, for the 3d models he provided for DynamX
+ [DrawLife](https://discord.gg/tEWfWmASn6) and [StateMC](https://www.statemc.de/), for all the bug report, and their countless tests
+ Blacknite and MK, for his intensive use of DynamX and this helpful suggestions
+ All the content creators (addons & content packs) for using DynamX and making wonderful things with it <3


## Links
Website: https://dynamx.fr  
Wiki: https://dynamx.fr/wiki/   
CurseForge: https://www.curseforge.com/minecraft/mc-mods/dynamx     
Discord: https://discord.gg/y53KGzD 
