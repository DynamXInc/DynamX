Cette page vous donne les configurations spécifiques aux hélicoptères. Assurez-vous d'avoir déjà configuré [la base](ModularVehicleInfo.md) de celui-ci.

### Prérequis

Vous devez avoir [exporté](Exportation.md) les modèles obj et [configuré](ModularVehicleInfo.md) toute la base de l'hélicoptère.

### Configuration de l'hélicoptère

Pour configurer un hélicoptère, il vous faudra un rotor, une physique, et un joystick. Voici comment tout configurer :

#### Configuration du rotor

Voici un exemple de configuration d'un rotor :

```
Rotor_Main{
	Position: 0 0.432414 3.99325
	RotationAxis: 0 1 0
	RotationSpeed: 70
	PartName: main_rotor
}
```

`Position` défini la position du rotor sur l'hélicoptère. `RotationAxis` défini son axe de rotation : avec `0 1 0` il sera vertical, comme le rotor principal d'un hélicoptère, et avec `1 0 0`, il sera horizontal comme le rotor arrière. `RotationSpeed` défini la vitesse de rotation du rotor à la puissance maximale de l'hélicoptère.

`PartName` défini la part du modèle 3D à dessiner. Elle doit être séparée du chassis et en position `0 0 0` au moment d'exporter le modèle (comme pour le volant d'une voiture).

Voici la liste de tous les paramètres configurables :

${PartRotor.md}

Vous pouvez mettre autant de rotors que vous voulez, tant que leur nom commence par "Rotor".

#### Configuration de la physique

Ici, nous allons définir la physique de l'hélicoptère. Il y a beaucoup de paramètres mais ce n'est pas si compliqué. Voici un exemple :
```
HelicopterPhysics{
    MinPower: 0.4
    InclinedGravityFactor: 1.8
    ThrustForce: 3000
    VerticalThrustCompensation: 2000
    BrakeForce: 500
    MouseYawForce: 2600
    MousePitchForce: 2000
    MouseRollForce: 400
    RollForce: 6000
}
```

Tous ces paramètres sont expliqués ici :

${HelicopterPhysicsInfo.md}

#### Configuration des manettes de pilotage

Coming soon.

${PartHandle.md}

### Conclusion

Bravo, si vous avez tout fait dans l'ordre, vous venez de mettre votre premièr hélicoptère en jeu ! Si vous avez d'autres questions, rendez-vous sur le Discord de DynamX !
Vous pouvez ensuite lui ajouter des [portes](../Doors.md) ouvrables et des [phares](../Lights.md) fonctionnels.
