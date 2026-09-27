package com.cenobitedk.mcmods.golden_chests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.toml.TomlParser;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import net.neoforged.fml.common.Mod;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class NeoForgeModMetadataTest {

    private static final Path META_INF = Path.of(System.getProperty("golden_chests.resources_dir"), "META-INF");

    private static Config metadata;

    @BeforeAll
    static void loadMetadata() throws IOException {
        Path file = META_INF.resolve("neoforge.mods.toml");
        assertTrue(Files.isRegularFile(file), file + " does not exist");
        try (Reader reader = Files.newBufferedReader(file)) {
            metadata = new TomlParser().parse(reader);
        }
    }

    @Test
    void noLegacyForgeMetadata() {
        assertFalse(
                Files.exists(META_INF.resolve("mods.toml")),
                "META-INF/mods.toml makes NeoForge reject the jar as an old Forge mod");
    }

    @Test
    void usesJavaModLoader() {
        assertEquals("javafml", metadata.get("modLoader"));
    }

    @Test
    void modEntryMatchesGradleProperties() {
        Config mod = onlyMod();
        assertEquals(GoldenChestsMod.MOD_ID, mod.get("modId"));
        assertEquals(
                System.getProperty("golden_chests.minecraft_version") + "-neoforge-"
                        + System.getProperty("golden_chests.mod_version"),
                mod.get("version"));
    }

    @Test
    void minecraftDependencyIncludesTargetVersion() {
        List<Config> dependencies = metadata.get(List.of("dependencies", GoldenChestsMod.MOD_ID));
        assertNotNull(dependencies, "No dependencies declared for " + GoldenChestsMod.MOD_ID);
        Config minecraft = dependencies.stream()
                .filter(d -> "minecraft".equals(d.get("modId")))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No minecraft dependency declared"));
        String range = minecraft.get("versionRange");
        String target = System.getProperty("golden_chests.minecraft_version");
        assertTrue(range.startsWith("[" + target), "versionRange " + range + " does not start at " + target);
    }

    @Test
    void modClassIsAnnotatedWithModId() throws ClassNotFoundException {
        Class<?> modClass = Class.forName(
                "com.cenobitedk.mcmods.golden_chests.GoldenChests",
                false,
                NeoForgeModMetadataTest.class.getClassLoader());
        Mod annotation = modClass.getAnnotation(Mod.class);
        assertNotNull(annotation, modClass.getName() + " is missing @Mod");
        assertEquals(GoldenChestsMod.MOD_ID, annotation.value());
    }

    private static Config onlyMod() {
        List<Config> mods = metadata.get("mods");
        assertNotNull(mods, "No [[mods]] entries");
        assertEquals(1, mods.size(), "Expected exactly one [[mods]] entry");
        return mods.getFirst();
    }
}
