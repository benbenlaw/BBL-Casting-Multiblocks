package com.benbenlaw.castingmb.block.entity.renderer;

import com.benbenlaw.casting.item.CastingDataComponents;
import com.benbenlaw.casting.item.util.FluidListComponent;
import com.benbenlaw.core.util.FluidRendererUtil;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.NonNull;

import java.util.function.Consumer;

public class MBTankSpecialRenderer implements SpecialModelRenderer<MBTankItemRenderState> {

    @Override
    public void submit(MBTankItemRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int light, int overlay, boolean hasReflection, int layer) {

        if (state == null || state.fluidStack == null || state.fluidStack.isEmpty()) {
            return;
        }

        float fillRatio = state.fluidStack.getAmount() / (float) state.tankCapacity;

        FluidRendererUtil.submitFluid(
                poseStack,
                Sheets.translucentBlockItemSheet(),
                submitNodeCollector,
                state.fluidStack,
                fillRatio,
                light
        );
    }

    @Override
    public MBTankItemRenderState extractArgument(ItemStack itemStack) {
        MBTankItemRenderState state = new MBTankItemRenderState();

        FluidListComponent fluidList = itemStack.get(CastingDataComponents.FLUIDS.get());

        if (fluidList != null && !fluidList.fluids().isEmpty()) {
            state.fluidStack = fluidList.fluids().getFirst();

        } else {
            state.fluidStack = FluidStack.EMPTY;
        }

        state.tankCapacity = 16000;
        return state;
    }

    @Override
    public void getExtents(Consumer<Vector3fc> consumer) {
        consumer.accept(new Vector3f(0, 0, 0));
        consumer.accept(new Vector3f(1, 1, 1));
    }

    public static class Unbaked implements SpecialModelRenderer.Unbaked<MBTankItemRenderState> {
        public static final MapCodec<Unbaked> CODEC = MapCodec.unit(new Unbaked());

        @Override
        public MapCodec<? extends SpecialModelRenderer.Unbaked<MBTankItemRenderState>> type() {
            return CODEC;
        }

        @Override
        public SpecialModelRenderer<MBTankItemRenderState> bake(@NonNull BakingContext context) {
            return new MBTankSpecialRenderer();
        }
    }
}