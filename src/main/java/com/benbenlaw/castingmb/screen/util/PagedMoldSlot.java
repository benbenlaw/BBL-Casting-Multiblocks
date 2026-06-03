package com.benbenlaw.castingmb.screen.util;

import com.benbenlaw.casting.util.CastingTags;
import com.benbenlaw.castingmb.screen.MBSolidifierMenu;
import com.benbenlaw.core.block.entity.handler.item.SyncableItemHandler;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemUtil;
import net.neoforged.neoforge.transfer.transaction.Transaction;

public class PagedMoldSlot extends Slot {
    private final MBSolidifierMenu menu;
    private final int index;

    public PagedMoldSlot(MBSolidifierMenu menu, int index, int x, int y) {
        super(new SimpleContainer(1), 0, x, y);
        this.menu = menu;
        this.index = index;
    }

    private int realIndex() {
        return this.menu.getMoldPage() * 5 + this.index;
    }

    private SyncableItemHandler handler() {
        return this.menu.blockEntity.getStoredMolds();
    }

    public boolean hasItem() {
        return !this.getItem().isEmpty();
    }

    public ItemStack getItem() {
        int real = this.realIndex();
        SyncableItemHandler h = this.handler();
        return real >= 0 && real < h.size() ? ItemUtil.getStack(h, real) : ItemStack.EMPTY;
    }

    public void set(ItemStack stack) {
        int real = this.realIndex();
        SyncableItemHandler h = this.handler();
        if (real >= 0 && real < h.size()) {
            if (!stack.isEmpty()) {
                for(int i = 0; i < h.size(); ++i) {
                    if (i != real) {
                        ItemStack existing = ItemUtil.getStack(h, i);
                        if (!existing.isEmpty() && ItemStack.isSameItemSameComponents(existing, stack)) {
                            return;
                        }
                    }
                }
            }

            h.runInternal(() -> {
                try (Transaction tx = Transaction.openRoot()) {
                    if (stack.isEmpty()) {
                        h.set(real, ItemResource.EMPTY, 0);
                    } else {
                        h.set(real, ItemResource.of(stack.copy()), 1);
                    }

                    tx.commit();
                }

            });
            this.setChanged();
        }
    }

    public ItemStack remove(int amount) {
        ItemStack current = this.getItem();
        if (current.isEmpty()) {
            return ItemStack.EMPTY;
        } else {
            int take = Math.min(amount, current.getCount());
            ItemStack result = current.copy();
            result.setCount(take);
            ItemStack remaining = current.copy();
            remaining.shrink(take);
            this.set(remaining);
            return result;
        }
    }

    public boolean mayPlace(ItemStack stack) {
        if (!stack.is(CastingTags.Items.MOLDS)) {
            return false;
        } else {
            SyncableItemHandler h = this.handler();

            for(int i = 0; i < h.size(); ++i) {
                ItemStack existing = ItemUtil.getStack(h, i);
                if (!existing.isEmpty() && ItemStack.isSameItemSameComponents(existing, stack)) {
                    return false;
                }
            }

            return true;
        }
    }

    public boolean mayPickup(Player player) {
        return true;
    }

    public int getMaxStackSize() {
        return 1;
    }
}
