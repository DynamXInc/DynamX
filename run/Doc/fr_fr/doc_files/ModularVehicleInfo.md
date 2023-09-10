Cette page vous guidera dans la création de votre véhicule, allant de la voiture à l'hélicoptère, en passant par les deux roues, remorques et bateaux.

### Prérequis

Vous devez avoir [exporté](Exportation.md) les modèles obj.

### Configuration du véhicule

Que vous souhaitez créer une voiture, un deux-roues, une remorque, un bateau ou un hélicoptère, cette partie est commune à tous les types de véhicules.

Créez un dossier portant le nom de votre véhicule dans votre pack, ensuite créez-y un fichier avec un des noms suivants, suivant ce que vous voulez ajouter :

| Type de véhicule    | Préfixe        | Exemple                                                |
| -------- | -------------- | ------------------------------------------------------------ |
| Voiture, camion ou deux-roues | vehicle_ | `vehicle_votre_vehicule.dynx` |
| Remorque     | trailer_          | `trailer_votre_vehicule.dynx` |
| Bateau   | boat_         | `boat_votre_vehicule.dynx` |
| Hélicoptère     | helicopter_         | `helicopter_votre_vehicule.dynx` |

!!!warning
    C'est le préfixe qui détermine le type de votre véhicule ! Si vous mettez quelque chose de différent, il sera ignoré.

!!!danger
    Faites attention aux deux formats de positions utilisés : dans Blender l'axe vertical est l'axe Z, alors que dans Minecraft c'est l'axe Y, de plus l'axe Z de Minecraft est dans le sens inverse (coordonnées multipliées par -1). Nous préciserons quelle format utiliser à chaque fois que c'est nécessaire.

	Format Blender : `X Y Z`. Format Minecraft : `X Z -Y`.

Voici les différentes propriétés à renseigner :

${ModularVehicleInfo.md}

!!!info
    La traduction du nom du véhicule est automatiquement créée avec la valeur de "Name".

Pour modifier le rendu de l'item, rendez-vous ici : [configuration des items](https://dynamx.fr/wiki/dynamx/DynamXItems/).


#### Configuration des boîtes collisions :

Ces boites de collisions sont utilisées pour la collision avec les autres véhicules *si l'option `UseComplexCollisions` est désactivée*. Dans tous les cas elles sont utilisées pour les collisions avec les joueurs et il faut obligatoirement les configurer.

Découpez votre modèles en plusieurs cubes représentatifs des collisions à faire avec les joueurs, puis pour chaque cube, ajoutez à la configuration du véhicule :

```
Shape_NomDuCube{
	Position: x y z
	Scale: x y z
}
```

Position est la position du cube, et Scale sa taille. Les coordonnées sont au format blender ! Attention : NomDuCube doit être unique.

Voici, par exemple, ce que ça donne avec le trophy truck (trois cubes de collision) :

![collision_shapes](collision_shapes.png)

!!!info
    La configuration de ces collisions sera peut-être simplifiée dans le futur.

#### Configuration des roues :

Pour chaque roue présente sur le véhicule (vous pouvez en mettre autant que vous voulez), ajoutez à la configuration du véhicule :

```
Wheel_NomDeLaRoue{
    AttachedWheel: VotrePack.roue_par_defaut
    IsRight: false
	Position: x y z
	IsSteerable: True
	MaxTurn: 0.7
	DrivingWheel: False
}
```

Voici toutes les propriétés possibles :

${PartWheel.md}


#### Configuration des sièges:

Pour chaque siège présent sur le véhicule, ajoutez à la configuration du véhicule :

```
Seat_NomDuSiege{
    Position: x y z
    Driver: True
}
```

Où Position est la position du siège au format Blender, et Driver indique si c'est le siège conducteur (true), ou non (false).

Voici toutes les propriétés possibles :

${PartSeat.md}

#### Configuration des variantes de textures :

Cette partie est expliquée dans [configuration du multi-textures](../MultiTextures.md).

#### Configuration de points de friction avec l'air :

Vous pouvez configurer des points où seront appliquées des forces de frictions avec l'air, permettant de fortement améliorer la tenue de route des vos véhicules, contre un temps de configuration plus long.

Ces points se configurent "au jugé", mais le mieux est d'en placer un vers l'avant du véhicule, centré entre les deux roues.

Exemple de configuration, pour le trophy truck du pack de base :

```
ForcePointNom{
    Position: 0 -0.89534 0
    Intensity: 0.4 1 0.4
}
```

Voici toutes les propriétés possibles :

${FrictionPoint.md}

"Nom" peut être remplacé par n'importe quel nom (vous pouvez ainsi avoir plusieurs points de friction).
Ces points sont visibles dans le debug "Friction points", vous verrez aussi les forces appliquées.


#### Autres configurations :

- [Configuration des phares](../Lights.md).
- [Configuration des portes](../Doors.md).

### Configuration d'une voiture ou d'une remorque

Rendez-vous [ici](CarInfo.md) !
Cette partie est également valable pour les motos et autres deux-roues.

### Configuration d'un bateau

Rendez-vous [ici](BoatInfo.md) !

### Configuration d'un hélicoptère

Rendez-vous [ici](HelicopterInfo.md) !

### Configuration d'un avion

*Malheureusement, ce n'est pas encore possible :'(*.

### Conclusion

Bravo, vous avez mis votre véhicule en jeu, si vous avez d'autres questions, rendez-vous sur le Discord de DynamX !
