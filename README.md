# Golden Chests

A Minecraft mod for Fabric and NeoForge.

Craft a **Golden Chest**, enchant it with Unbreaking, and link a pair so they share one inventory — even after you mine them and place them somewhere else.

<p align="center">
  <img src="/docs/assets/golden_chest_isometric_transparent.png" alt="Golden Chest icon" width="300px">
</p>

## Features

- Craft with 8 gold ingots surrounding a chest
- Enchant with Unbreaking (enchanting table or anvil)
- Two _unenchanted_ chests placed together merge into a vanilla-style 54-slot double chest
- Two _enchanted_ chests placed together look like a double chest and share a 27-slot linked inventory
- Mixed enchanted/unenchanted neighbors do not merge
- A link is always a pair: a chest that is still linked (its partner is placed elsewhere or carried as an item) only reconnects with that partner, and never merges with another chest or pair
- Linked inventory is kept in world save data, so the pair reconnects when either chest is placed back down
- Chests that still have an active partner show a **Linked** tooltip
- Grindstone removes the enchantment and breaks the link
- Mining both chests of a linked pair breaks the link: the shared inventory drops at the last chest mined, and both items become plain enchanted chests again

### Notes

- Only chests in loaded chunks count as a live link. If one chest of a pair is in an unloaded chunk and you mine the other, the link is treated as broken: the shared inventory drops where you mined, and the far chest opens to an empty inventory when its chunk loads again

## Requirements

- **Fabric:** [Fabric Loader](https://fabricmc.net/use/installer/) 0.19.3 or newer, and [Fabric API](https://modrinth.com/mod/fabric-api) 0.152.1 or newer (the build for Minecraft 26.2)
- **NeoForge:** [NeoForge](https://neoforged.net/) 26.2.0.7-beta or newer for Minecraft 26.2

## Client / Server

Golden Chests adds a new block and item, so it must be installed on **both** the client and the server. Use the JAR that matches your loader (Fabric or NeoForge) on each side.

- **Singleplayer / local worlds:** install the mod (plus Fabric API on Fabric) in your client's `mods` folder. The integrated server runs the mod for you, including LAN worlds you open to others. Anyone joining a LAN world also needs the mod installed.
- **Dedicated server:** put the mod (plus Fabric API on Fabric) in the server's `mods` folder. Every player who joins must also have the same mod version installed on their client. Players without it cannot connect.

Linked chest inventories are stored in the world save, so they are kept on the server (or in your singleplayer world) and are not tied to any one client.

## Building

Requires **Java 25**.

```bash
./gradlew :fabric:build :neoforge:build
```

Release JARs are copied to `dist/` at the repository root, named `golden-chests-<minecraft>-<loader>-<mod version>.jar`, e.g. `golden-chests-26.3-fabric-1.0.0.jar`. The versions come from `gradle.properties`. On non-version-line branches (anything other than `26.2`, `26.3`, …), the short commit SHA is appended to the file name (e.g. `golden-chests-26.3-fabric-1.0.0-a1b2c3d.jar`); the version in the mod metadata is `<minecraft>-<loader>-<mod version>` (e.g. `26.3-fabric-1.0.0`), without the SHA. Each build replaces that loader's previous jar in `dist/`, so it holds only the latest Fabric and NeoForge jars. `./gradlew clean` empties `dist/`. Architectury is used as a build tool only; players do not need a second mod installed.

## Checks

Pull requests must pass CI before merging. Run the same checks locally with:

```bash
./gradlew spotlessCheck test build
```

If `spotlessCheck` fails, `./gradlew spotlessApply` reformats the code.

## License

MIT
