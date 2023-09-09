### Introduction

Cette page vous apprendra à ajouter des portes ouvrables et ayant une physique sur les véhicules.

!!!warning
    Lest portes peuvent parfois bugger et mal se synchroniser, notament en multijoueur, ce sera amélioré dans les prochaines versions de DynamX ;)

!!!info
    Le support des portes sur les blocs et props est prévu et arrivera plus tard.

### Prérequis 
Vous devez avoir configuré un véhicule.

!!!danger
    Cette version du tutoriel fonctionne uniquement sur les versions 4.0.0 et ultérieures de DynamX.

### Préparation du modèle

Dans votre modèle 3D, séparez chaque porte dans un objet séparé et exportez le modèle en les mettant à l'origine (point 0 0 0) du modèle, comme pour le volant par exemple.

A noter que seules les portes ayant une rotation sont supportées, vous ne pouvez pas faire de porte qui suit un translation (comme la porte d'un berlingo par exemple).

### Configuration

Voici un exemple de configuration d'une porte :
```
leftfrontdoor{
    Position: 1.0403 -0.2035 0.8
    Scale: 0.102 0.509 0.436
    LocalCarAttachPoint: 1.1038 -0.95709 0.93769
    LocalDoorAttachPoint: 0 -0.78 0

    OpenedDoorAngleLimit: 0 0.78
    ClosedDoorAngleLimit: 0 0
    DoorOpenForce: 1 200
    DoorCloseForce: -1.5 300
}
```
Les valeurs sont bien sur à adapter selon votre modèle, et le sens dans lequel la porte doit s'ouvrir.

Et voici l'explication de chacune des propriétés :
${PartDoor.md}

Pour vous aider à les configurer, vous pouvez utiliser l'option de debug '' :
![image-1](images/debug_doors.png){ width="90%" }

### Conclusion

Vous pouvez maintenant mettre des portes sur les véhicules DynamX ! Si vous rencontrez des difficultés ou que ce tutoriel n'est pas assez clair, vous pouver aller demander de l'aide sur le discord de DynamX.

