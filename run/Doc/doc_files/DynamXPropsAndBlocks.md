DynamX permet d'ajouter des blocs et des props avec un modèle .obj. Voici un tutoriel complet pour l'ajout de ceux-ci en jeu.

!!!info 
	Un prop est un bloc soumis a la physique.

!!!warning
    Il est fortement conseillé d'exporter les modèles via Blender. Les modèles exportés par exemple via BlockBench ne sont pas optimisés et peuvent faire lag.

### Préparation du modèle

DynamX offre la possibiliter de créer automatiquement une boite de collision pour chaque `object` du fichier obj, cette boîte de collisions servira pour les collisions avec toutes les entités et les autres objets physiques.

Par exemple, si vous découpez votre modèle de cette manière (modèle de table) : 

![table](table.png)

L'algorithme va créer 2 boites de collisions, une pour les pieds et une pour le haut de la table, pensez-y en créant votre modèle (et évitez également de mettre trop d'objects inutiles risquant d'alourdir le bloc/prop).

### Exportation des modèles

Exportez le modèle , au format obj, accompagné du .mtl et des .png associés, puis placez les dans un dossier `assets/dynamxmod/models/obj/nom_de_votre_bloc(ou_prop)/` de votre pack.

### Configuration du bloc (ou prop)

- ##### Partie commune aux blocs et props

!!!info 
	Si vous voulez seulement créer un prop, vous devez d'abord créer le bloc correspondant, vous pourrez ensuite le retirer des creative tabs.

Créez un dossier `blocks` à la racine de votre pack puis créez-y un fichier `block_votre_block.dynx`.

!!!warning
	Pour être reconnu, le nom du fichier doit commencer par `block_`, que ce soit pour faire un bloc et/ou un prop !
!!!danger
	Faites attention aux deux formats de positions utilisés : dans Blender l'axe vertical est l'axe Z, alors que dans Minecraft c'est l'axe Y, de plus l'axe Z de Minecraft est dans le sens inverse (coordonnées multipliées par -1). Nous préciserons quelle format utiliser à chaque fois que c'est nécessaire.

	Format Blender : `X Y Z`. Format Minecraft : `X Z -Y`.

Voici la liste des variables permettant de configurer un bloc. 

!!!info
	Le prop configuré à l'aide de ce bloc reprendra les valeurs configurées sur le bloc, sauf pour la creative tab qu'il faudra de nouveau configurer (sauf si vous gardez celle par défaut).

${BlockObject.md}

!!!info
	La traduction du nom du bloc/prop est automatiquement créée avec la valeur de "Name"

- ##### Configuration du prop

Ajoutez-le code suivant dans le fichier du bloc : 

```
Prop_NomDuProp{
    //Configuration du prop ici
}
```

!!!info 
	Le prop est une sous-propriété du bloc dont le nom doit obligatoirement commencer par "Prop".

Voici la liste des variables permettant de configurer un prop. 

!!!info
	Les propriétés déjà renseignées dans le bloc sont optionnelles, le prop utilisera la config du bloc (sauf pour la creative tab qu'il faut également configurer dans le prop si vous ne voulez pas utiliser la creative tab par défaut).

${PropObject.md}

- ##### Configuration des variantes de textures

Cette partie est expliquée dans [configuration du multi-textures](../MultiTextures.md).

- ##### Fin de la configuration

Pour modifier le rendu de l'item du bloc/prop, rendez-vous ici : [configuration des items](https://dynamx.fr/wiki/dynamx/DynamXItems/).

### Conclusion 

Voilà, vous avez mis votre bloc et/ou prop en jeu, si vous avez d'autres questions, rendez-vous sur le Discord de DynamX !
Vous pouvez aussi ajouter des [lumières fonctionnelles](../Lights.md) sur votre bloc.
