package com.benbenlaw.castingmb.block.entity;

import com.benbenlaw.casting.block.custom.CastingBlock;
import com.benbenlaw.casting.block.entity.FluidSending;
import com.benbenlaw.casting.item.CastingDataComponents;
import com.benbenlaw.casting.item.util.FluidListComponent;
import com.benbenlaw.casting.recipe.custom.MeltingRecipe;
import com.benbenlaw.castingmb.block.CastingMBBlockEntities;
import com.benbenlaw.castingmb.block.custom.MBControllerBlock;
import com.benbenlaw.castingmb.block.entity.handler.DynamicInputItemHandler;
import com.benbenlaw.castingmb.block.entity.handler.MultiFluidResourceHandler;
import com.benbenlaw.castingmb.network.packets.SyncFuelTanks;
import com.benbenlaw.castingmb.screen.MBControllerMenu;
import com.benbenlaw.castingmb.util.CastingMBTags;
import com.benbenlaw.core.block.entity.SyncableBlockEntity;
import com.benbenlaw.core.block.entity.handler.fluid.InputFluidHandler;
import com.benbenlaw.core.block.entity.handler.fluid.OutputFluidHandler;
import com.benbenlaw.core.block.entity.handler.item.InputItemHandler;
import com.benbenlaw.core.multiblock.MultiblockData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidStackTemplate;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidUtil;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.OptionalInt;
import java.util.Set;

public class MBControllerBlockEntity extends SyncableBlockEntity implements MenuProvider, FluidSending {

    private final ContainerData data;
    private int[] progress = new int[100];
    private int[] maxProgress = new int[100];
    private OptionalInt temperature = OptionalInt.empty();

    public MultiblockData cachedMultiblockData = null;
    public Set<BlockPos> multiblockBlockPos = new HashSet<>();
    public boolean structureDirty = true;

    private int regulatorCount = 0;
    private int maxItemSlots = 0;
    private BlockPos clientSideFuelTankPos;

    private final InputItemHandler inputHandler =
            new DynamicInputItemHandler(this, 100, (i, stack) -> i >= 0 && i < 99);

    private final MultiFluidResourceHandler outputFluidHandler =
            new MultiFluidResourceHandler(this, 1, 0, i -> true);

    public MBControllerBlockEntity(BlockPos pos, BlockState state) {
        super(CastingMBBlockEntities.MB_CONTROLLER_BLOCK_ENTITY.get(), pos, state);

        this.data = new ContainerData() {
            @Override
            public int get(int index) {
                if (index < 100) return progress[index];
                if (index < 200) return maxProgress[index - 100];
                if (index == 200) return temperature.orElse(Integer.MIN_VALUE);
                if (index == 201) return outputFluidHandler.getCapacityAsInt(0, FluidResource.EMPTY);
                if (index == 202) return maxItemSlots;
                return 0;
            }

            @Override
            public void set(int index, int value) {
                if (index < 100) progress[index] = value;
                else if (index < 200) maxProgress[index - 100] = value;
                else if (index == 200) {
                    temperature = (value == Integer.MIN_VALUE) ? OptionalInt.empty() : OptionalInt.of(value);
                }
                else if (index == 201) {
                    outputFluidHandler.setTotalCapacity(value);
                }
                else if (index == 202) {
                    maxItemSlots = value;
                }
            }

            @Override
            public int getCount() {
                return 203;
            }
        };
    }

    public int getMaxItemSlots() {
        return this.maxItemSlots;
    }

    public void tick() {
        if (level == null || level.isClientSide()) return;

        if (level.getGameTime() % 100 == 0) {
            structureDirty = true;
        }

        if (structureDirty) {
            validateMultiblock();
            if (cachedMultiblockData != null) {
                multiblockBlockPos.clear();
                multiblockBlockPos.addAll(cachedMultiblockData.allBlockPositions());

                updateMultiblockStats();
            }
            structureDirty = false;
        }

        boolean isRunning = level.getBlockState(worldPosition).getValue(MBControllerBlock.RUNNING);
        MBTankBlockEntity activeFuelTank = getActiveFuelTank();
        this.temperature = activeFuelTank != null ? activeFuelTank.getFuelTemp() : OptionalInt.empty();

        if (!isRunning || temperature.isEmpty()) {
            updateWorkingState(false);
            return;
        }

        int currentTemp = temperature.getAsInt();
        boolean changed = false;
        boolean isWorking = false;

        int loopLimit = Math.min(maxItemSlots, 100);
        for (int i = 0; i < loopLimit; i++) {

            if (i >= inputHandler.size()) break;

            ItemStack stack = inputHandler.getResource(i).toStack();

            if (stack.isEmpty()) {
                if (progress[i] > 0) {
                    progress[i] = 0;
                    changed = true;
                }
                continue;
            }

            RecipeHolder<MeltingRecipe> recipeHolder = getRecipeForSlot(stack);

            if (recipeHolder != null) {
                MeltingRecipe recipe = recipeHolder.value();

                if (currentTemp >= recipe.meltingTemp()) {
                    int max = 200;
                    int tempDiff = currentTemp - recipe.meltingTemp();
                    max -= (tempDiff / 100) * 10;
                    if (recipe.durationModifier().isPresent()) {
                        max = (int) (max * recipe.durationModifier().get());
                    }
                    maxProgress[i] = Math.max(max, 20);

                    if (canFitFluids(recipe.output())) {
                        isWorking = true;
                        progress[i]++;
                        changed = true;

                        if (progress[i] >= maxProgress[i]) {
                            executeMelting(i, recipe, activeFuelTank);
                            progress[i] = 0;
                        }
                    }
                } else if (progress[i] > 0) {
                    progress[i] = 0;
                    changed = true;
                }
            }
        }

        updateWorkingState(isWorking);
        this.tickResourceSending(level, worldPosition);

        if (changed) {
            setChanged();
            sync();
        }
    }

    private void updateMultiblockStats() {
        if (cachedMultiblockData == null) return;

        int oldMax = this.maxItemSlots;
        this.maxItemSlots = Math.min(cachedMultiblockData.volume(), 100);

        if (this.level != null) {
            this.level.invalidateCapabilities(this.worldPosition);
        }

        if (this.maxItemSlots < oldMax) {
            clampItemsToCapacity();
        }

        int newCapacity = cachedMultiblockData.volume() * 1000;
        outputFluidHandler.setTotalCapacity(newCapacity);

        this.maxItemSlots = cachedMultiblockData.volume();

        outputFluidHandler.clampFluidsToCapacity();

        this.regulatorCount = 0;
        for (BlockPos pos : cachedMultiblockData.extraBlocks()) {
            BlockState state = level.getBlockState(pos);
            if (state.is(CastingMBTags.Blocks.CONTROLLER_REGULATORS)) {
                regulatorCount++;
            }
        }

        outputFluidHandler.setMaxFluidTypes(1 + regulatorCount);
    }

    private void clampItemsToCapacity() {
        if (level == null || level.isClientSide()) return;

        try (Transaction tx = Transaction.open(null)) {
            for (int i = maxItemSlots; i < inputHandler.size(); i++) {
                ItemResource resource = inputHandler.getResource(i);
                if (!resource.isEmpty()) {
                    int amount = inputHandler.getAmountAsInt(i);

                    Block.popResource(level, worldPosition, resource.toStack(amount));

                    inputHandler.extractInternal(i, resource, amount, tx);
                }
            }
            tx.commit();
        }
    }

    public @Nullable MBTankBlockEntity getActiveFuelTank() {
        if (level == null || cachedMultiblockData == null) return null;

        MBTankBlockEntity hottestTank = null;
        int highestTemp = Integer.MIN_VALUE;

        for (BlockPos mbPos : cachedMultiblockData.extraBlocks()) {
            BlockEntity entity = level.getBlockEntity(mbPos);

            if (entity instanceof MBTankBlockEntity tank) {
                if (!tank.getInputFluidHandler().getResource(0).isEmpty()) {

                    OptionalInt fuelTemp = tank.getFuelTemp();

                    if (fuelTemp.isPresent()) {
                        int currentTemp = fuelTemp.getAsInt();

                        if (currentTemp > highestTemp) {
                            highestTemp = currentTemp;
                            hottestTank = tank;
                        }
                    }
                }
            }
        }
        return hottestTank;
    }
    private void executeMelting(int slot, MeltingRecipe recipe, MBTankBlockEntity fuelTank) {
        FluidStack fuelStack = FluidUtil.getStack(fuelTank.getInputFluidHandler(), 0);
        if (fuelStack.isEmpty()) return;

        var fuelRecipe = MBTankBlockEntity.getFuel(level, fuelStack);
        if (fuelRecipe == null) return;

        int amountToConsume = fuelRecipe.value().fluid().amount();

        try (Transaction tx = Transaction.open(null)) {
            inputHandler.extractInternal(slot, ItemResource.of(inputHandler.getResource(slot).toStack()), recipe.input().count(), tx);

            fuelTank.getInputFluidHandler().extractInternal(0, fuelTank.getInputFluidHandler().getResource(0), amountToConsume, tx);

            for (FluidStackTemplate fluid : recipe.output()) {
                int remaining = fluid.amount();

                for (int tank = 0; tank < outputFluidHandler.getMaxFluidTypes() && remaining > 0; tank++) {
                    remaining -= outputFluidHandler.insertInternal(tank, FluidResource.of(fluid), remaining, tx);
                }
            }
            tx.commit();
        }
    }

    private void validateMultiblock() {
        assert level != null;
        var foundData = com.benbenlaw.casting.multiblock.CoreMultiblockDetector.findMultiblock(level, worldPosition, this.getBlockState().getBlock(), wallState -> wallState.is(CastingMBTags.Blocks.CONTROLLER_WALLS),
                floorState -> floorState.is(CastingMBTags.Blocks.CONTROLLER_FLOORS), extraValidBlocks -> extraValidBlocks.is(CastingMBTags.Blocks.CONTROLLER_EXTRA_BLOCKS), true, true, 1024, 128, 64);
        this.cachedMultiblockData = foundData;

        if (this.cachedMultiblockData != null) {
            MBTankBlockEntity tank = getActiveFuelTank();
            PacketDistributor.sendToAllPlayers(new SyncFuelTanks(this.worldPosition, tank != null ? tank.getBlockPos() : null));

            for (BlockPos pos : cachedMultiblockData.extraBlocks()) {
                if (level.getBlockEntity(pos) instanceof MBSolidifierBlockEntity solidifier) {
                    solidifier.setController(this);
                }
            }
        }

    }

    private void updateWorkingState(boolean working) {
        BlockState state = level.getBlockState(worldPosition);
        if (state.hasProperty(CastingBlock.WORKING) && state.getValue(CastingBlock.WORKING) != working) {
            level.setBlock(worldPosition, state.setValue(CastingBlock.WORKING, working), 3);
        }
    }

    private boolean canFitFluids(List<FluidStackTemplate> outputs) {
        int totalNeeded = outputs.stream().mapToInt(FluidStackTemplate::amount).sum();
        int currentTotal = outputFluidHandler.getTotalFluidAmount();
        int capacity = outputFluidHandler.getCapacityAsInt(0, FluidResource.EMPTY);

        if (currentTotal + totalNeeded > capacity) return false;

        for (FluidStackTemplate out : outputs) {
            boolean canPlace = false;
            for (int i = 0; i < outputFluidHandler.getMaxFluidTypes(); i++) {
                FluidStack existing = FluidUtil.getStack(outputFluidHandler, i);
                if (existing.isEmpty() || FluidStack.isSameFluidSameComponents(existing, out)) {
                    canPlace = true;
                    break;
                }
            }
            if (!canPlace) return false;
        }
        return true;
    }

    private RecipeHolder<MeltingRecipe> getRecipeForSlot(ItemStack stack) {
        if (level == null || level.getServer() == null || stack.isEmpty()) return null;
        return level.getServer().getRecipeManager().recipeMap().values().stream()
                .filter(holder -> holder.value().getType() == MeltingRecipe.TYPE)
                .map(holder -> (RecipeHolder<MeltingRecipe>) holder)
                .filter(holder -> holder.value().input().test(stack))
                .findFirst().orElse(null);
    }

    public void setClientSideFuelTankPos(BlockPos pos) {
        this.clientSideFuelTankPos = pos;
    }

    public BlockPos getClientSideFuelTankPos() {
        return this.clientSideFuelTankPos;
    }

    public boolean onPlayerUse(Player player, InteractionHand hand) {
        return FluidUtil.interactWithFluidHandler(player, hand, this.getBlockPos(), this.getOutputFluidHandler());
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        inputHandler.serialize(output.child("inputs"));
        outputFluidHandler.serialize(output.child("outputFluids"));
        output.putIntArray("progress", progress);
        output.putIntArray("maxProgress", maxProgress);
        output.putInt("temperature", temperature.orElse(Integer.MIN_VALUE));
        super.saveAdditional(output);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        inputHandler.deserialize(input.childOrEmpty("inputs"));
        outputFluidHandler.deserialize(input.childOrEmpty("outputFluids"));
        this.progress = input.getIntArray("progress").orElse(new int[100]);
        this.maxProgress = input.getIntArray("maxProgress").orElse(new int[100]);
        int tempVal = input.getIntOr("temperature", Integer.MIN_VALUE);
        this.temperature = (tempVal == Integer.MIN_VALUE) ? OptionalInt.empty() : OptionalInt.of(tempVal);
        super.loadAdditional(input);
    }

    public InputItemHandler getInputHandler() { return inputHandler; }

    public ResourceHandler<ItemResource> getItemCapability() {
        return inputHandler;
    }

    public MultiFluidResourceHandler getOutputFluidHandler() { return outputFluidHandler; }
    public ResourceHandler<FluidResource> getFluidCapability() { return outputFluidHandler; }

    @Override public @Nullable AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new MBControllerMenu(id, inv, this.worldPosition, data);
    }

    @Override public Component getDisplayName() {
        return Component.translatable("block.castingmb.mb_controller");
    }

    @Override public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        dropInventoryContents(inputHandler);
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder builder) {
        super.collectImplicitComponents(builder);
        builder.set(CastingDataComponents.FLUIDS.get(), FluidListComponent.fromHandlers(outputFluidHandler));
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter components) {
        super.applyImplicitComponents(components);
        FluidListComponent component = components.get(CastingDataComponents.FLUIDS.get());
        if (component != null) {
            this.outputFluidHandler.setTotalCapacity(Integer.MAX_VALUE);
            this.outputFluidHandler.setMaxFluidTypes(64);

            component.applyToHandlers(outputFluidHandler);
            this.structureDirty = true;
        }
    }

    @Override
    public OutputFluidHandler sendingHandler() {
        return outputFluidHandler;
    }
}