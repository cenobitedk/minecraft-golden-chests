package com.cenobitedk.mcmods.golden_chests.client;

import com.cenobitedk.mcmods.golden_chests.blockentity.GoldenChestBlockEntity;
import com.cenobitedk.mcmods.golden_chests.blocks.GoldenChestBlock;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.object.chest.ChestModel;
import net.minecraft.client.renderer.MultiblockChestResources;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.ChestRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.sprite.SpriteGetter;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.block.state.properties.EnumProperty;

public class GoldenChestRenderer implements BlockEntityRenderer<GoldenChestBlockEntity, GoldenChestRenderState> {

    // Model layers — reuse vanilla chest geometry (same UV layout)
    public static final ModelLayerLocation LAYER_SINGLE =
            new ModelLayerLocation(Identifier.fromNamespaceAndPath("golden_chests", "golden_chest"), "main");
    public static final ModelLayerLocation LAYER_DOUBLE_LEFT =
            new ModelLayerLocation(Identifier.fromNamespaceAndPath("golden_chests", "golden_chest_left"), "main");
    public static final ModelLayerLocation LAYER_DOUBLE_RIGHT =
            new ModelLayerLocation(Identifier.fromNamespaceAndPath("golden_chests", "golden_chest_right"), "main");

    // Sprites — CHEST_MAPPER adds the "entity/chest/" prefix automatically
    private static final SpriteId SPRITE_SINGLE =
            Sheets.CHEST_MAPPER.apply(Identifier.fromNamespaceAndPath("golden_chests", "golden"));
    private static final SpriteId SPRITE_LEFT =
            Sheets.CHEST_MAPPER.apply(Identifier.fromNamespaceAndPath("golden_chests", "golden_left"));
    private static final SpriteId SPRITE_RIGHT =
            Sheets.CHEST_MAPPER.apply(Identifier.fromNamespaceAndPath("golden_chests", "golden_right"));

    private final MultiblockChestResources<ChestModel> models;
    private final SpriteGetter sprites;

    public GoldenChestRenderer(BlockEntityRendererProvider.Context context) {
        this.models = new MultiblockChestResources<>(
                new ChestModel(context.bakeLayer(LAYER_SINGLE)),
                new ChestModel(context.bakeLayer(LAYER_DOUBLE_LEFT)),
                new ChestModel(context.bakeLayer(LAYER_DOUBLE_RIGHT)));
        this.sprites = context.sprites();
    }

    @Override
    public GoldenChestRenderState createRenderState() {
        return new GoldenChestRenderState();
    }

    @Override
    public void extractRenderState(
            GoldenChestBlockEntity be,
            GoldenChestRenderState state,
            float partialTick,
            net.minecraft.world.phys.Vec3 cameraPos,
            net.minecraft.client.renderer.feature.ModelFeatureRenderer.CrumblingOverlay crumbling) {
        BlockEntityRenderState.extractBase(be, state, crumbling);
        state.openness = be.getOpenness(partialTick);
        state.enchanted = be.isEnchanted();

        var blockState = be.getBlockState();
        EnumProperty<Direction> facingProp = GoldenChestBlock.FACING;
        EnumProperty<ChestType> typeProp = GoldenChestBlock.TYPE;
        state.facing = blockState.hasProperty(facingProp) ? blockState.getValue(facingProp) : Direction.NORTH;
        state.type = blockState.hasProperty(typeProp) ? blockState.getValue(typeProp) : ChestType.SINGLE;
    }

    @Override
    public void submit(
            GoldenChestRenderState state,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            CameraRenderState cameraState) {
        poseStack.pushPose();
        poseStack.mulPose(ChestRenderer.modelTransformation(state.facing));

        float lidAngle = 1f - state.openness;
        lidAngle = 1f - lidAngle * lidAngle * lidAngle;

        ChestModel model = models.select(state.type);
        SpriteId sprite = switch (state.type) {
            case LEFT -> SPRITE_LEFT;
            case RIGHT -> SPRITE_RIGHT;
            default -> SPRITE_SINGLE;
        };

        model.setupAnim(lidAngle);
        collector.submitModel(
                model,
                lidAngle,
                poseStack,
                state.lightCoords,
                OverlayTexture.NO_OVERLAY,
                -1,
                sprite,
                sprites,
                0,
                state.breakProgress);

        if (state.enchanted) {
            collector.submitModel(
                    model,
                    lidAngle,
                    poseStack,
                    RenderTypes.entityGlint(),
                    state.lightCoords,
                    OverlayTexture.NO_OVERLAY,
                    -1,
                    state.breakProgress);
        }

        poseStack.popPose();
    }
}
