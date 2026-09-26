package com.cenobitedk.mcmods.golden_chests;

import com.cenobitedk.mcmods.golden_chests.blockentity.GoldenChestBlockEntity;
import com.cenobitedk.mcmods.golden_chests.client.GoldenChestRenderer;
import com.cenobitedk.mcmods.golden_chests.client.GoldenChestSpecialRenderer;
import com.cenobitedk.mcmods.golden_chests.registry.ModBlockEntityTypes;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;
import net.minecraft.client.model.object.chest.ChestModel;
import net.minecraft.client.renderer.special.SpecialModelRenderers;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.entity.BlockEntityType;

public class GoldenChestsFabricClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        // Register custom chest special renderer that properly applies enchantment glint
        SpecialModelRenderers.ID_MAPPER.put(
                Identifier.fromNamespaceAndPath("golden_chests", "chest"),
                GoldenChestSpecialRenderer.Unbaked.MAP_CODEC);

        ModelLayerRegistry.registerModelLayer(GoldenChestRenderer.LAYER_SINGLE, ChestModel::createSingleBodyLayer);
        ModelLayerRegistry.registerModelLayer(
                GoldenChestRenderer.LAYER_DOUBLE_LEFT, ChestModel::createDoubleBodyLeftLayer);
        ModelLayerRegistry.registerModelLayer(
                GoldenChestRenderer.LAYER_DOUBLE_RIGHT, ChestModel::createDoubleBodyRightLayer);

        BlockEntityRendererRegistry.register(
                (BlockEntityType<GoldenChestBlockEntity>) ModBlockEntityTypes.GOLDEN_CHEST.get(),
                GoldenChestRenderer::new);
    }
}
