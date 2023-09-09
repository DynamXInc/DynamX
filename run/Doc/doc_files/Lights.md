### Introduction

Cette page vous apprendra à ajouter des lumières sur vos blocs et véhicules. De base dans DynamX, une lumière est une part qui restera lumineuse la nuit, mais qui n'émettra pas de lumière. Voici deux exemples :

![image-1](images/ford_explorer.png){ width="49%" }
![image-2](images/firetruck.png){ width="49%" }

Cela permet de faire des phares, des clignotants, des gyrophares et bien plus encore.

!!!info
    Le support des lumières sur les items et armures est prévu et arrivera plus tard.

En ajoutant BetterLights, qui est en cours de création (pas encore public), vous pourrez bientôt avoir des parts qui *émettent* réellement de la lumière, en éclairant les blocs et entités, et faire des vrais phares, des lampes torches, etc.

### Prérequis 
Vous devez avoir configuré au moins un bloc ou un véhicule.

!!!danger
    Cette version du tutoriel fonctionne uniquement sur les versions 4.0.0 et ultérieures de DynamX.

### Préparation du modèle

Dans votre modèle 3D, veillez à bien séparer chaque objet pouvant s'allumer séparément, et à les nommer d'une manière assez claire. Ensuite, ajoutez-leur au moins une texture pour l'état allumé, de la même manière que dans l'étape "Configuration du modèle" du MultiTextures : [ici](../MultiTextures/#configuration-du-modele).

### Configuration

#### Ajouter l'état allumé à la part lumineuse

Les lumières se configurent à l'aide du bloc "MultiLight", dont voici un exemple pour des feux arrières :

```
MultiLight_rear{
    PartName: rearlight
    LightObject_rearlights{
        LightId: 4
        Textures: on
    }
    LightObject_stop{
        LightId: 6
        Textures: on
    }
}
```

`MultiLight_rear` créée une nouvelle source de lumière, vous pouvez mettre nimporte quel suffixe à la place de 'rear'. `PartName: rearlight` désigne la part dans le modèle obj. Ici, le phare arrière gauche et le phare arrière droit sont dans la même part, c'est plus simple à configurer. Pour chaque part du modèle, vous ne devez avoir qu'un seul `MultiLight`.

Les sous-blocs `LightObject_rearlights` et `LightObject_stop` créent deux nouveau types de lumière : les feux arrières de la voiture quand les phares de nuit sont allumés, et les feux arrières allumés quand on freine. Pour les feux nocturnes, `LightId: 4` désigne l'id de la lumière, et plusieurs `LightObject` peuvent partager le même id. Quand vous voudrez allumer le phare, avec le [BasicsAddon](https://github.com/DynamXInc/BasicsAddon#lights-and-sounds) par exemple, c'est cet id qu'il faudra donner. `Textures: on` désigne enfin la texture allumée du phare, telle que configurée dans le fichier mtl (voir plus haut).

Pour compléter les feux arrières, vous pouvez de la même manière ajouter les phares avants :
```
MultiLight_front{
    PartName: light
    LightObject {
        LightId: 4
        Textures: on
    }
}
```
Notez que le `LightObject` n'a ici pas de nom : pas besoin comme il n'y en a qu'un seul, et qu'il partage le même `LightId` que `LightObject_rearlights` : les deux s'allumeront en même temps.

#### Faire clignoter les lumières

Voici un exemple de clignotants à gauche. Pour une configuration plus simple, dans le modèle obj les clignotants avant et arrières sont dans la même part.
```
MultiLight_turnleft {
    PartName: left
    LightObject{
        LightId: 2
        Textures: on
        BlinkSequenceTicks: 8 15
    }
}
```
Par rapport à avant, on a rajouté la ligne `BlinkSequenceTicks: 8 15`. C'est une séquence de ticks (20 ticks correspondent à une seconde), indicant en altenance le temps auquel la lumière s'allume puis s'éteint : avec `8 15`, la lumière s'allumera au bout de 8 ticks, s'éteindra au bout de 15 ticks, puis (on repart à 0) elle s'allumera de nouveau après 8 ticks. Avec `BlinkSequenceTicks: -1 0 8 15` commencera dans l'état allumé, puis s'éteindra après 8 ticks, et se rallumera au bout de 15 ticks.
Vous pouvez également faire des séquences d'autres longueurs, et plus compliquées, par exemple avec `BlinkSequenceTicks: -1 0 2 4 6 18`, la lumière commencera allumée, puis s'éteindra après 2 ticks, puis se rallumera au bout de 4 ticks, puis s'éteindra à 6 ticks, puis se rallumera à 18 ticks.

#### Ajouter une spotlight (avec le mod BetterLights)

Cette partie explique comment ajouter un projecteur : la source de lumière éclairera vraiment les blocs et entités devant elle, à l'aide de shaders et du mod BettetLighs.

!!!warning
    Le mod BetterLights n'est pas encore disponible !

Modifiez le phare arrière précédent comme suit :
```
MultiLight_rear{
    PartName: rearlight
    LightObject_headlights{
        LightId: 4
        Textures: on
        SpotLightColor: 0.5 0.1 0.1 1
    }
    LightObject_stop{
        LightId: 6
        Textures: on
        SpotLightColor: 0.9 0.1 0.1 1
    }
    SpotLight_left {
        Offset: 0.9 -0.1 -0.8
        Direction: 0 -0.3 -1
        Angle: 15
        Distance: 20
    }
    SpotLight_right {
        Offset: -0.9 -0.1 -0.8
        Direction: 0 -0.3 -1
        Angle: 15
        Distance: 20
    }
}
```
On a ajouté `SpotLightColor` aux LightObjects : ce sera la couleur de la lumière projetée quand la source est allumée. Si les deux LightObjects sont allumés, le dernier dans la liste sera appliqué.
Les blocs `SpotLight_left` et `SpotLight_right` définissent les projecteurs gauche et droite. 

- `Offset` correspond à leur position par rapport à l'origine du modèle 3D.
- `Direction` correspond à la direction de la lumière dans le repère du modèle.
- `Angle` correspond à la "largeur" de la lumière, en degrés.
- `Distance` correspond à la portée de la lumière, en mètres.

#### Liste des propriétés configurables

##### MultiLight
${PartLightSource.md}

##### LightObject
${SubLightObject.md}

##### SpotLightObject (BetterLights)
Coming soon: ${SpotLightObject.md}

### Conclusion

Vous savez maintenant comment mettre des lumières sur les objets DynamX ! Si vous rencontrez des difficultés ou que ce tutoriel n'est pas assez clair, vous pouver aller demander de l'aide sur le discord de DynamX !

