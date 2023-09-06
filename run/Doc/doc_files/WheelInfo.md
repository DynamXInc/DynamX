Cette page vous apprendra à mettre des roues sur vos voitures, deux-roues et autres remorques.

### Prérequis

Vous devez avoir [configuré](CarInfo.md) un premier véhicule terrestre.

### Configuration des roues

Pour chaque type de roue présente sur votre véhicule, créez un fichier `wheel_votre_roue.dynx` (de préférence dans le même dossier que votre véhicule).

La nom de la roue par défaut à indiquer dans la configuration du véhicule sera alors `AttachedWheel: VotrePack.votre_roue`.

!!!warning
    Pour être reconnu, le nom du fichier doit commencer par "wheel_" !

Voici les différentes propriétés à renseigner :

*Les valeurs conseillées sont données à titre indicatif, mais vous devrez tester des heures durant pour arriver à une bonne tenue de route*.

${PartWheelInfo.md}

!!!tip
    Une même roue peut être utilisée sur plusieurs véhicules !

!!!tip
    La suspension peut être fastidieuse à configurer, car beaucoup de ces propriétés ont une influence les une sur les autres, soyez patients, et vigilants ! Vous pouvez aussi copier les valeurs conseillées, ou celles de packs existants...

!!!info
    La configuration de la largeur et du rayon de la roue seront peut-être simplifiés dans le futur.

##### Faire une moto

Pour faire une moto, ajoutez deux roues sur les côtés, invisibles en mettant `Model: disable_rendering` dans leur config, en les plaçant légèrement plus haut que le sol et en ne leur mettant aucune force de freinage. Après, c'est assez long à équilibrer, tout comme les suspensions... Pensez à mettre un centre de gravité un peu au dessus de 0 pour avoir un comportement réaliste de la moto.

### Conclusion

Bravo, si vous avez tout fait dans l'ordre, vous venez de mettre votre première voiture en jeu ! Si vous avez d'autres questions, rendez-vous sur le Discord de DynamX !
