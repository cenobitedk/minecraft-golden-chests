package com.cenobitedk.mcmods.golden_chests.registry;

import com.cenobitedk.mcmods.golden_chests.blockentity.GoldenChestBlockEntity;
import java.util.function.Supplier;
import net.minecraft.world.level.block.entity.BlockEntityType;

public class ModBlockEntityTypes {
    public static Supplier<BlockEntityType<GoldenChestBlockEntity>> GOLDEN_CHEST;
}
