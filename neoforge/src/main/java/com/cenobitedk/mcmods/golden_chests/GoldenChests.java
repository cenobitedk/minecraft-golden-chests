package com.cenobitedk.mcmods.golden_chests;

import com.cenobitedk.mcmods.golden_chests.blockentity.GoldenChestBlockEntity;
import com.cenobitedk.mcmods.golden_chests.blocks.GoldenChestBlock;
import com.cenobitedk.mcmods.golden_chests.client.GoldenChestRenderer;
import com.cenobitedk.mcmods.golden_chests.client.GoldenChestSpecialRenderer;
import com.cenobitedk.mcmods.golden_chests.items.GoldenChestItem;
import com.cenobitedk.mcmods.golden_chests.registry.ModBlockEntityTypes;
import com.cenobitedk.mcmods.golden_chests.registry.ModBlocks;
import com.cenobitedk.mcmods.golden_chests.registry.ModItems;
import java.util.Set;
import net.minecraft.client.model.object.chest.ChestModel;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.enchantment.Enchantable;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterSpecialModelRendererEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(GoldenChestsMod.MOD_ID)
public class GoldenChests {

    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(GoldenChestsMod.MOD_ID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(GoldenChestsMod.MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, GoldenChestsMod.MOD_ID);

    private static final DeferredBlock<GoldenChestBlock> GOLDEN_CHEST_BLOCK =
            BLOCKS.registerBlock("golden_chest", GoldenChestBlock::new, p -> p.strength(2.5f));

    private static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GoldenChestBlockEntity>> GOLDEN_CHEST_BE =
            BLOCK_ENTITY_TYPES.register(
                    "golden_chest",
                    () -> new BlockEntityType<>(GoldenChestBlockEntity::new, Set.of(GOLDEN_CHEST_BLOCK.get())));

    private static final DeferredItem<GoldenChestItem> GOLDEN_CHEST_ITEM = ITEMS.registerItem(
            "golden_chest",
            p -> new GoldenChestItem(
                    GOLDEN_CHEST_BLOCK.get(),
                    p.component(DataComponents.ENCHANTABLE, new Enchantable(22))
                            .component(
                                    DataComponents.ENCHANTMENTS,
                                    net.minecraft.world.item.enchantment.ItemEnchantments.EMPTY)));

    public GoldenChests(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        BLOCK_ENTITY_TYPES.register(modEventBus);
        modEventBus.addListener(this::buildCreativeTab);

        ModBlocks.GOLDEN_CHEST = GOLDEN_CHEST_BLOCK;
        ModBlockEntityTypes.GOLDEN_CHEST = GOLDEN_CHEST_BE;
        ModItems.GOLDEN_CHEST = () -> GOLDEN_CHEST_ITEM.get();
    }

    private void buildCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(GOLDEN_CHEST_ITEM.get());
        }
    }

    @EventBusSubscriber(modid = GoldenChestsMod.MOD_ID, value = Dist.CLIENT)
    public static class ClientEvents {

        @SubscribeEvent
        public static void onRegisterLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
            event.registerLayerDefinition(GoldenChestRenderer.LAYER_SINGLE, ChestModel::createSingleBodyLayer);
            event.registerLayerDefinition(GoldenChestRenderer.LAYER_DOUBLE_LEFT, ChestModel::createDoubleBodyLeftLayer);
            event.registerLayerDefinition(
                    GoldenChestRenderer.LAYER_DOUBLE_RIGHT, ChestModel::createDoubleBodyRightLayer);
        }

        @SubscribeEvent
        public static void onRegisterSpecialModelRenderers(RegisterSpecialModelRendererEvent event) {
            event.register(
                    Identifier.fromNamespaceAndPath(GoldenChestsMod.MOD_ID, "chest"),
                    GoldenChestSpecialRenderer.Unbaked.MAP_CODEC);
        }

        @SubscribeEvent
        public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
            event.registerBlockEntityRenderer(ModBlockEntityTypes.GOLDEN_CHEST.get(), GoldenChestRenderer::new);
        }
    }
}
