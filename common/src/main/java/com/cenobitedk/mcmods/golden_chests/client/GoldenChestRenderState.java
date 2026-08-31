package com.cenobitedk.mcmods.golden_chests.client;

import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.properties.ChestType;

public class GoldenChestRenderState extends BlockEntityRenderState {
    public float openness;
    public Direction facing = Direction.NORTH;
    public ChestType type = ChestType.SINGLE;
    public boolean enchanted;
}
