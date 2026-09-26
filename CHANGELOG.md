# Changelog

All notable changes to Golden Chests are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## [Unreleased] - Minecraft 26.3

### Changed
- Updated to Minecraft 26.3 (Fabric Loader 0.19.5, Fabric API 0.161.0+26.3, NeoForge 26.3.0.22-beta).
- Enchantment glint on the chest and its item is rendered with the 26.3 glint render type.
- Block breaking animation on the chest uses the 26.3 crumbling overlay pipeline.
- Release jars are named `golden-chests-<minecraft>-<loader>-<loader version>-<mod version>.jar` and collected in `dist/`.

### Fixed
- NeoForge: the mod jar is now recognized and loaded. It was previously skipped as a Forge/legacy NeoForge mod, and its shared classes could not see Minecraft classes.

## [1.0.0] - Minecraft 26.2

### Added
- Initial release for Fabric and NeoForge.
- Golden chest crafted from 8 gold ingots surrounding a chest.
- Unbreaking enchantment via enchanting table or anvil.
- Unenchanted neighbors merge into a 54-slot double chest; enchanted neighbors share a 27-slot linked inventory.
- Linked inventory is stored in world save data and reconnects when a chest is placed back down.
- Grindstone removes the enchantment and breaks the link.
- **Linked** tooltip on items that still have an active partner.
