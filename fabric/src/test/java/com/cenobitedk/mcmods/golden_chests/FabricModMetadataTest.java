package com.cenobitedk.mcmods.golden_chests;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class FabricModMetadataTest {

    private static JsonObject metadata;

    @BeforeAll
    static void loadMetadata() throws IOException {
        Path file = Path.of(property("resources_dir"), "fabric.mod.json");
        assertTrue(Files.isRegularFile(file), file + " does not exist");
        try (Reader reader = Files.newBufferedReader(file)) {
            metadata = JsonParser.parseReader(reader).getAsJsonObject();
        }
    }

    @Test
    void modIdMatchesCommonModId() {
        assertEquals(GoldenChestsMod.MOD_ID, metadata.get("id").getAsString());
    }

    @Test
    void versionIsExpandedFromGradle() {
        assertEquals(property("mod_version"), metadata.get("version").getAsString());
    }

    @Test
    void dependenciesMatchGradleProperties() {
        JsonObject depends = metadata.getAsJsonObject("depends");
        assertEquals(property("minecraft_version"), depends.get("minecraft").getAsString());
        assertEquals(">=" + property("java_version"), depends.get("java").getAsString());
        assertEquals(
                ">=" + property("fabric_loader_version"),
                depends.get("fabricloader").getAsString());
    }

    @Test
    void entrypointClassesExist() {
        JsonObject entrypoints = metadata.getAsJsonObject("entrypoints");
        assertFalse(entrypoints.isEmpty(), "No entrypoints declared");
        for (Map.Entry<String, JsonElement> side : entrypoints.entrySet()) {
            for (JsonElement entrypoint : side.getValue().getAsJsonArray()) {
                String className = entrypoint.getAsString();
                assertDoesNotThrow(
                        () -> Class.forName(className, false, getClass().getClassLoader()),
                        side.getKey() + " entrypoint " + className + " does not exist");
            }
        }
    }

    private static String property(String name) {
        return System.getProperty("golden_chests." + name);
    }
}
