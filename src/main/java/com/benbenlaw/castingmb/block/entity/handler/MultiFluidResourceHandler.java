package com.benbenlaw.castingmb.block.entity.handler;

import com.benbenlaw.core.block.entity.SyncableBlockEntity;
import com.benbenlaw.core.block.entity.handler.fluid.OutputFluidHandler;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidUtil;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

import java.util.function.Predicate;

public class MultiFluidResourceHandler extends OutputFluidHandler {
    private int totalCapacity;
    private int maxFluidTypes;
    // Store our own reference to bypass the private access in the parent class
    private final SyncableBlockEntity syncableBlockEntity;

    public MultiFluidResourceHandler(SyncableBlockEntity blockEntity, int maxFluidTypes, int totalCapacity, Predicate<Integer> canOutput) {
        super(blockEntity, 20, 1000000, canOutput);

        this.syncableBlockEntity = blockEntity;
        this.totalCapacity = totalCapacity;
        this.maxFluidTypes = maxFluidTypes;
    }

    public void setTotalCapacity(int newCapacity) {
        if (this.totalCapacity != newCapacity) {
            this.totalCapacity = newCapacity;
            this.syncableBlockEntity.setChanged();
            this.syncableBlockEntity.sync(); // Sync to client so the GUI updates the bar height
        }
    }

    public void setMaxFluidTypes(int max) {
        int cappedMax = Math.min(max, this.size());
        if (this.maxFluidTypes != cappedMax) {
            this.maxFluidTypes = cappedMax;
            this.syncableBlockEntity.setChanged();
            this.syncableBlockEntity.sync();
        }
    }

    @Override
    public int insert(int index, FluidResource resource, int amount, TransactionContext transaction) {
        // 1. Basic Validation
        if (resource.isEmpty() || amount <= 0 || index >= maxFluidTypes) return 0;

        // 2. Multiblock Capacity Check
        int currentTotal = getTotalFluidAmount();
        int spaceLeft = Math.max(0, this.totalCapacity - currentTotal);
        int actualToInsert = Math.min(amount, spaceLeft);

        if (actualToInsert <= 0) return 0;

        // 3. Resource Validation (The "getStack" equivalent for Transfer API)
        FluidResource existingResource = getResource(index);

        // If the slot isn't empty AND it's a different fluid, we can't insert here
        if (!existingResource.isEmpty() && !existingResource.equals(resource)) {
            return 0;
        }

        // 4. Perform the actual insertion using the super class
        return super.insert(index, resource, actualToInsert, transaction);
    }

    @Override
    public int getCapacityAsInt(int index, FluidResource resource) {
        return this.totalCapacity;
    }
    public int getTotalFluidAmount() {
        int total = 0;
        for (int i = 0; i < this.size(); i++) {
            total += this.getAmountAsInt(i);
        }
        return total;
    }

    public int getMaxFluidTypes() {
        return maxFluidTypes;
    }

    public void clampFluidsToCapacity() {
        int currentTotal = getTotalFluidAmount();
        if (currentTotal <= this.totalCapacity) return;

        int amountToRemove = currentTotal - this.totalCapacity;

        // Iterate backwards through slots to void the most recently added fluids first
        try (Transaction tx = Transaction.open(null)) {
            for (int i = this.size() - 1; i >= 0 && amountToRemove > 0; i--) {
                int slotAmount = getAmountAsInt(i);
                if (slotAmount > 0) {
                    int toExtract = Math.min(slotAmount, amountToRemove);
                    extract(i, getResource(i), toExtract, tx);
                    amountToRemove -= toExtract;
                }
            }
            tx.commit();
        }
        this.syncableBlockEntity.setChanged();
        this.syncableBlockEntity.sync();
    }
}