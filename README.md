# Golden Chests

A Minecraft **26.3** mod for Fabric and NeoForge.

Craft a golden chest, enchant it with Unbreaking, and link a pair so they share one inventory — even after you mine them and place them somewhere else.

Development for this Minecraft version lives on the `26.3` branch. Fabric and NeoForge are built from the same branch.

## Features

- Craft with 8 gold ingots surrounding a chest
- Enchant with Unbreaking (enchanting table or anvil)
- Two unenchanted chests placed together merge into a vanilla-style 54-slot double chest
- Two enchanted chests placed together look like a double chest and share a 27-slot linked inventory
- Mixed enchanted/unenchanted neighbors do not merge
- Linked inventory is kept in world save data, so the pair reconnects when either chest is placed back down
- Grindstone removes the enchantment and breaks the link
- Items that still have an active partner show a **Linked** tooltip

## Building

Requires **Java 25**.

```bash
./gradlew :fabric:build :neoforge:build
```

JARs are written to `fabric/build/libs/` and `neoforge/build/libs/`. Architectury is used as a build tool only; players do not need a second mod installed.

## License

MIT
