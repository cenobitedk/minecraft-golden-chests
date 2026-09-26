package com.cenobitedk.mcmods.golden_chests;

import com.cenobitedk.mcmods.golden_chests.blockentity.GoldenChestBlockEntity;
import com.cenobitedk.mcmods.golden_chests.blocks.GoldenChestBlock;
import com.cenobitedk.mcmods.golden_chests.items.GoldenChestItem;
import com.cenobitedk.mcmods.golden_chests.registry.ModBlockEntityTypes;
import com.cenobitedk.mcmods.golden_chests.registry.ModBlocks;
import com.cenobitedk.mcmods.golden_chests.registry.ModItems;
import java.util.Set;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.enchantment.Enchantable;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;

public class GoldenChests implements ModInitializer {

    @Override
    public void onInitialize() {
        registerBlock();
        registerBlockEntityType();
        registerItem();
        addToCreativeTab();
    }

    private static void registerBlock() {
        Identifier id = Identifier.fromNamespaceAndPath(GoldenChestsMod.MOD_ID, "golden_chest");
        ResourceKey<net.minecraft.world.level.block.Block> key = ResourceKey.create(Registries.BLOCK, id);
        GoldenChestBlock block =
                new GoldenChestBlock(BlockBehaviour.Properties.of().setId(key).strength(2.5f));
        Registry.register(BuiltInRegistries.BLOCK, id, block);
        ModBlocks.GOLDEN_CHEST = () -> block;
    }

    private static void registerBlockEntityType() {
        BlockEntityType<GoldenChestBlockEntity> type =
                new BlockEntityType<>(GoldenChestBlockEntity::new, Set.of(ModBlocks.GOLDEN_CHEST.get()));
        Registry.register(
                BuiltInRegistries.BLOCK_ENTITY_TYPE,
                Identifier.fromNamespaceAndPath(GoldenChestsMod.MOD_ID, "golden_chest"),
                type);
        ModBlockEntityTypes.GOLDEN_CHEST = () -> type;
    }

    private static void registerItem() {
        Identifier id = Identifier.fromNamespaceAndPath(GoldenChestsMod.MOD_ID, "golden_chest");
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, id);
        Item item = new GoldenChestItem(
                ModBlocks.GOLDEN_CHEST.get(),
                new Item.Properties()
                        .setId(key)
                        .component(DataComponents.ENCHANTABLE, new Enchantable(22))
                        // Default ENCHANTMENTS to empty so the grindstone's empty-enchantment
                        // patch is a no-op and grindstoned chests stack with fresh ones.
                        .component(
                                DataComponents.ENCHANTMENTS,
                                net.minecraft.world.item.enchantment.ItemEnchantments.EMPTY));
        Registry.register(BuiltInRegistries.ITEM, id, item);
        ModItems.GOLDEN_CHEST = () -> item;
    }

    private static void addToCreativeTab() {
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS)
                .register(output -> output.accept(ModItems.GOLDEN_CHEST.get()));
    }
}
