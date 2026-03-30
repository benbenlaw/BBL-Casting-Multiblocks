package com.benbenlaw.castingmb.mixin;

import com.benbenlaw.castingmb.util.IMovableSlot;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(Slot.class)
public abstract class SlotMixin implements IMovableSlot {

    @Final
    @Shadow
    @Mutable
    public int x;
    @Final
    @Shadow @Mutable public int y;

    @Override
    public void castingmb$setPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }
}