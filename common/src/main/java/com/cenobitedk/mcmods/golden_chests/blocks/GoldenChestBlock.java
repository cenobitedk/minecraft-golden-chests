package com.cenobitedk.mcmods.golden_chests.blocks;

import com.cenobitedk.mcmods.golden_chests.blockentity.GoldenChestBlockEntity;
import com.cenobitedk.mcmods.golden_chests.storage.SharedChestData;
import com.mojang.serialization.MapCodec;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.stats.Stats;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.ContainerUser;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class GoldenChestBlock extends BaseEntityBlock {

    public static final MapCodec<GoldenChestBlock> CODEC = MapCodec.unit(GoldenChestBlock::new);

    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    public static final EnumProperty<ChestType> TYPE = BlockStateProperties.CHEST_TYPE;
    public static final BooleanProperty ENCHANTED = BooleanProperty.create("enchanted");

    // Single-chest shape
    private static final VoxelShape SHAPE_SINGLE = box(1, 0, 1, 15, 14, 15);
    // Half-chest shapes per facing direction — mirrors vanilla ChestBlock's HALF_SHAPES
    private static final java.util.Map<Direction, VoxelShape> HALF_SHAPES =
            Shapes.rotateHorizontal(Block.boxZ(14.0, 0.0, 14.0, 0.0, 15.0));

    public GoldenChestBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition
                .any()
                .setValue(FACING, Direction.NORTH)
                .setValue(TYPE, ChestType.SINGLE)
                .setValue(ENCHANTED, false));
    }

    public GoldenChestBlock() {
        this(BlockBehaviour.Properties.of().strength(2.5f));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, TYPE, ENCHANTED);
    }

    // --- Placement and neighbor updates ---

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection().getOpposite();
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        boolean enchanted = readUnbreakingLevel(context.getItemInHand(), level) > 0;
        // Enchanted chests only join once linking has run in setPlacedBy.
        ChestType type = enchanted ? ChestType.SINGLE : getChestTypeOnPlacement(level, pos, facing, false);
        return defaultBlockState().setValue(FACING, facing).setValue(TYPE, type).setValue(ENCHANTED, enchanted);
    }

    private static ChestType getChestTypeOnPlacement(Level level, BlockPos pos, Direction facing, boolean enchanted) {
        // Check clockwise neighbor (becomes LEFT if partner found there)
        BlockPos cwPos = pos.relative(facing.getClockWise());
        BlockState cwState = level.getBlockState(cwPos);
        if (isPartnerChest(cwState, facing, enchanted)) {
            // Update the clockwise neighbor to RIGHT, we become LEFT
            level.setBlock(
                    cwPos, cwState.setValue(TYPE, ChestType.RIGHT), Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_CLIENTS);
            return ChestType.LEFT;
        }
        // Check counter-clockwise neighbor (becomes RIGHT if partner found there)
        BlockPos ccwPos = pos.relative(facing.getCounterClockWise());
        BlockState ccwState = level.getBlockState(ccwPos);
        if (isPartnerChest(ccwState, facing, enchanted)) {
            // Update the counter-clockwise neighbor to LEFT, we become RIGHT
            level.setBlock(
                    ccwPos, ccwState.setValue(TYPE, ChestType.LEFT), Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_CLIENTS);
            return ChestType.RIGHT;
        }
        return ChestType.SINGLE;
    }

    private static boolean isPartnerChest(BlockState state, Direction facing, boolean enchanted) {
        return state.getBlock() instanceof GoldenChestBlock
                && state.getValue(TYPE) == ChestType.SINGLE
                && state.getValue(FACING) == facing
                && state.getValue(ENCHANTED) == enchanted; // must match: both enchanted or both not
    }

    /** Returns the direction of the partner half, relative to this block's position. */
    public static Direction getConnectedDirection(BlockState state) {
        Direction facing = state.getValue(FACING);
        return state.getValue(TYPE) == ChestType.LEFT ? facing.getClockWise() : facing.getCounterClockWise();
    }

    @Override
    protected BlockState updateShape(
            BlockState state,
            LevelReader level,
            ScheduledTickAccess ticks,
            BlockPos pos,
            Direction direction,
            BlockPos neighborPos,
            BlockState neighborState,
            net.minecraft.util.RandomSource random) {
        Direction facing = state.getValue(FACING);
        ChestType currentType = state.getValue(TYPE);
        boolean thisEnchanted = state.getValue(ENCHANTED);

        // A compatible neighbor = same block, same facing, same enchanted state
        boolean neighborIsCompatible = neighborState.getBlock() instanceof GoldenChestBlock
                && neighborState.getValue(FACING) == facing
                && neighborState.getValue(ENCHANTED) == thisEnchanted;

        // Enchanted chests only join when they share a link. Link ids are not synced to
        // clients, so the client leaves joining to the server.
        if (neighborIsCompatible && thisEnchanted) {
            if (level.isClientSide()) return state;
            neighborIsCompatible = sharesLink(level, pos, neighborPos);
        }

        if (direction == facing.getClockWise()) {
            // The block to our clockwise changed
            if (neighborIsCompatible && currentType != ChestType.RIGHT) {
                return state.setValue(TYPE, ChestType.LEFT);
            } else if (!neighborIsCompatible && currentType == ChestType.LEFT) {
                return state.setValue(TYPE, ChestType.SINGLE);
            }
        } else if (direction == facing.getCounterClockWise()) {
            // The block to our counter-clockwise changed
            if (neighborIsCompatible && currentType != ChestType.LEFT) {
                return state.setValue(TYPE, ChestType.RIGHT);
            } else if (!neighborIsCompatible && currentType == ChestType.RIGHT) {
                return state.setValue(TYPE, ChestType.SINGLE);
            }
        }
        return state;
    }

    @Override
    protected MapCodec<GoldenChestBlock> codec() {
        return CODEC;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (state.getValue(TYPE) == ChestType.SINGLE) return SHAPE_SINGLE;
        // Vanilla uses getConnectedDirection (direction toward partner) as the HALF_SHAPES key
        return HALF_SHAPES.getOrDefault(getConnectedDirection(state), SHAPE_SINGLE);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide()
                ? createTickerHelper(
                        type,
                        com.cenobitedk.mcmods.golden_chests.registry.ModBlockEntityTypes.GOLDEN_CHEST.get(),
                        GoldenChestBlockEntity::lidAnimateTick)
                : null;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new GoldenChestBlockEntity(pos, state);
    }

    // --- Placement: read enchantment and link to neighbours ---

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        if (level.isClientSide()) return;
        if (!(level.getBlockEntity(pos) instanceof GoldenChestBlockEntity chest)) return;

        // Always derive unbreakingLevel from the item's current ENCHANTMENTS, not from
        // BLOCK_ENTITY_DATA.unbreaking_level. Grindstone removes ENCHANTMENTS but leaves
        // BLOCK_ENTITY_DATA intact, so reading from ENCHANTMENTS correctly reflects the
        // grindstone's effect.
        int lvl = readUnbreakingLevel(stack, level);
        chest.setUnbreakingLevel(lvl);

        if (lvl == 0) {
            UUID oldLinkId = chest.getLinkId();
            if (oldLinkId != null && level instanceof ServerLevel sl) {
                // Grindstoned — this chest still holds its own ref at this point,
                // so use hasOtherRefs (count > 1) to detect if any partner also holds a ref.
                // If no partner is loaded, drop orphaned items from the shared storage.
                if (!SharedChestData.get(sl).hasOtherRefs(oldLinkId)) {
                    SharedChestData data = SharedChestData.get(sl);
                    Containers.dropContents(level, pos, data.getOrCreate(oldLinkId));
                    data.remove(oldLinkId);
                }
            }
            // Grindstone removed the enchantment — break the link so this chest acts
            // as a fresh unenchanted chest regardless of any stale link_id in its data.
            chest.setLinkId(null);
        } else {
            tryLink(level, pos, chest);
        }
    }

    static int readUnbreakingLevel(ItemStack stack, Level level) {
        ItemEnchantments enchants = stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        return level.registryAccess()
                .lookup(Registries.ENCHANTMENT)
                .flatMap(reg -> reg.get(Enchantments.UNBREAKING))
                .map(enchants::getLevel)
                .orElse(0);
    }

    /**
     * Links the placed chest with an adjacent enchanted chest, but only when neither is already
     * part of a pair. A chest that still has a live link (its partner is placed elsewhere or is
     * still an item) only reconnects with that partner, so two pairs never merge.
     */
    private static void tryLink(Level level, BlockPos pos, GoldenChestBlockEntity chest) {
        if (!(level instanceof ServerLevel sl)) return;
        SharedChestData data = SharedChestData.get(sl);

        if (chest.getLinkId() != null && !data.exists(chest.getLinkId())) {
            chest.setLinkId(null);
        }

        if (chest.getLinkId() == null) {
            Direction facing = level.getBlockState(pos).getValue(FACING);
            Direction[] order = {facing.getClockWise(), facing.getCounterClockWise(), facing, facing.getOpposite()};
            for (Direction dir : order) {
                BlockPos nPos = pos.relative(dir);
                if (!(level.getBlockState(nPos).getBlock() instanceof GoldenChestBlock)
                        || !(level.getBlockEntity(nPos) instanceof GoldenChestBlockEntity neighbour)) continue;
                if (!neighbour.isEnchanted()) continue;
                if (neighbour.getLinkId() != null && data.exists(neighbour.getLinkId())) continue;

                UUID linkId = UUID.randomUUID();
                int sharedLevel = Math.max(chest.getUnbreakingLevel(), neighbour.getUnbreakingLevel());
                data.getOrCreate(linkId);
                chest.setLinkId(linkId);
                chest.setUnbreakingLevel(sharedLevel);
                neighbour.setLinkId(linkId);
                neighbour.setUnbreakingLevel(sharedLevel);
                break;
            }
        }

        connectToLinkedPartner(level, pos, chest);
    }

    /** Joins the placed chest visually with a side neighbour that shares its link. */
    private static void connectToLinkedPartner(Level level, BlockPos pos, GoldenChestBlockEntity chest) {
        if (chest.getLinkId() == null) return;
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof GoldenChestBlock) || state.getValue(TYPE) != ChestType.SINGLE) return;
        Direction facing = state.getValue(FACING);

        for (ChestType ownType : new ChestType[] {ChestType.LEFT, ChestType.RIGHT}) {
            Direction dir = ownType == ChestType.LEFT ? facing.getClockWise() : facing.getCounterClockWise();
            BlockPos nPos = pos.relative(dir);
            BlockState nState = level.getBlockState(nPos);
            if (isPartnerChest(nState, facing, true) && sharesLink(level, pos, nPos)) {
                ChestType partnerType = ownType == ChestType.LEFT ? ChestType.RIGHT : ChestType.LEFT;
                level.setBlock(nPos, nState.setValue(TYPE, partnerType), Block.UPDATE_ALL);
                level.setBlock(pos, state.setValue(TYPE, ownType), Block.UPDATE_ALL);
                return;
            }
        }
    }

    private static boolean sharesLink(BlockGetter level, BlockPos a, BlockPos b) {
        return level.getBlockEntity(a) instanceof GoldenChestBlockEntity chestA
                && level.getBlockEntity(b) instanceof GoldenChestBlockEntity chestB
                && chestA.getLinkId() != null
                && chestA.getLinkId().equals(chestB.getLinkId());
    }

    // --- Opening the chest ---

    @Override
    public InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (!(level.getBlockEntity(pos) instanceof GoldenChestBlockEntity chest)) return InteractionResult.CONSUME;

        ChestType type = state.getValue(TYPE);
        if (type != ChestType.SINGLE && !chest.isEnchanted()) {
            // Unenchanted double chest → 54-slot combined view
            Direction partnerDir = getConnectedDirection(state);
            if (level.getBlockEntity(pos.relative(partnerDir)) instanceof GoldenChestBlockEntity partner
                    && !partner.isEnchanted()) {
                GoldenChestBlockEntity leftBE = type == ChestType.LEFT ? chest : partner;
                GoldenChestBlockEntity rightBE = type == ChestType.LEFT ? partner : chest;
                player.openMenu(doubleChestMenu(leftBE, rightBE));
                player.awardStat(Stats.OPEN_CHEST);
                return InteractionResult.CONSUME;
            }
        }

        // Enchanted/linked or single → 27-slot own/shared inventory
        player.openMenu(chest);
        player.awardStat(Stats.OPEN_CHEST);
        return InteractionResult.CONSUME;
    }

    private static MenuProvider doubleChestMenu(GoldenChestBlockEntity left, GoldenChestBlockEntity right) {
        return new MenuProvider() {
            @Override
            public net.minecraft.network.chat.Component getDisplayName() {
                return left.getDisplayName();
            }

            @Override
            public net.minecraft.world.inventory.AbstractContainerMenu createMenu(
                    int id, net.minecraft.world.entity.player.Inventory inv, Player player) {
                Container combined = new Container() {
                    final net.minecraft.world.SimpleContainer l = left.getEffectiveContainer();
                    final net.minecraft.world.SimpleContainer r = right.getEffectiveContainer();

                    @Override
                    public int getContainerSize() {
                        return 54;
                    }

                    @Override
                    public boolean isEmpty() {
                        return l.isEmpty() && r.isEmpty();
                    }

                    @Override
                    public net.minecraft.world.item.ItemStack getItem(int s) {
                        return s < 27 ? l.getItem(s) : r.getItem(s - 27);
                    }

                    @Override
                    public net.minecraft.world.item.ItemStack removeItem(int s, int c) {
                        return s < 27 ? l.removeItem(s, c) : r.removeItem(s - 27, c);
                    }

                    @Override
                    public net.minecraft.world.item.ItemStack removeItemNoUpdate(int s) {
                        return s < 27 ? l.removeItemNoUpdate(s) : r.removeItemNoUpdate(s - 27);
                    }

                    @Override
                    public void setItem(int s, net.minecraft.world.item.ItemStack st) {
                        if (s < 27) l.setItem(s, st);
                        else r.setItem(s - 27, st);
                    }

                    @Override
                    public void setChanged() {
                        l.setChanged();
                        r.setChanged();
                    }

                    @Override
                    public boolean stillValid(Player p) {
                        return true;
                    }

                    @Override
                    public void clearContent() {
                        l.clearContent();
                        r.clearContent();
                    }

                    @Override
                    public void startOpen(ContainerUser u) {
                        left.startOpen(u);
                    }

                    @Override
                    public void stopOpen(ContainerUser u) {
                        left.stopOpen(u);
                    }
                };
                return new ChestMenu(MenuType.GENERIC_9x6, id, inv, combined, 6);
            }
        };
    }

    @Override
    public boolean triggerEvent(BlockState state, Level level, BlockPos pos, int type, int data) {
        super.triggerEvent(state, level, pos, type, data);
        BlockEntity be = level.getBlockEntity(pos);
        return be != null && be.triggerEvent(type, data);
    }

    // --- Breaking ---

    @Override
    public void playerDestroy(
            Level level, Player player, BlockPos pos, BlockState state, BlockEntity be, ItemStack tool) {
        player.awardStat(Stats.BLOCK_MINED.get(this));
        player.causeFoodExhaustion(0.005F);

        if (!level.isClientSide() && be instanceof GoldenChestBlockEntity chest) {
            if (!chest.isLinked()) {
                Containers.dropContents(level, pos, chest.getEffectiveContainer());
            } else if (level instanceof ServerLevel sl) {
                UUID linkId = chest.getLinkId();
                SharedChestData data = SharedChestData.get(sl);
                // By the time playerDestroy runs, setRemoved has already decremented the ref.
                // If activeRefs is now 0, this was the last loaded linked chest.
                if (!data.hasActiveRefs(linkId)) {
                    Containers.dropContents(sl, pos, data.getOrCreate(linkId));
                    data.remove(linkId);
                }
            }
            Block.popResource(level, pos, buildDrop(chest, level));
        }
    }

    private ItemStack buildDrop(GoldenChestBlockEntity chest, Level level) {
        ItemStack stack = new ItemStack(this);
        int lvl = chest.getUnbreakingLevel();

        // Only embed BLOCK_ENTITY_DATA (specifically link_id) when a live partner
        // chest still exists. By the time playerDestroy runs, setRemoved has already
        // decremented the ref, so hasActiveRefs==true means another chest is still linked.
        // Without this, a mined enchanted chest (no partner) would carry a dead link_id
        // and fail to stack with freshly enchanted chests that have no BLOCK_ENTITY_DATA.
        boolean partnerExists = chest.isLinked()
                && level instanceof ServerLevel sl
                && SharedChestData.get(sl).hasActiveRefs(chest.getLinkId());

        if (partnerExists) {
            var output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
            chest.saveCustomOnly(output);
            output.discard("items");
            BlockItem.setBlockEntityData(stack, chest.getType(), output);
        }

        if (lvl > 0) {
            level.registryAccess()
                    .lookup(Registries.ENCHANTMENT)
                    .flatMap(reg -> reg.get(Enchantments.UNBREAKING))
                    .ifPresent(holder -> stack.enchant(holder, lvl));
        }
        return stack;
    }
}
