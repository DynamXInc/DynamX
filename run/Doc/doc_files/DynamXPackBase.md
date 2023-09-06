DynamX permet d'ajouter facilement tout type de contenu (véhicules, armures, items, blocs et props) à Minecraft. Pour cela, vous devez créer un pack, c'est ce vous allez apprendre ici.

### Fonctionnement des packs

Les packs sont soit des dossiers, soit des fichiers compressés (zip) renommés avec l'extension .dnxpack.

Pour créer un pack, il est plus simple de travailler avec un dossier, à placer dans le **dossier DynamX** créé au premier lancement de Minecraft avec le mod.

Créez un dossier `MyPack` portant le nom de votre pack, il contiendra tous les fichiers de votre pack.

Dans ce pack, vous devrez ensuite créer un fichier `pack_info.dynx` qui va contenir les informations basiques au sujet de votre pack.

!!!warning
	Nous vous invitons à prendre connaissance de la [syntaxe des packs](../DynamXPackConfigsSyntax/) avant de remplir ce fichier.

### Fichier pack_info.dynx

Ce fichier est obligatoire dans chaque pack. Voici les propriétés qu'il est possible de renseigner :

${PackInfo.md}

!!!info
	La propriété PackName contient le nom de votre pack, et elle permet de renommer le dossier sans rien casser, tant que PackName reste le même. Celui-ci sera ré-utilisé à plusieurs autres endroits.

Voici un exemple parfait pour un pack utilisant DynamX version 4.0.0 ou plus :
```
PackName: MyPack
PackVersion: 1.0.0
DcFileVersion: 12.5.0
CompatibleWithLoaderVersions: [1.1.0,)
```

Si votre pack dépend d'addons, il est possible d'ajouter plusieurs blocs RequiredAddon comportant les propriétés suivantes :

${RequiredAddonInfo.md}

### Pack d'exemple

Vous pouvez télécharger des exemples de pack [ici](https://files.dynamx.fr/addons/) et partir de ceux-ci pour créer le votre !

!!!tip
	Les fichiers .dnxpack sont des fichiers .zip renommés : vous pouvez les ouvrir avec des logiciels comme WinRAR.

### Debugger un pack

Pour vous aider à créer des packs, DynamX affiche tous les erreurs de chargement dans le sous-menu "Erreurs" du menu de debug DynamX, accessible avec la commande `/dynamx debug_gui`.

### La suite

Vous pouvez maintenant passer à l'étape suivante : (re)voir la syntaxe des fichiers de configuration des packs, et commencer à ajouter du contenu à DynamX ! [C'est ici](../DynamXPackConfigsSyntax/).
