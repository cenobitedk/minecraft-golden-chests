package com.cenobitedk.mcmods.golden_chests.storage;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.*;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

public class SharedChestData extends SavedData {

    public static final int CHEST_SIZE = 27;

    private record Entry(UUID id, List<ItemStack> items) {}

    private static final Codec<Entry> ENTRY_CODEC = RecordCodecBuilder.create(inst -> inst.group(
                    UUIDUtil.STRING_CODEC.fieldOf("id").forGetter(Entry::id),
                    ItemStack.OPTIONAL_CODEC.listOf().fieldOf("items").forGetter(Entry::items))
            .apply(inst, Entry::new));

    public static final Codec<SharedChestData> CODEC = ENTRY_CODEC
            .listOf()
            .xmap(
                    entries -> {
                        SharedChestData data = new SharedChestData();
                        for (Entry entry : entries) {
                            SimpleContainer c = data.makeContainer();
                            List<ItemStack> items = entry.items();
                            for (int i = 0; i < Math.min(items.size(), CHEST_SIZE); i++) {
                                c.setItem(i, items.get(i));
                            }
                            data.inventories.put(entry.id(), c);
                        }
                        return data;
                    },
                    data -> {
                        List<Entry> list = new ArrayList<>();
                        data.inventories.forEach((uuid, c) -> {
                            List<ItemStack> items = new ArrayList<>(CHEST_SIZE);
                            for (int i = 0; i < c.getContainerSize(); i++)
                                items.add(c.getItem(i).copy());
                            list.add(new Entry(uuid, items));
                        });
                        return list;
                    });

    public static final SavedDataType<SharedChestData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath("golden_chests", "shared_chest_data"), SharedChestData::new, CODEC, null);

    private final Map<UUID, SimpleContainer> inventories = new HashMap<>();

    /** Tracks how many loaded GoldenChestBlockEntities are currently linked to each UUID. */
    private final Map<UUID, Integer> activeRefs = new HashMap<>();

    public static SharedChestData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    private SimpleContainer makeContainer() {
        return new SimpleContainer(CHEST_SIZE) {
            @Override
            public void setChanged() {
                super.setChanged();
                SharedChestData.this.setDirty();
            }
        };
    }

    public SimpleContainer getOrCreate(UUID linkId) {
        return inventories.computeIfAbsent(linkId, k -> makeContainer());
    }

    /**
     * Returns true while the link's shared inventory exists. The inventory is only removed when
     * the last chest of a pair is mined or grindstoned, so a chest (placed or still an item)
     * with an existing link belongs to a pair and must not be linked to anything else.
     */
    public boolean exists(UUID linkId) {
        return inventories.containsKey(linkId);
    }

    public void remove(UUID linkId) {
        inventories.remove(linkId);
        setDirty();
    }

    // --- Reference counting for loaded block entities ---

    /** Increment the loaded reference count for a linkId. */
    public void addRef(UUID linkId) {
        activeRefs.merge(linkId, 1, Integer::sum);
    }

    /** Decrement the loaded reference count for a linkId. */
    public void removeRef(UUID linkId) {
        activeRefs.merge(linkId, -1, Integer::sum);
        int count = activeRefs.getOrDefault(linkId, 0);
        if (count <= 0) activeRefs.remove(linkId);
    }

    /** Returns true if at least one loaded block entity still holds a ref to this linkId. */
    public boolean hasActiveRefs(UUID linkId) {
        return activeRefs.getOrDefault(linkId, 0) > 0;
    }

    /**
     * Returns true if MORE THAN ONE loaded block entity holds a ref to this linkId.
     * Useful when the caller itself still holds a ref and wants to know if others exist.
     */
    public boolean hasOtherRefs(UUID linkId) {
        return activeRefs.getOrDefault(linkId, 0) > 1;
    }
}
