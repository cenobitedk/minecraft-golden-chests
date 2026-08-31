package com.cenobitedk.mcmods.golden_chests.blockentity;

import com.cenobitedk.mcmods.golden_chests.blocks.GoldenChestBlock;
import com.cenobitedk.mcmods.golden_chests.registry.ModBlockEntityTypes;
import com.cenobitedk.mcmods.golden_chests.storage.SharedChestData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.ContainerUser;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestLidController;
import net.minecraft.world.level.block.entity.ContainerOpenersCounter;
import net.minecraft.world.level.block.entity.LidBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

import java.util.UUID;

public class GoldenChestBlockEntity extends BlockEntity implements MenuProvider, LidBlockEntity {

    private int unbreakingLevel = 0;
    private UUID linkId = null;

    private final ChestLidController lidController = new ChestLidController();

    // Permanent local container — the single source of truth for unlinked chests.
    // Using a stable field avoids all sync issues from creating new instances per call.
    private final SimpleContainer localContainer = new SimpleContainer(SharedChestData.CHEST_SIZE) {
        @Override public void startOpen(ContainerUser user) { GoldenChestBlockEntity.this.startOpen(user); }
        @Override public void stopOpen(ContainerUser user)  { GoldenChestBlockEntity.this.stopOpen(user); }
        @Override public void setChanged() {
            super.setChanged();
            GoldenChestBlockEntity.this.setChanged();
        }
    };

    private final ContainerOpenersCounter openersCounter = new ContainerOpenersCounter() {
        @Override
        protected void onOpen(Level level, BlockPos pos, BlockState state) {
            level.playSound(null, pos, SoundEvents.CHEST_OPEN, SoundSource.BLOCKS, 0.5f,
                    level.getRandom().nextFloat() * 0.1f + 0.9f);
        }

        @Override
        protected void onClose(Level level, BlockPos pos, BlockState state) {
            level.playSound(null, pos, SoundEvents.CHEST_CLOSE, SoundSource.BLOCKS, 0.5f,
                    level.getRandom().nextFloat() * 0.1f + 0.9f);
        }

        @Override
        protected void openerCountChanged(Level level, BlockPos pos, BlockState state, int oldCount, int newCount) {
            level.blockEvent(pos, state.getBlock(), 1, newCount);
            // If we're part of a double chest, keep the partner's lid in sync.
            if (state.hasProperty(GoldenChestBlock.TYPE)
                    && state.getValue(GoldenChestBlock.TYPE)
                       != net.minecraft.world.level.block.state.properties.ChestType.SINGLE) {
                net.minecraft.core.Direction partnerDir =
                    GoldenChestBlock.getConnectedDirection(state);
                level.blockEvent(pos.relative(partnerDir), state.getBlock(), 1, newCount);
            }
        }

        @Override
        public boolean isOwnContainer(Player player) {
            if (player.containerMenu instanceof ChestMenu menu) {
                return menu.getContainer() == getEffectiveContainer();
            }
            return false;
        }
    };

    public GoldenChestBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntityTypes.GOLDEN_CHEST.get(), pos, state);
    }

    // --- Lid animation ---

    public static void lidAnimateTick(Level level, BlockPos pos, BlockState state, GoldenChestBlockEntity be) {
        be.lidController.tickLid();
    }

    @Override public float getOpenNess(float partialTick)  { return lidController.getOpenness(partialTick); }
    public         float getOpenness(float partialTick)    { return lidController.getOpenness(partialTick); }

    public void startOpen(ContainerUser user) {
        if (!isRemoved() && level != null)
            openersCounter.incrementOpeners(user.getLivingEntity(), level, getBlockPos(), getBlockState(),
                    user.getContainerInteractionRange());
    }

    public void stopOpen(ContainerUser user) {
        if (!isRemoved() && level != null)
            openersCounter.decrementOpeners(user.getLivingEntity(), level, getBlockPos(), getBlockState());
    }

    @Override
    public boolean triggerEvent(int type, int data) {
        if (type == 1) { lidController.shouldBeOpen(data > 0); return true; }
        return super.triggerEvent(type, data);
    }

    // --- Level lifecycle (ref counting) ---

    @Override
    public void setLevel(Level level) {
        super.setLevel(level);
        // setLevel fires BEFORE loadAdditional, so linkId is always null here.
        // Ref registration is done in loadAdditional once linkId is known.
    }

    @Override
    public void setRemoved() {
        // Decrement ref before calling super so level is still set
        if (linkId != null && level instanceof ServerLevel sl) {
            SharedChestData.get(sl).removeRef(linkId);
        }
        super.setRemoved();
    }

    // --- Enchantment & linking state ---

    public boolean isEnchanted() { return unbreakingLevel > 0; }
    public int  getUnbreakingLevel() { return unbreakingLevel; }

    public void setUnbreakingLevel(int level) {
        this.unbreakingLevel = level;
        setChanged();
        // Push the update to clients so the glint renders immediately
        if (this.level != null && !this.level.isClientSide()) {
            this.level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
            // Sync the ENCHANTED block state property
            BlockState state = getBlockState();
            boolean enchanted = level > 0;
            if (state.hasProperty(GoldenChestBlock.ENCHANTED)
                    && state.getValue(GoldenChestBlock.ENCHANTED) != enchanted) {
                this.level.setBlock(getBlockPos(),
                        state.setValue(GoldenChestBlock.ENCHANTED, enchanted),
                        Block.UPDATE_ALL);
            }
        }
    }

    // --- Client sync ---

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("unbreaking_level", unbreakingLevel);
        return tag;
    }

    public UUID getLinkId() { return linkId; }

    public void setLinkId(UUID id) {
        UUID oldLinkId = this.linkId;
        this.linkId = id;
        setChanged();

        // Maintain ref counts when linkId changes
        if (level instanceof ServerLevel sl) {
            if (oldLinkId != null && !oldLinkId.equals(id)) {
                SharedChestData.get(sl).removeRef(oldLinkId);
            }
            if (id != null && !id.equals(oldLinkId)) {
                SharedChestData.get(sl).addRef(id);
            }
        }
    }

    public boolean isLinked() { return linkId != null; }

    // --- Inventory ---

    /** Returns the container that backs this chest. Always the same instance for unlinked chests. */
    public SimpleContainer getEffectiveContainer() {
        if (linkId != null && level instanceof ServerLevel sl)
            return SharedChestData.get(sl).getOrCreate(linkId);
        return localContainer;
    }

    // --- MenuProvider ---

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.golden_chests.golden_chest");
    }

    @Override
    public AbstractContainerMenu createMenu(int windowId, Inventory playerInventory, Player player) {
        SimpleContainer inner = getEffectiveContainer();
        // Wrap so startOpen/stopOpen always reach this BE's ContainerOpenersCounter,
        // even when inner is a shared (SharedChestData) container.
        Container decorated = new Container() {
            @Override public int getContainerSize()                    { return inner.getContainerSize(); }
            @Override public boolean isEmpty()                         { return inner.isEmpty(); }
            @Override public ItemStack getItem(int s)                  { return inner.getItem(s); }
            @Override public ItemStack removeItem(int s, int c)        { return inner.removeItem(s, c); }
            @Override public ItemStack removeItemNoUpdate(int s)       { return inner.removeItemNoUpdate(s); }
            @Override public void setItem(int s, ItemStack st)        { inner.setItem(s, st); }
            @Override public void setChanged()                         { inner.setChanged(); }
            @Override public boolean stillValid(Player p)              { return inner.stillValid(p); }
            @Override public void clearContent()                       { inner.clearContent(); }
            @Override public void startOpen(ContainerUser u)  { GoldenChestBlockEntity.this.startOpen(u); }
            @Override public void stopOpen(ContainerUser u)   { GoldenChestBlockEntity.this.stopOpen(u); }
        };
        return new ChestMenu(MenuType.GENERIC_9x3, windowId, playerInventory, decorated, 3);
    }

    // --- Serialization ---

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("unbreaking_level", unbreakingLevel);
        if (linkId != null) output.putString("link_id", linkId.toString());
        if (linkId == null) {
            // Save the permanent local container's items
            ContainerHelper.saveAllItems(output, localContainer.getItems());
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        unbreakingLevel = input.getIntOr("unbreaking_level", 0);
        input.getString("link_id").ifPresent(s -> linkId = UUID.fromString(s));
        // Register ref now that linkId is known. setLevel() already fired (it always fires before
        // loadAdditional) with linkId==null, so this is the correct place to call addRef.
        if (linkId != null && level instanceof ServerLevel sl) {
            SharedChestData.get(sl).addRef(linkId);
        }
        if (linkId == null) {
            var items = net.minecraft.core.NonNullList.withSize(SharedChestData.CHEST_SIZE, ItemStack.EMPTY);
            ContainerHelper.loadAllItems(input, items);
            // Populate localContainer directly, bypassing setChanged()
            for (int i = 0; i < items.size(); i++) localContainer.getItems().set(i, items.get(i));
        }
    }
}
