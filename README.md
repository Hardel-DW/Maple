# Maple - Optimisations
Maple works on any Fabric/NeoForge server, server-side, from 26.1. It reduces the RAM of a Minecraft server. It is server-side, has no config, and changes nothing in the game.

Maple was designed to take advantage of large-scale optimisations that scale, notably for the proper working of **[Leafs](https://modrinth.com/mod/leafs)**, the mod that runs the world in multithread.

![Delimeter](https://cdn.modrinth.com/data/cached_images/c57c204c55df0ce5357df6501f616f2c7b7c6df1.png)

# What it optimises
The big part of a server's memory is air blocks that over-allocate RAM, and everything the game keeps around chunks just in case. Maple removes what is useless, without costing any CPU.

- **Air.** Three chunk sections out of four are pure air. Every chunk keeps its own sections, but all the empty ones share the same data.
- **Chunk copies.** For each chunk used by the generation of its neighbours, the game builds an empty copy it never reads. Maple removes it.
- **Protections.** Each piece of chunk carries a heavy protection against simultaneous access, a debug check. Maple removes it, as Lithium does.
- **Lists.** The game keeps lists of POI and chunk types. They grow with every visited chunk and never shrink. Maple cleans them when a chunk unloads.
- **Shared pathfinding buffer.** Each mob owns its own pathfinding buffers that only grow, even when it does not move. Mobs share this memory.

![Delimeter](https://cdn.modrinth.com/data/cached_images/c57c204c55df0ce5357df6501f616f2c7b7c6df1.png)

# The gains
Measured on a server without Leafs, 5 players flying over new land at 36 blocks per second, view distance 10. The generated world is identical to vanilla, block for block.

| Measure | Vanilla | Maple |
|---|---|---|
| Live memory at the end of the run | 0.66 to 0.69 GB | 0.52 to 0.53 GB |
| CPU per generated chunk | 27.9 ms | 26.7 ms |

# FAQ
**Compatible with Lithium and FerriteCore?**
Yes, and both are recommended. Maple turns off none of Lithium's options.

**Compatible with C2ME, Moonrise or VMP?**
Not tested. They rewrite the same chunk structures.

**Does it change the game?**
No. It is just optimisations with no consequence.
