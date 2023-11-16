Cette page vous donne les configurations spécifiques aux bateaux. Assurez-vous d'avoir déjà configuré [la base](ModularVehicleInfo.md) de celui-ci.

### Prérequis

Vous devez avoir [exporté](Exportation.md) les modèles obj et [configuré](ModularVehicleInfo.md) toute la base du bateau.

### Configuration du bateau

Un bateau, c'est plutôt simple, il suffit de lui ajouter des flotteurs, et un propulseur (qui simule aussi bien l'effet d'un moteur que de voiles).

#### Configuration des flotteurs

Un flotteur ressemble à cela (exemple des flotteurs centraux du zodiac du pack de base) :
```
Float_Core{
    Position: 0 0 -0.6
    Scale: 1 2.5 1.7
    Offset: 0 0 -0.9
    LineSize: 2 0 7
    BuoyCoefficient: 1.2
}
```

!!!info
    Waw, c'est compliqué !
    Pas de panique, on va revenir sur tous ces paramètres.

Les deux premières lignes définissent respectivement la position (le centre) et la taille du flotteur, comme beaucoup d'autres éléments dans DynamX.

L'`Offset` permet de facilement décaler le flotteur si le bateau penche d'un côté ou de l'autre (c'est un équivalent de la position).

Ensuite, entrons dans les détails du fonctionnement d'un flotteur. Pour calculer la flottaison, le flotteur que vous êtes en train de configurer sera découpé en plusieurs "sous-cubes", où sera calculée la poussée d'Archimède (c'est elle qui fait flotter les bateaux). `LineSize` correspond au nombre de cubes sur chaque axe, ici 2 sur l'axe X et 7 sur l'axe Z. L'axe Y est ignoré par DynamX et le 0 revient à mette un 1.

Enfin, `BuoyCoefficient` correspond à un coefficient de flottaison : augmentez-le pour que le bateau flotte mieux, ou au contraire diminuez-le si il ne flotte pas assez.

!!!info
    Pour vous aider à la configuration, les flotteurs sont visibles en activant l'option 'FLOATS' du menu de debug DynamX.
    Vous pouvez par exemple regarder les flotteurs du Zodiac (pack DynamX de base).

Et voici, au cas-où la liste des paramètres configurables :

${PartFloat.md}

Vous pouvez mettre autant de flotteurs que vous voulez, tant que leur nom commence par "Float".

#### Configuration de la propulsion

Ce bloc permet de configurer le point où les forces de propulsion et de direction sont appliquées. Même si vous utilisez un bateau à voile, c'est important ! Voici un exemple, utilisé pour le zodiac du pack de base :

```
BoatPropeller{
    Position: 0 2.60954 -0.53935
    AccelerationForce: 9000
    BrakeForce: 7000
    SteerForce: 200
}
```

Voici la liste des paramètres configurables :

${BoatPropellerInfo.md}

### Conclusion

Vous avez de terminé de configurer votre premier bateau ! Si besoin, vous pouvez continuer en configurant [son moteur](EngineInfo.md) (et ses sons).
Si vous voulez, vous pourrez également lui ajouter des [portes](../Doors.md) ouvrables et des [phares](../Lights.md) fonctionnels.
