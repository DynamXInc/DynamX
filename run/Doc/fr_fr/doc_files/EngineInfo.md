Cette page vous apprendra à mettre un moteur sur votre voiture, bateau ou hélicoptère.

### Prérequis

Vous devez avoir [configuré](ModularVehicleInfo.md) un premier véhicule.

### Configuration du moteur :

Créez un fichier `engine_votre_moteur.dynx` (de préférence dans le même dossier que votre véhicule).

La nom du moteur par défaut à indiquer dans la configuration du véhicule sera alors `DefaultEngine: VotrePack.votre_moteur`.

!!!warning
    Pour être reconnu, le nom du fichier doit commencer par "engine_" !

Voici les différentes propriétés à renseigner :

${CarEngineInfo.md}

##### Configuration de la courbe de puissance :

!!!info
    Cette configuration n'est utile que pour les voitures et bateaux. Elle est inutile pour les hélicoptères.

La puissance d'un moteur varie selon ses tours par minute (RPM), voici comment le configurer, à l'aide de ses points :

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

Où ratio est le ratio de puissance (entre 0 et 1) et rpm les tours minutes correspondants. Vous pouvez mettre autant de points que vous voulez.

!!!danger
    Le nombre de rpm du dernier point doit être plus grand ou égal aux MaxRPM du moteur (défini plus haut).

##### Configuration des vitesses :

!!!info
    Cette configuration n'est utile que pour les voitures et bateaux. Elle est inutile pour les hélicoptères.

Les vitesses sont passées de manière automatique aux paliers définis dans la configuration du moteur.

Exemple de la configuration des vitesses de la ds7 :

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

Explications :

Gear_0 est la marche arrière, Gear_1 est le point mort, et Gear_2, ..., Gear_7 sont les vitesses de marche avant (vous pouvez en mettre autant que vous le souhaitez). 

Voici la définition des propriétés :

${GearInfo.md}

### Configuration des sons du moteur :

Créez un fichier `sounds_vos_sons.dynx` (de préférence dans le même dossier que votre véhicule).

La nom des sons par défaut à indiquer dans la configuration du véhicule sera alors `DefaultSounds: VotrePack.vos_sons`.

!!!warning
    Pour être reconnu, le nom du fichier doit commencer par "sounds_" !

Voici la structure à respecter (exemple de la ds7) :

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

Explications :

Les sons intérieurs (Interior) et extérieurs (Exterior) sont séparés dans deux blocs distincts.

Pour chaque son, vous devez spécifier le nom de son fichier dans la propriété "Sound" de telle sorte que pour un son "
test" placé dans `assets/dynamxmod/sounds/test.ogg`, vous ayez `Sound: test`.

!!!warning 
    Comme dans Minecraft vanilla, seuls les sons .ogg sont supportés.

Le son "Starting" est joué en allumant le moteur, il ne nécessite pas de PitchRange.

Les autres sons dépendent des tours minutes du moteur, par exemple, le son "0-1500" sera joué entre 0 et 1500 tours par
minutes. Dans ce cas, "PitchRange" défini la hauteur du son à 0 rpm et à 1500 rpm (si pitch vaut 1, le son sera le même
que dans le .ogg, si pitch vaut 0.5 il sera très grave, et au contraire il sera très aigu si pitch vaut 2). Entre chaque
son, une transition est faite par le mod.

!!!info
    Pour les hélicoptères, les RPM sont définis en fonction de la puissance donnée par le joueur. Cela correspond directement à la vitesse de rotation du rotor.

Voici la définition des propriétés :

${EngineSound.md}

!!!tip
    Le fonctionnement des sons peut être modifié avec les évènements de l'API du mod (voir la documentation dédiée).

### Conclusion

Si vous avez tout fait dans l'ordre, il vous restera peut-être [des roues](WheelInfo.md) à configurer ;)
