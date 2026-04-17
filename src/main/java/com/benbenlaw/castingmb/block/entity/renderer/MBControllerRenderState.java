package com.benbenlaw.castingmb.block.entity.renderer;

import com.benbenlaw.casting.block.entity.renderer.TankBlockEntityRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.ArrayList;
import java.util.List;

public class MBControllerRenderState extends TankBlockEntityRenderState {
    public List<FluidStack> fluids = new ArrayList<>();
    public BlockPos controllerPos;
    public AABB innerBounds;
}
