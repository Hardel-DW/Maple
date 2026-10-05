# Maple - Optimisations
Maple fonctionne sur n'importe quel serveur Fabric/NeoForge en server-side à partir de la 26.1. Il réduit la mémoire RAM d'un serveur Minecraft. Il est côté serveur, sans config, et ne change rien au jeu.

Maple a été pensé pour tirer parti des optimisations à grande échelle qui scalent, notamment pour le bon fonctionnement de **[Leafs](https://modrinth.com/mod/leafs)**, le mod qui fait tourner le monde en multithread.

![Delimeter](https://cdn.modrinth.com/data/cached_images/c57c204c55df0ce5357df6501f616f2c7b7c6df1.png)

# Ce qu'il optimise
La grosse partie de la mémoire d'un serveur, c'est des blocs d'air qui surallouent de la RAM, et tout ce que le jeu garde autour des chunks au cas où. Maple retire ce qui ne sert à rien, sans coûter de CPU.

- **L'air.** Trois sections de chunk sur quatre sont de l'air pur. Chaque chunk garde ses propres sections, mais toutes les vides partagent les mêmes données.
- **Les copies de chunks.** Pour chaque chunk utilisé par la génération de ses voisins, le jeu construit une copie vide qu'il ne lit jamais. Maple la supprime.
- **Les protections.** Chaque morceau de chunk embarque une protection lourde contre les accès simultanés, un contrôle de débogage. Maple la retire, comme Lithium.
- **Les listes.** Le jeu tient des listes de POI et de types de chunks. Elles grossissent avec chaque chunk visité et ne rétrécissent jamais. Maple les nettoie quand un chunk se décharge.
- **Buffer de pathfinding partagé.** Chaque mob possède ses propres buffers de pathfinding qui ne font que grossir, même quand il ne bouge pas. Les mobs se partagent cette mémoire.

![Delimeter](https://cdn.modrinth.com/data/cached_images/c57c204c55df0ce5357df6501f616f2c7b7c6df1.png)

# Les gains
Mesurés sur un serveur sans Leafs, 5 joueurs qui survolent des terres nouvelles à 36 blocs par seconde, distance de vue 10. Le monde généré est identique à vanilla, bloc pour bloc.

| Mesure | Vanilla | Maple |
|---|---|---|
| Mémoire vivante en fin de run | 0,66 à 0,69 Go | 0,52 à 0,53 Go |
| CPU par chunk généré | 27,9 ms | 26,7 ms |

# FAQ
**Compatible avec Lithium et FerriteCore ?**
Oui, et les deux sont recommandés. Maple ne désactive aucune option de Lithium.

**Compatible avec C2ME, Moonrise ou VMP ?**
Non testé. Ils réécrivent les mêmes structures de chunks.

**Est-ce que ça change le jeu ?**
Non. C'est juste des optimisations sans conséquence.
