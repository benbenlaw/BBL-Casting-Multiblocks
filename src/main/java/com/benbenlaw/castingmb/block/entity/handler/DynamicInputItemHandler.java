package com.benbenlaw.castingmb.block.entity.handler;

import com.benbenlaw.castingmb.block.entity.MBControllerBlockEntity;
import com.benbenlaw.core.block.entity.handler.item.InputItemHandler;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

import java.util.function.BiPredicate;

public class DynamicInputItemHandler extends InputItemHandler {
    private final MBControllerBlockEntity controller;
    private final int absoluteMaxSize;

    public DynamicInputItemHandler(MBControllerBlockEntity blockEntity, int maxSize, BiPredicate<Integer, ItemStack> canInsert) {
        super(blockEntity, maxSize, canInsert);
        this.controller = blockEntity;
        this.absoluteMaxSize = maxSize;
    }

    @Override
    protected int getCapacity(int index, ItemResource resource) {
        return 1;
    }

    @Override
    public int size() {
        return absoluteMaxSize;
    }

    @Override
    public int insert(int index, ItemResource resource, int amount, TransactionContext transaction) {
        if (index >= controller.getMaxItemSlots()) {
            return 0;
        }
        return super.insert(index, resource, amount, transaction);
    }

    @Override
    public boolean isValid(int index, ItemResource resource) {
        if (index >= controller.getMaxItemSlots()) {
            return false;
        }
        return super.isValid(index, resource);
    }
}