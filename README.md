# Mapple - Optimisations
Mapple works on any Fabric/NeoForge server, server-side, from 26.1. It reduces the RAM of a Minecraft server. It is server-side, has no config, and changes nothing in the game.

Mapple was designed to take advantage of large-scale optimisations that scale, notably for the proper working of **[Leafs](https://modrinth.com/mod/leafs)**, the mod that runs the world in multithread.

![Delimeter](https://cdn.modrinth.com/data/cached_images/c57c204c55df0ce5357df6501f616f2c7b7c6df1.png)

# What it optimises
The big part of a server's memory is air blocks that over-allocate RAM, and everything the game keeps around chunks just in case. Mapple removes what is useless.

- **Air.** Three chunk sections out of four are pure air. Mapple merges them into a single instance.
- **Chunk copies.** For each loaded chunk, the game keeps an empty copy it never reads. Mapple removes it.
- **Protections.** Each piece of chunk carries a heavy protection against simultaneous access. Mapple replaces it with a light version that does the same job.
- **Block palettes.** When a chunk contains few kinds of blocks, Mapple packs them tighter in memory. On disk, nothing changes.
- **Lists.** The game keeps lists of POI, structures and chunk types. They grow with every visited chunk and never shrink. Mapple cleans them when a chunk unloads.
- **Shared pathfinding buffer.** Each mob owns its own pathfinding buffers that only grow, even when it does not move. Mobs share this memory.
- **Cold storage for idle chunks.** A chunk waiting in the generation pipeline is compressed with LZ4 until someone needs it.
- **Disk.** A chunk that was only loaded is not written back to disk if it is not modified. Mapple only saves what changed.

![Delimeter](https://cdn.modrinth.com/data/cached_images/c57c204c55df0ce5357df6501f616f2c7b7c6df1.png)

# The gains
Tested with Leafs, each isolated player generating new chunks costs 33 MB instead of 52 MB.

| Measure | Vanilla | Mapple |
|---|---|---|
| Memory | 2 220 MB | 1 560 MB |
| Objects in memory | 31 million | 16 million |
| Chunks written at autosave | 23 210 | 3 377 |
| Pathfinding nodes | 326 705 | 67 854 |

# FAQ
**Compatible with Lithium and FerriteCore?**
Yes, and both are recommended.

**Compatible with C2ME, Moonrise or VMP?**
Not tested. They rewrite the same chunk structures.

**Does it change the game?**
No. It is just optimisations with no consequence.
