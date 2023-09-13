Cette page explique comment utiliser DynamX pour ajouter des items avec un modèle .obj.

Si vous souhaitez modifier le rendu de l'item d'un véhicule, bloc, prop ou d'une armure, rendez vous dans la deuxième partie de cette page.

### Ajouter un item simple

#### Préparation du modèle

Dans votre logiciel de modélisation, placez le centre de l'item en 0 0 0, endroit où sera placé la main.

#### Exportation du modèle

Exportez le modèle , au format obj, accompagné du .mtl et des .png associés, puis placez les dans un dossier `assets/dynamxmod/models/obj/nom_de_votre_item/` de votre pack.

#### Configuration de l'item

Créez un dossier `items` dans votre pack, ensuite créez-y un fichier `item_votre_item.dynx`.

!!!warning
	Pour être reconnu, le nom du fichier doit commencer par `item_` !

Voici les différentes propriétés à renseigner :

${ItemObject.md}

!!!info
	La traduction du nom de l'item est automatiquement créée avec la valeur de "Name".


### Modifier le rendu en jeu d'un item

Par défaut tous les items sont rendus en 3D avec leur modèle obj.

Il est possible de modifier ce rendu avec les options `ItemScale` et `Item3DRenderLocation` (facultatives).

`Item3DRenderLocation` indique à quel endroit votre item est rendu en 3D, voici les 3 valeurs possibles :

- `all` : l'item est rendu en 3D partout
- `world` : l'item est rendu en 3D partout sauf dans les menus (guis) où sa texture 2D est utilisée
- `none` : le rendu est toujours en 2D, la texture de l'item est utilisée

#### Si Item3DRenderLocation est différent de `all` :

Au premier lancement, un fichier json pour l'item, `assets/dynamxmod/models/item/votre_item.json` sera automatiquement créé dans votre pack, pointant vers une texture `assets/dynamxmod/textures/item/votre_item.png` (non créée). Vous pourrez par la suite ajouter la texture et/ou modifier le json.

### Conclusion 

Vous avez mis votre item en jeu, si vous avez d'autres questions, rendez-vous sur le Discord de DynamX !