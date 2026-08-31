package com.cenobitedk.mcmods.golden_chests.registry;

import com.cenobitedk.mcmods.golden_chests.blockentity.GoldenChestBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.function.Supplier;

public class ModBlockEntityTypes {
    public static Supplier<BlockEntityType<GoldenChestBlockEntity>> GOLDEN_CHEST;
}
