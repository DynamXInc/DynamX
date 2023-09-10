Cette page vous donne les configurations spécifiques aux voitures, camions, deux-roues et remorques. Assurez-vous d'avoir déjà configuré [la base](ModularVehicleInfo.md) de celle-ci.

### Prérequis

Vous devez avoir [exporté](Exportation.md) les modèles obj et [configuré](ModularVehicleInfo.md) toute la base du véhicule.

### Configuration du véhicule

#### Configuration du volant

Pour ajouter un volant, ajoutez ceci dans la configuration du véhicule :

```
SteeringWheel{
    PartName: SteeringWheel
    Position: x y z
    BaseRotationQuat: w x y z
}
```

Voici toutes les propriétés possibles :

${SteeringWheelInfo.md}

#### Configuration d'une remorque :

Si votre véhicule est une remorque (pas de moteur), ou si on peut attacher une remorque à cette voiture, ajoutez :

```
Trailer{
    AttachPoint: x y z
    AttachStrength: force_maximale
}
```

Voici toutes les propriétés possibles :

${TrailerAttachInfo.md}

!!!warning
    Si vous configurez une remorque, assurez vous que votre fichier se nomme `trailer_votre_vehicule.dynx` !

### Conclusion

Vous avez de terminé de configurer la voiture, les prochaines étapes seront de configurer [son moteur](EngineInfo.md) (et ses sons) puis [ses roues](WheelInfo.md) !
Si vous voulez, vous pourrez également lui ajouter des [portes](../Doors.md) ouvrables et des [phares](../Lights.md) fonctionnels.
