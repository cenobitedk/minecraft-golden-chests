package com.cenobitedk.mcmods.golden_chests.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;
import java.util.function.Consumer;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.object.chest.ChestModel;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.special.NoDataSpecialModelRenderer;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.resources.model.sprite.SpriteGetter;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.resources.Identifier;

/**
 * Custom special renderer for the golden chest item.
 * Identical to ChestSpecialRenderer except it correctly applies the enchantment
 * glint when foil=true (ChestSpecialRenderer ignores the foil boolean).
 */
public class GoldenChestSpecialRenderer implements NoDataSpecialModelRenderer {

    private static final SpriteId SPRITE =
            Sheets.CHEST_MAPPER.apply(Identifier.fromNamespaceAndPath("golden_chests", "golden"));

    private final ChestModel model;
    private final SpriteGetter sprites;

    public GoldenChestSpecialRenderer(ChestModel model, SpriteGetter sprites) {
        this.model = model;
        this.sprites = sprites;
    }

    @Override
    public void submit(
            PoseStack poseStack, SubmitNodeCollector collector, int light, int overlay, boolean foil, int seed) {
        model.setupAnim(0f);
        collector.submitModel(model, 0f, poseStack, light, overlay, -1, SPRITE, sprites, seed, null);
        if (foil) {
            // ChestSpecialRenderer ignores foil — we fix that here with a second glint pass
            collector.submitModel(model, 0f, poseStack, RenderTypes.glint(), light, overlay, -1, null);
        }
    }

    @Override
    public void getExtents(Consumer<org.joml.Vector3fc> consumer) {
        // Delegate to the model's root part for proper bounding box
        model.setupAnim(0f);
        model.root().getExtentsForGui(new PoseStack(), consumer);
    }

    // --- Unbaked ---

    public static final class Unbaked implements NoDataSpecialModelRenderer.Unbaked {

        public static final MapCodec<Unbaked> MAP_CODEC = MapCodec.unit(new Unbaked());

        @Override
        public MapCodec<? extends NoDataSpecialModelRenderer.Unbaked> type() {
            return MAP_CODEC;
        }

        @Override
        public SpecialModelRenderer<Void> bake(SpecialModelRenderer.BakingContext ctx) {
            ChestModel model = new ChestModel(ctx.entityModelSet().bakeLayer(ModelLayers.CHEST));
            return new GoldenChestSpecialRenderer(model, ctx.sprites());
        }
    }
}
