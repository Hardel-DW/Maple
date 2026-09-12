# Mapple - Optimisations
Mapple fonctionne sur n'importe quel serveur Fabric/NeoForge en server-side à partir de la 26.1. Il réduit la mémoire RAM d'un serveur Minecraft. Il est côté serveur, sans config, et ne change rien au jeu.

Mapple a été pensé pour tirer parti des optimisations à grande échelle qui scalent, notamment pour le bon fonctionnement de **[Leafs](https://modrinth.com/mod/leafs)**, le mod qui fait tourner le monde en multithread.

![Delimeter](https://cdn.modrinth.com/data/cached_images/c57c204c55df0ce5357df6501f616f2c7b7c6df1.png)

# Ce qu'il optimise
La grosse partie de la mémoire d'un serveur, c'est des blocs d'air qui surallouent de la RAM, et tout ce que le jeu garde autour des chunks au cas où. Mapple retire ce qui ne sert à rien.

- **L'air.** Trois sections de chunk sur quatre sont de l'air pur. Mapple les fusionne en une seule instance.
- **Les copies de chunks.** Pour chaque chunk chargé, le jeu garde une copie vide qu'il ne lit jamais. Mapple la supprime.
- **Les protections.** Chaque morceau de chunk embarque une protection lourde contre les accès simultanés. Mapple la remplace par une version légère qui fait le même travail.
- **Les palettes de blocs.** Quand un chunk contient peu de sortes de blocs, Mapple les range plus serré en mémoire. Sur le disque, rien ne change.
- **Les listes.** Le jeu tient des listes de POI, de structures et de types de chunks. Elles grossissent avec chaque chunk visité et ne rétrécissent jamais. Mapple les nettoie quand un chunk se décharge.
- **Buffer de pathfinding partagé.** Chaque mob possède ses propres buffers de pathfinding qui ne font que grossir, même quand il ne bouge pas. Les mobs se partagent cette mémoire.
- **Stockage froid des chunks inactifs.** Un chunk qui attend dans la chaîne de génération est compressé en LZ4 jusqu'à ce que quelqu'un en ait besoin.
- **Le disque.** Un chunk qui a seulement été chargé n'est pas réécrit sur le disque s'il n'est pas modifié. Mapple ne sauvegarde que ce qui a bougé.

![Delimeter](https://cdn.modrinth.com/data/cached_images/c57c204c55df0ce5357df6501f616f2c7b7c6df1.png)

# Les gains
Testés avec Leafs, chaque joueur isolé qui génère des nouveaux chunks coûte 33 Mo au lieu de 52 Mo.

| Mesure | Vanilla | Mapple |
|---|---|---|
| Mémoire | 2 220 Mo | 1 560 Mo |
| Objets en mémoire | 31 millions | 16 millions |
| Chunks écrits à l'autosave | 23 210 | 3 377 |
| Nœuds de pathfinding | 326 705 | 67 854 |

# FAQ
**Compatible avec Lithium et FerriteCore ?**
Oui, et les deux sont recommandés.

**Compatible avec C2ME, Moonrise ou VMP ?**
Non testé. Ils réécrivent les mêmes structures de chunks.

**Est-ce que ça change le jeu ?**
Non. C'est juste des optimisations sans conséquence.
