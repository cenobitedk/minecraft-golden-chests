# Golden Chests

A Minecraft **26.2** mod for Fabric and NeoForge.

Craft a golden chest, enchant it with Unbreaking, and link a pair so they share one inventory — even after you mine them and place them somewhere else.

Development for this Minecraft version lives on the `26.2` branch. Fabric and NeoForge are built from the same branch.

## Features

- Craft with 8 gold ingots surrounding a chest
- Enchant with Unbreaking (enchanting table or anvil)
- Two unenchanted chests placed together merge into a vanilla-style 54-slot double chest
- Two enchanted chests placed together look like a double chest and share a 27-slot linked inventory
- Mixed enchanted/unenchanted neighbors do not merge
- Linked inventory is kept in world save data, so the pair reconnects when either chest is placed back down
- Mining both chests of a linked pair breaks the link: the shared inventory drops at the last chest mined, and both items become plain enchanted chests again
- Only chests in loaded chunks count as a live link. If one chest of a pair is in an unloaded chunk and you mine the other, the link is treated as broken: the shared inventory drops where you mined, and the far chest opens to an empty inventory when its chunk loads again
- Grindstone removes the enchantment and breaks the link
- Items that still have an active partner show a **Linked** tooltip

## Building

Requires **Java 25**.

```bash
./gradlew :fabric:build :neoforge:build
```

Release JARs are copied to `dist/` at the repository root, named `golden-chests-<minecraft>-<loader>-<mod version>.jar`, e.g. `golden-chests-26.2-fabric-1.0.0.jar`. The versions come from `gradle.properties`. `./gradlew clean` empties `dist/`. Architectury is used as a build tool only; players do not need a second mod installed.

## Checks

Pull requests must pass CI before merging. Run the same checks locally with:

```bash
./gradlew spotlessCheck test build
```

If `spotlessCheck` fails, `./gradlew spotlessApply` reformats the code.

## License

MIT
