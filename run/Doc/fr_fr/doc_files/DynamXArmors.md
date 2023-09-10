Cette page explique comment utiliser DynamX pour ajouter des armures avec un modèle .obj.

### Préparation du modèle

Une fois que vous avez modélisé votre armure sur Blender, vous devez séparer les différentes parties qui doivent avoir ces noms :

| Partie de l'armure | Nom de l'objet du modèle | Position dans Blender (x y z) |
| ------------------ | ------------------------ | ----------------------------- |
| Corps              | bodyModel                | 0 0 0                         |
| Tête               | headModel                | 0 0 0                         |
| Bras gauche        | leftArmModel             | -0.3125 0 -0.125              |
| Bras droit         | rightArmModel            | 0.3125 0 -0.125               |
| Jambe gauche       | leftLegModel             | 0.11875 0 -0.75               |
| Jambe droite       | rightLegModel            | 0.11875 0 -0.75               |
| Pied gauche        | leftFeetModel            | -                             |
| Pied droit         | rightFeetModel           | -                             |

Voici la structure que vous devriez avoir :

![table](armor_model_structure.png)

!!!info
	Ceci est une structure conseillée, vous verrez plus loin que vous pouvez mettre moins d'objets (uniquement un casque par exemple).

!!!tip
	Affichez le steve dans Blender pour bien dimensionner les différentes parties de l'armure.

### Exportation du modèle

Exportez le modèle ainsi créé, au format obj, accompagné du .mtl et du(des) .png associé(s), puis placez les dans un dossier `assets/dynamxmod/models/obj/nom_de_votre_armure/` de votre pack.

!!!warning
	Faites attention au nom des objets donnés par défaut par Blender (ouvrez l'obj exporté avec un éditeur de texte, et vérifiez que vous avez bien `o bodyModel` pour le corps), faites de même pour les autres objets du modèle...

### Configuration de l'armure

Créez un dossier `armors` dans votre pack, ensuite créez-y un fichier `armor_votre_armure.dynx`.

!!!warning
	Pour être reconnu, le nom du fichier doit commencer par "armor_" !

Voici les différentes propriétés à renseigner :

${ArmorObject.md}

Pour modifier le rendu de l'item, rendez-vous ici : [configuration des items](DynamXItems.md).

Vous avez configuré la base de l'armure, maintenant il faut configurer comment elle sera affichée :

- Configuration de la tête :

  Ajoutez `ArmorHead: headModel` pour ajouter un item de casque dessinant l'objet headModel du modèle.

- Configuration du corps:

  Pour ajouter un item de plastron, ajoutez `ArmorBody: bodyModel` pour dessiner l'objet bodyModel du modèle, et ajoutez `ArmorArms: leftArmModel rightArmModel` pour dessiner les objets leftArmModel et rightArmModel du modèle.

- Configuration des jambes :

  Ajoutez `ArmorLegs: leftLegModel rightLegModel` pour ajouter un item de jambières dessinant les objets leftLegModel et rightLegModel du modèle.

- Configuration des pieds :

  Ajoutez `ArmorFoot: leftFeetModel rightFeetModel` pour ajouter un item de bottes dessinant les objets leftFeetModel et rightFeetModel du modèle.

- #### Configuration de variantes de texture

Cette partie est expliquée dans [configuration du multi-textures](MultiTextures.md).

!!!info
	Si vous mettez plusieurs textures sur votre armure, elle ne pourra pas avoir de barre de durabilité.

### Conclusion

Voilà, vous avez mis votre armure en jeu, si vous avez d'autres questions, rendez-vous sur le Discord de DynamX !