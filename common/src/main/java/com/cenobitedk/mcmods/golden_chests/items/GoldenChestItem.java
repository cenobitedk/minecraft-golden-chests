package com.cenobitedk.mcmods.golden_chests.items;

import com.cenobitedk.mcmods.golden_chests.storage.SharedChestData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import java.util.UUID;

/**
 * Custom BlockItem that forces enchantment glint when the chest item carries
 * an unbreaking_level in its BLOCK_ENTITY_DATA component, or has normal enchantments.
 */
public class GoldenChestItem extends BlockItem {

    public GoldenChestItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return stack.isEnchanted();
    }

    @Override
    public void appendHoverText(ItemStack stack, net.minecraft.world.item.Item.TooltipContext context,
                                net.minecraft.world.item.component.TooltipDisplay display,
                                java.util.function.Consumer<Component> tooltip,
                                net.minecraft.world.item.TooltipFlag flag) {
        super.appendHoverText(stack, context, display, tooltip, flag);
        var beData = stack.get(DataComponents.BLOCK_ENTITY_DATA);
        if (beData != null && !beData.getUnsafe().getStringOr("link_id", "").isEmpty()) {
            tooltip.accept(Component.translatable("item.golden_chests.golden_chest.linked")
                    .withStyle(ChatFormatting.AQUA));
        }
    }

    /**
     * Called when the player takes this item from any crafting result (crafting table, etc.).
     * The grindstone does NOT call this — see inventoryTick for the grindstone cleanup path.
     */
    @Override
    public void onCraftedBy(ItemStack stack, Player player) {
        super.onCraftedBy(stack, player);
        stripBlockEntityData(stack);
    }

    /**
     * Strips stale BLOCK_ENTITY_DATA from unenchanted golden chests each inventory tick.
     * This covers the grindstone case: GrindstoneMenu uses a plain Slot (not ResultSlot)
     * so onCraftedBy is never fired; inventoryTick cleans up on the next server tick.
     */
    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, EquipmentSlot slot) {
        super.inventoryTick(stack, level, entity, slot);
        normalizeStack(stack, level);
        // Also clean the cursor item — inventoryTick only fires for inventory slots,
        // not for the item held on the mouse cursor (e.g. just taken from grindstone).
        if (entity instanceof Player player) {
            ItemStack carried = player.containerMenu.getCarried();
            if (carried.getItem() == this) normalizeStack(carried, level);
        }
    }

    /**
     * Removes stale components that prevent stacking:
     * - Unenchanted items: always strip BLOCK_ENTITY_DATA (grindstone leaves it behind)
     * - Enchanted items with a link_id: strip BLOCK_ENTITY_DATA if no chest in the world
     *   still holds that link — the partner was mined too, so the link is dead and the
     *   item should behave like any other enchanted chest of that level.
     */
    private static void normalizeStack(ItemStack stack, ServerLevel level) {
        if (!stack.isEnchanted()) {
            stripBlockEntityData(stack);
            return;
        }
        var beData = stack.get(DataComponents.BLOCK_ENTITY_DATA);
        if (beData == null) return;
        String linkIdStr = beData.getUnsafe().getStringOr("link_id", "");
        if (linkIdStr.isEmpty()) return;
        try {
            UUID linkId = UUID.fromString(linkIdStr);
            if (!SharedChestData.get(level).hasActiveRefs(linkId)) {
                stripBlockEntityData(stack);
            }
        } catch (IllegalArgumentException ignored) {
            stripBlockEntityData(stack); // malformed UUID — clean it up
        }
    }

    private static void stripBlockEntityData(ItemStack stack) {
        stack.remove(DataComponents.BLOCK_ENTITY_DATA);
    }
}
