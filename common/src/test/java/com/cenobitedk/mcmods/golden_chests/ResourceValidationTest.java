package com.cenobitedk.mcmods.golden_chests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.Strictness;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class ResourceValidationTest {

    private static final String NS = GoldenChestsMod.MOD_ID;
    private static final Path ROOT = Path.of(System.getProperty("golden_chests.root_dir"));
    private static final Path RESOURCES = ROOT.resolve("common/src/main/resources");
    private static final Path ASSETS = RESOURCES.resolve("assets").resolve(NS);
    private static final Path DATA = RESOURCES.resolve("data");
    private static final Pattern TRANSLATABLE = Pattern.compile("Component\\.translatable\\(\\s*\"([^\"]+)\"");
    private static final Pattern NAMESPACED_ID = Pattern.compile("^" + NS + ":([a-z0-9_./-]+)$");

    @Test
    void allJsonFilesAreStrictlyValid() throws IOException {
        List<Path> files = filesWithExtension(RESOURCES, ".json");
        assertFalse(files.isEmpty(), "No JSON resources found in " + RESOURCES);
        for (Path file : files) {
            readJson(file);
        }
    }

    @Test
    void everyTranslatableKeyInSourceHasAnEnglishTranslation() throws IOException {
        JsonObject lang = readJson(ASSETS.resolve("lang/en_us.json")).getAsJsonObject();
        Set<String> missing = new TreeSet<>();
        for (String module : List.of("common", "fabric", "neoforge")) {
            for (Path file : filesWithExtension(ROOT.resolve(module).resolve("src/main/java"), ".java")) {
                Matcher matcher = TRANSLATABLE.matcher(Files.readString(file));
                while (matcher.find()) {
                    if (!lang.has(matcher.group(1))) {
                        missing.add(matcher.group(1) + " (" + ROOT.relativize(file) + ")");
                    }
                }
            }
        }
        assertTrue(missing.isEmpty(), "Missing en_us translations: " + missing);
    }

    @Test
    void everyBlockAndItemHasAnEnglishName() throws IOException {
        JsonObject lang = readJson(ASSETS.resolve("lang/en_us.json")).getAsJsonObject();
        for (String block : idsIn(ASSETS.resolve("blockstates"))) {
            assertTrue(lang.has("block." + NS + "." + block), "Missing en_us name for block " + block);
        }
        for (String item : idsIn(ASSETS.resolve("items"))) {
            assertTrue(lang.has("item." + NS + "." + item), "Missing en_us name for item " + item);
        }
    }

    @Test
    void blockstateModelsExist() throws IOException {
        for (Path file : filesWithExtension(ASSETS.resolve("blockstates"), ".json")) {
            forEachString(readJson(file), (key, value) -> {
                if (key.equals("model")) {
                    assertAssetExists(file, value, "models", ".json");
                }
            });
        }
    }

    @Test
    void itemModelsExist() throws IOException {
        for (Path file : filesWithExtension(ASSETS.resolve("items"), ".json")) {
            forEachString(readJson(file), (key, value) -> {
                if (key.equals("model") || key.equals("base")) {
                    assertAssetExists(file, value, "models", ".json");
                }
            });
        }
    }

    @Test
    void modelParentsAndTexturesExist() throws IOException {
        for (Path file : filesWithExtension(ASSETS.resolve("models"), ".json")) {
            JsonObject model = readJson(file).getAsJsonObject();
            if (model.has("parent")) {
                assertAssetExists(file, model.get("parent").getAsString(), "models", ".json");
            }
            if (model.has("textures")) {
                for (Map.Entry<String, JsonElement> texture :
                        model.getAsJsonObject("textures").entrySet()) {
                    String value = texture.getValue().getAsString();
                    if (!value.startsWith("#")) {
                        assertAssetExists(file, value, "textures", ".png");
                    }
                }
            }
        }
    }

    @Test
    void chestEntityTexturesExist() {
        for (String texture : List.of("golden", "golden_left", "golden_right")) {
            Path png = ASSETS.resolve("textures/entity/chest/" + texture + ".png");
            assertTrue(Files.isRegularFile(png), "Missing chest texture " + RESOURCES.relativize(png));
        }
    }

    @Test
    void dataFilesOnlyReferenceItemsThatExist() throws IOException {
        Set<String> items = idsIn(ASSETS.resolve("items"));
        for (Path file : filesWithExtension(DATA, ".json")) {
            forEachString(readJson(file), (key, value) -> {
                Matcher matcher = NAMESPACED_ID.matcher(value);
                if (matcher.matches()) {
                    assertTrue(
                            items.contains(matcher.group(1)),
                            RESOURCES.relativize(file) + " references unknown item " + value);
                }
            });
        }
    }

    @Test
    void goldenChestRecipeProducesGoldenChest() throws IOException {
        JsonObject recipe =
                readJson(DATA.resolve(NS + "/recipe/golden_chest.json")).getAsJsonObject();
        assertEquals(
                NS + ":golden_chest", recipe.getAsJsonObject("result").get("id").getAsString());
    }

    private static void assertAssetExists(Path referencedFrom, String id, String folder, String extension) {
        String namespace = id.contains(":") ? id.substring(0, id.indexOf(':')) : "minecraft";
        if (!namespace.equals(NS)) {
            return;
        }
        String path = id.substring(id.indexOf(':') + 1);
        Path asset = ASSETS.resolve(folder).resolve(path + extension);
        assertTrue(
                Files.isRegularFile(asset),
                RESOURCES.relativize(referencedFrom) + " references missing " + RESOURCES.relativize(asset));
    }

    private static Set<String> idsIn(Path directory) throws IOException {
        Set<String> ids = new TreeSet<>();
        for (Path file : filesWithExtension(directory, ".json")) {
            String name = file.getFileName().toString();
            ids.add(name.substring(0, name.length() - ".json".length()));
        }
        return ids;
    }

    private static List<Path> filesWithExtension(Path directory, String extension) throws IOException {
        if (!Files.isDirectory(directory)) {
            return List.of();
        }
        try (Stream<Path> paths = Files.walk(directory)) {
            return new ArrayList<>(paths.filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(extension))
                    .sorted()
                    .toList());
        }
    }

    private static JsonElement readJson(Path file) {
        try (Reader reader = Files.newBufferedReader(file);
                JsonReader json = new JsonReader(reader)) {
            json.setStrictness(Strictness.STRICT);
            JsonElement element = new Gson().getAdapter(JsonElement.class).read(json);
            assertEquals(JsonToken.END_DOCUMENT, json.peek(), "Trailing content in " + file);
            return element;
        } catch (IOException | RuntimeException e) {
            return fail("Invalid JSON in " + RESOURCES.relativize(file) + ": " + e.getMessage(), e);
        }
    }

    private static void forEachString(JsonElement element, BiConsumer<String, String> consumer) {
        if (element.isJsonObject()) {
            for (Map.Entry<String, JsonElement> entry :
                    element.getAsJsonObject().entrySet()) {
                if (entry.getValue().isJsonPrimitive()
                        && entry.getValue().getAsJsonPrimitive().isString()) {
                    consumer.accept(entry.getKey(), entry.getValue().getAsString());
                } else {
                    forEachString(entry.getValue(), consumer);
                }
            }
        } else if (element.isJsonArray()) {
            for (JsonElement child : element.getAsJsonArray()) {
                if (child.isJsonPrimitive() && child.getAsJsonPrimitive().isString()) {
                    consumer.accept("", child.getAsString());
                } else {
                    forEachString(child, consumer);
                }
            }
        }
    }
}
