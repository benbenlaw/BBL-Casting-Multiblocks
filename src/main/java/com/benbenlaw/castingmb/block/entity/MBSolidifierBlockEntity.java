package com.benbenlaw.castingmb.block.entity;

import com.benbenlaw.casting.block.custom.CastingBlock;
import com.benbenlaw.casting.block.entity.FluidAccepting;
import com.benbenlaw.casting.block.entity.TankBlockEntity;
import com.benbenlaw.casting.recipe.custom.SolidifierRecipe;
import com.benbenlaw.castingmb.block.CastingMBBlockEntities;
import com.benbenlaw.castingmb.block.custom.MBSolidifierBlock;
import com.benbenlaw.castingmb.network.packets.SyncFuelTanks;
import com.benbenlaw.castingmb.screen.MBSolidifierMenu;
import com.benbenlaw.core.block.entity.SyncableBlockEntity;
import com.benbenlaw.core.block.entity.handler.fluid.FilterFluidHandler;
import com.benbenlaw.core.block.entity.handler.fluid.InputFluidHandler;
import com.benbenlaw.core.block.entity.handler.item.CombinedItemHandler;
import com.benbenlaw.core.block.entity.handler.item.InputItemHandler;
import com.benbenlaw.core.block.entity.handler.item.OutputItemHandler;
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
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidUtil;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemUtil;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import java.util.OptionalInt;

public class MBSolidifierBlockEntity extends SyncableBlockEntity implements MenuProvider, FluidAccepting {

    private final ContainerData data;
    private int maxProgress = 200;
    private int progress = 0;
    private OptionalInt temperature = OptionalInt.empty();

    private final InputItemHandler inputHandler = new InputItemHandler(this, 1, (i, stack) -> i == 0);
    private final OutputItemHandler outputHandler = new OutputItemHandler(this, 1, i -> i == 0);
    private final FilterFluidHandler filterFluidHandler = new FilterFluidHandler(this, 1);

    private MBControllerBlockEntity cachedController;
    private BlockPos controllerPos;
    private BlockPos clientSideFuelTankPos;

    public MBSolidifierBlockEntity(BlockPos pos, BlockState state) {
        super(CastingMBBlockEntities.MB_SOLIDIFIER_BLOCK_ENTITY.get(), pos, state);
        this.data = new ContainerData() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case 0 -> MBSolidifierBlockEntity.this.progress;
                    case 1 -> MBSolidifierBlockEntity.this.maxProgress;
                    case 2 -> MBSolidifierBlockEntity.this.temperature.orElse(Integer.MIN_VALUE);
                    case 3 -> {
                        var controller = getController();
                        yield (controller != null) ? controller.cachedMultiblockData.volume() : 0;
                    }
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) {
                switch (index) {
                    case 0 -> MBSolidifierBlockEntity.this.progress = value;
                    case 1 -> MBSolidifierBlockEntity.this.maxProgress = value;
                    case 2 -> MBSolidifierBlockEntity.this.temperature =
                            (value == Integer.MIN_VALUE) ? OptionalInt.empty() : OptionalInt.of(value);
                }
            }

            @Override
            public int getCount() {
                return 4;
            }
        };
    }

    public void setClientSideFuelTankPos(BlockPos pos) {
        this.clientSideFuelTankPos = pos;
    }

    public BlockPos getClientSideFuelTankPos() {
        return this.clientSideFuelTankPos;
    }

    public void tick() {
        if (level == null || level.isClientSide()) return;

        MBControllerBlockEntity controller = getController();
        if (controller == null) return;

        MBTankBlockEntity tank = getCoolestFuelTank();
        PacketDistributor.sendToAllPlayers(new SyncFuelTanks(this.worldPosition, tank != null ? tank.getBlockPos() : null));

        boolean isRunning = level.getBlockState(worldPosition).getValue(MBSolidifierBlock.RUNNING);

        MBTankBlockEntity coolantTank = getCoolestFuelTank();
        int currentTemp = coolantTank != null ? coolantTank.getFuelTemp().orElse(20) : 20;
        this.temperature = OptionalInt.of(currentTemp);

        if (!isRunning) {
            updateWorkingState(false);
            if (progress > 0) {
                progress = 0;
                setChanged();
                sync();
            }
            return;
        }

        boolean changed = false;
        boolean isCurrentlyWorking = false;

        ItemStack inputStack = ItemUtil.getStack(inputHandler, 0);
        RecipeHolder<SolidifierRecipe> recipeHolder = getRecipe();

        // 1. Check Bucket Filling logic using Controller Fluid
        if (canFillBucket(inputStack)) {
            isCurrentlyWorking = true;
            maxProgress = 20;
            progress++;
            changed = true;

            if (progress >= maxProgress) {
                executeBucketFill();
                progress = 0;
            }
        }
        // 2. Check Recipe Solidifying logic
        else if (recipeHolder != null) {
            SolidifierRecipe recipe = recipeHolder.value();

            if (canFormOutput(recipe)) {
                isCurrentlyWorking = true;

                int baseMaxProgress = 200;
                double finalModifier = recipe.durationModifier().orElse(1.0);

                int recipeMeltingTemp = recipe.meltingTemp();
                if (currentTemp < recipeMeltingTemp) {
                    int tempDifference = recipeMeltingTemp - currentTemp;
                    float tempBonus = (tempDifference / 25f) * 0.01f;
                    finalModifier -= tempBonus;
                }

                maxProgress = (int) (baseMaxProgress * finalModifier);
                if (maxProgress < 10) maxProgress = 10;

                progress++;
                changed = true;

                if (progress >= maxProgress) {
                    if (coolantTank != null) {
                        FluidStack coolantStack = FluidUtil.getStack(coolantTank.getInputFluidHandler(), 0);
                        // Ensure there is enough fluid left to complete the craft
                        if (coolantStack.getAmount() >= 100) { // Matches fuelUsage above
                            executeSolidifying(recipe);
                            progress = 0;
                            setChanged();
                        }
                    }
                }
            } else if (progress > 0) {
                progress = 0;
                changed = true;
            }
        } else if (progress > 0) {
            progress = 0;
            changed = true;
        }

        updateWorkingState(isCurrentlyWorking);

        if (changed) {
            setChanged();
            sync();
        }
    }

    public @Nullable MBControllerBlockEntity getController() {
        if (level == null) return null;

        // 1. Return cached if valid
        if (cachedController != null && !cachedController.isRemoved()) {
            return cachedController;
        }

        // 2. ONLY use the synced position. Do NOT scan neighbors here.
        if (controllerPos != null) {
            if (level.getBlockEntity(controllerPos) instanceof MBControllerBlockEntity controller) {
                this.cachedController = controller;
                return controller;
            }
        }
        return null;
    }

    public @Nullable MBTankBlockEntity getCoolestFuelTank() {
        MBControllerBlockEntity controller = getController();

        if (controller == null || controller.cachedMultiblockData == null) {
            return null;
        }

        MBTankBlockEntity coolestTank = null;
        int lowestTemp = Integer.MAX_VALUE;

        for (BlockPos pos : controller.cachedMultiblockData.extraBlocks()) {
            assert level != null;
            if (level.getBlockEntity(pos) instanceof MBTankBlockEntity tank) {
                int temp = tank.getFuelTemp().orElse(1000);
                if (temp < lowestTemp) {
                    lowestTemp = temp;
                    coolestTank = tank;
                }
            }
        }
        return coolestTank;
    }

    public void setController(MBControllerBlockEntity controller) {
        this.cachedController = controller;
        this.controllerPos = controller.getBlockPos();
        this.setChanged();
        this.sync();
    }

    private boolean canFillBucket(ItemStack inputStack) {
        MBControllerBlockEntity controller = getController();
        if (controller == null || !inputStack.is(Items.BUCKET)) return false;

        // Check if any fluid in the controller is at least 1000mB and has a bucket
        var handler = controller.getOutputFluidHandler();
        for (int i = 0; i < handler.size(); i++) {
            FluidStack fluid = FluidUtil.getStack(handler, i);
            if (fluid.getAmount() >= 1000 && !new ItemStack(fluid.getFluid().getBucket()).is(Items.AIR)) {
                try (Transaction tx = Transaction.open(null)) {
                    long inserted = outputHandler.insertInternalReturn(0, ItemResource.of(new ItemStack(fluid.getFluid().getBucket())), 1, tx);
                    return inserted == 1;
                }
            }
        }
        return false;
    }

    private void executeBucketFill() {
        MBControllerBlockEntity controller = getController();
        if (controller == null) return;

        var handler = controller.getOutputFluidHandler();
        for (int i = 0; i < handler.size(); i++) {
            FluidStack fluid = FluidUtil.getStack(handler, i);
            ItemStack fullBucket = new ItemStack(fluid.getFluid().getBucket());

            if (fluid.getAmount() >= 1000 && !fullBucket.is(Items.AIR)) {
                try (Transaction tx = Transaction.open(null)) {
                    handler.extract(i, FluidResource.of(fluid), 1000, tx);
                    inputHandler.extractInternal(0, ItemResource.of(new ItemStack(Items.BUCKET)), 1, tx);
                    outputHandler.insertInternal(0, ItemResource.of(fullBucket), 1, tx);
                    tx.commit();
                    return;
                }
            }
        }
    }

    private void executeSolidifying(SolidifierRecipe recipe) {
        MBControllerBlockEntity controller = getController();
        MBTankBlockEntity coolantTank = getCoolestFuelTank();

        if (controller == null) return;

        var controllerHandler = controller.getOutputFluidHandler();

        try (Transaction tx = Transaction.open(null)) {
            // 1. Extract Molten Metal from the Controller (using recipe.fluid().amount())
            boolean metalExtracted = false;
            for (int i = 0; i < controllerHandler.size(); i++) {
                FluidStack inTank = FluidUtil.getStack(controllerHandler, i);
                if (recipe.fluid().ingredient().test(inTank)) {
                    controllerHandler.extract(i, FluidResource.of(inTank), recipe.fluid().amount(), tx);
                    metalExtracted = true;
                    break;
                }
            }

            if (metalExtracted) {
                // 2. Handle Fuel Consumption if a tank is present
                if (coolantTank != null) {
                    var coolantHandler = coolantTank.getInputFluidHandler();
                    FluidStack fuelStack = FluidUtil.getStack(coolantHandler, 0);

                    // Get the Fuel Recipe to know how much to drain
                    var fuelRecipeHolder = TankBlockEntity.getFuel(level, fuelStack);
                    if (fuelRecipeHolder != null) {
                        // DRAIN THE EXACT AMOUNT FROM THE FUEL RECIPE
                        int amountToDrain = fuelRecipeHolder.value().fluid().amount();
                        coolantHandler.extractInternal(0, FluidResource.of(fuelStack), amountToDrain, tx);
                    }
                }

                // 3. Insert Result Item
                ItemStack result = getStackFromSized(recipe.output());
                if (!result.isEmpty()) {
                    outputHandler.insertInternal(0, ItemResource.of(result), result.getCount(), tx);
                }

                tx.commit();
            }
        }
    }

    private boolean hasEnoughFluid(SolidifierRecipe recipe) {
        MBControllerBlockEntity controller = getController();
        if (controller == null) return false;

        var handler = controller.getOutputFluidHandler();
        for (int i = 0; i < handler.size(); i++) {
            FluidStack inTank = FluidUtil.getStack(handler, i);
            if (!inTank.isEmpty() && recipe.fluid().ingredient().test(inTank) && inTank.getAmount() >= recipe.fluid().amount()) {
                return true;
            }
        }
        return false;
    }

    private RecipeHolder<SolidifierRecipe> getRecipe() {
        MBControllerBlockEntity controller = getController();
        if (level == null || level.getServer() == null || controller == null) return null;

        ItemStack mold = ItemUtil.getStack(inputHandler, 0);
        if (mold.isEmpty()) return null;

        // Get the filter resource
        FluidResource filterResource = filterFluidHandler.getResource(0);
        var handler = controller.getOutputFluidHandler();
        var recipes = level.getServer().getRecipeManager().recipeMap().values().stream()
                .filter(holder -> holder.value().getType() == SolidifierRecipe.TYPE)
                .map(holder -> (RecipeHolder<SolidifierRecipe>) holder)
                .toList();

        for (int i = 0; i < handler.size(); i++) {
            FluidStack fluid = FluidUtil.getStack(handler, i);
            if (fluid.isEmpty()) continue;

            // FILTER CHECK: If filter is not blank, the fluid must match the filter
            if (!filterResource.isEmpty() && !FluidStack.isSameFluidSameComponents(fluid, filterResource.toStack(1))) {
                continue;
            }

            for (var holder : recipes) {
                SolidifierRecipe recipe = holder.value();
                if (recipe.mold().test(mold) && recipe.fluid().ingredient().test(fluid) && fluid.getAmount() >= recipe.fluid().amount()) {
                    return holder;
                }
            }
        }
        return null;
    }

    private void updateWorkingState(boolean working) {
        BlockState currentState = level.getBlockState(worldPosition);
        if (currentState.hasProperty(CastingBlock.WORKING) && currentState.getValue(CastingBlock.WORKING) != working) {
            level.setBlock(worldPosition, currentState.setValue(CastingBlock.WORKING, working), 3);
        }
    }

    private boolean canFormOutput(SolidifierRecipe recipe) {
        ItemStack recipeOutput = getStackFromSized(recipe.output());
        if (recipeOutput.isEmpty()) return false;
        try (Transaction tx = Transaction.open(null)) {
            long inserted = outputHandler.insertInternalReturn(0, ItemResource.of(recipeOutput), recipeOutput.getCount(), tx);
            return inserted == recipeOutput.getCount();
        }
    }

    private ItemStack getStackFromSized(SizedIngredient sizedIngredient) {
        return sizedIngredient.ingredient().items()
                .findFirst()
                .map(holder -> new ItemStack(holder.value(), sizedIngredient.count()))
                .orElse(ItemStack.EMPTY);
    }

    public boolean onPlayerUse(Player player, InteractionHand hand) {
        MBControllerBlockEntity controller = getController();
        if (controller != null) {
            return FluidUtil.interactWithFluidHandler(player, hand, controller.getBlockPos(), controller.getOutputFluidHandler());
        }
        return false;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        inputHandler.serialize(output.child("input"));
        outputHandler.serialize(output.child("output"));
        filterFluidHandler.serialize(output.child("filterFluid"));
        output.putInt("progress", progress);
        output.putInt("maxProgress", maxProgress);
        output.putInt("temperature", temperature.orElse(Integer.MIN_VALUE));
        if (controllerPos != null) {
            output.putLong("controller_pos", controllerPos.asLong());
        }
        super.saveAdditional(output);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        inputHandler.deserialize(input.childOrEmpty("input"));
        outputHandler.deserialize(input.childOrEmpty("output"));
        filterFluidHandler.deserialize(input.childOrEmpty("filterFluid"));
        progress = input.getIntOr("progress", 0);
        maxProgress = input.getIntOr("maxProgress", 200);
        int tempVal = input.getIntOr("temperature", Integer.MIN_VALUE);
        this.temperature = (tempVal == Integer.MIN_VALUE) ? OptionalInt.empty() : OptionalInt.of(tempVal);
        long posLong = input.getLongOr("controller_pos", 0);
        if (posLong != 0) {
            this.controllerPos = BlockPos.of(posLong);
        }
        super.loadAdditional(input);
    }

    public InputItemHandler getInputHandler() { return inputHandler; }
    public OutputItemHandler getOutputHandler() { return outputHandler; }
    public ResourceHandler<ItemResource> getItemCapability() { return new CombinedItemHandler(inputHandler, outputHandler); }
    public FilterFluidHandler getFilterFluidHandler() { return filterFluidHandler; }

    @Override
    public InputFluidHandler receivingHandler() {
        MBControllerBlockEntity controller = getController();
        if (controller == null) return null;

        var controllerHandler = controller.getOutputFluidHandler();

        return new InputFluidHandler(this, 1, controllerHandler.getCapacityAsInt(0, FluidResource.EMPTY), (i, s) -> true) {
            @Override
            public int insert(int index, FluidResource resource, int amount, TransactionContext transaction) {
                // DIRECT call to the controller handler, avoiding any "capability" lookups
                // that might loop back to the Solidifier.
                for (int i = 0; i < controllerHandler.size(); i++) {
                    int inserted = controllerHandler.insertInternal(i, resource, amount, transaction);
                    if (inserted > 0) return inserted;
                }
                return 0;
            }

            @Override
            public int getCapacityAsInt(int index, FluidResource resource) {
                return controllerHandler.getCapacityAsInt(0, resource);
            }
        };
    }

    @Override
    public @Nullable FilterFluidHandler getFilter() { return filterFluidHandler; }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int container, @NonNull Inventory inventory, @NonNull Player player) {
        return new MBSolidifierMenu(container, inventory, this.worldPosition, data);
    }

    @Override
    public @NonNull Component getDisplayName() { return Component.translatable("block.castingmb.mb_solidifier"); }

    @Override
    public void preRemoveSideEffects(@NonNull BlockPos pos, @NonNull BlockState state) {
        dropInventoryContents(inputHandler);
        dropInventoryContents(outputHandler);
    }
}