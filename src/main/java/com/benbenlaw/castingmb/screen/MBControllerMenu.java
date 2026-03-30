package com.benbenlaw.castingmb.screen;

import com.benbenlaw.castingmb.block.entity.MBControllerBlockEntity;
import com.benbenlaw.castingmb.util.IMovableSlot;
import com.benbenlaw.core.screen.SimpleAbstractContainerMenu;
import com.benbenlaw.core.screen.util.slot.InputSlot;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class MBControllerMenu extends SimpleAbstractContainerMenu {

    public MBControllerBlockEntity blockEntity;
    public Level level;
    public ContainerData data;
    protected Player player;
    public BlockPos blockPos;

    private int lastMaxSlots = -1;

    public MBControllerMenu(int containerID, Inventory inventory, FriendlyByteBuf extraData) {
        this(containerID, inventory, extraData.readBlockPos(), new SimpleContainerData(203));
    }

    public MBControllerMenu(int containerID, Inventory inventory, BlockPos blockPos, ContainerData data) {
        super(CastingMBMenuTypes.MB_CONTROLLER_MENU.get(), containerID, inventory, blockPos, 100);

        if (data.getCount() < 203) {
            throw new IllegalArgumentException("ContainerData too small! Expected 203, got " + data.getCount());
        }

        this.player = inventory.player;
        this.blockPos = blockPos;
        this.level = inventory.player.level();
        this.data = data;

        this.addDataSlots(data);

        this.blockEntity = (MBControllerBlockEntity) this.level.getBlockEntity(blockPos);

        if (blockEntity != null) {
            for (int i = 0; i < 100; i++) {
                this.addSlot(new InputSlot(blockEntity.getInputHandler(),
                        blockEntity.getInputHandler()::set, i, -2000, -2000).size(1));
            }
        }
    }

    public void scrollTo(int rowOffset) {
        int startSlot = rowOffset * 5;
        int machineStartOffset = 36;

        int maxAllowedSlots = this.data.get(202);

        for (int i = 0; i < 100; i++) {
            int menuSlotIndex = i + machineStartOffset;
            Slot slot = this.getSlot(menuSlotIndex);
            IMovableSlot movable = (IMovableSlot) slot;

            if (i >= startSlot && i < startSlot + 15 && i < maxAllowedSlots) {
                int visualIndex = i - startSlot;
                int col = visualIndex % 5;
                int row = visualIndex / 5;

                movable.castingmb$setPosition(8 + (col * 19), 16 + (row * 19));
            } else {
                movable.castingmb$setPosition(-2000, -2000);
            }
        }
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        int currentMaxSlots = this.data.get(202);
        if (currentMaxSlots != lastMaxSlots) {
            this.lastMaxSlots = currentMaxSlots;
            this.scrollTo(0);
        }
    }

    public int getScaledProgress(int visualSlotIndex, int rowOffset) {
        int actualSlot = (rowOffset * 5) + visualSlotIndex;

        if (actualSlot < 0 || actualSlot >= 100) return 0;

        int progress = this.data.get(actualSlot);
        int maxProgress = this.data.get(actualSlot + 100);
        int progressBarHeight = 16;

        if (maxProgress <= 0 || progress <= 0) return 0;
        return (int) Math.ceil((double) progress * progressBarHeight / maxProgress);
    }

    @Override
    public ItemStack quickMoveStack(Player playerIn, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot != null && slot.hasItem()) {
            ItemStack itemstack1 = slot.getItem();
            itemstack = itemstack1.copy();

            int playerInventoryEnd = 36;
            int machineSlotsStart = 36;
            int activeMachineSlots = this.data.get(202);
            int machineSlotsEnd = machineSlotsStart + activeMachineSlots;

            if (index >= machineSlotsStart) {
                if (!this.moveItemStackTo(itemstack1, 0, playerInventoryEnd, true)) {
                    return ItemStack.EMPTY;
                }
            }
            else {
                if (!this.moveItemStackTo(itemstack1, machineSlotsStart, machineSlotsEnd, false)) {
                    return ItemStack.EMPTY;
                }
            }

            if (itemstack1.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            if (itemstack1.getCount() == itemstack.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(playerIn, itemstack1);
        }

        return itemstack;
    }
}