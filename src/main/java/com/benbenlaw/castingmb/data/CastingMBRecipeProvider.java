package com.benbenlaw.castingmb.data;

import com.benbenlaw.casting.block.CastingBlocks;
import com.benbenlaw.casting.data.custom.SolidifierRecipeBuilder;
import com.benbenlaw.casting.fluid.FluidData;
import com.benbenlaw.casting.item.CastingItems;
import com.benbenlaw.castingmb.CastingMB;
import com.benbenlaw.castingmb.block.CastingMBBlocks;
import com.benbenlaw.core.tag.ResourceType;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static com.benbenlaw.casting.data.custom.FluidStackTemplateHelper.getFluidIngredient;

public class CastingMBRecipeProvider extends RecipeProvider {

    public CastingMBRecipeProvider(HolderLookup.Provider provider, RecipeOutput output) {
        super(provider, output);
    }

    public static class Runner extends RecipeProvider.Runner {
        public Runner(PackOutput packOutput, CompletableFuture<HolderLookup.Provider> provider) {
            super(packOutput, provider);
        }

        @Override
        protected @NotNull RecipeProvider createRecipeProvider(HolderLookup.@NotNull Provider provider, @NotNull RecipeOutput recipeOutput) {
            return new CastingMBRecipeProvider(provider, recipeOutput);
        }

        @Override
        public @NotNull String getName() {
            return CastingMB.MOD_ID + " Recipes";
        }
    }


    @Override
    protected void buildRecipes() {

        //Reset
        shapeless(RecipeCategory.MISC, CastingMBBlocks.MB_CONTROLLER).requires(CastingMBBlocks.MB_CONTROLLER);
        shapeless(RecipeCategory.MISC, CastingMBBlocks.MB_TANK).requires(CastingMBBlocks.MB_TANK);

        simpleSolidifierRecipe(CastingMBBlocks.MB_BLACK_BRICKS, getFluidIngredient("molten_black_brick", 1000),
                CastingBlocks.BLACK_BRICKS, "black_brick/mb_black_brick", ResourceType.STORAGE_BLOCKS, getTempFromFluid("molten_black_brick"));

        simpleSolidifierRecipe(CastingMBBlocks.MB_CONTROLLER, getFluidIngredient("molten_black_brick", 4000),
                CastingBlocks.CONTROLLER, "black_brick/mb_controller", ResourceType.STORAGE_BLOCKS, getTempFromFluid("molten_black_brick"));

        simpleSolidifierRecipe(CastingMBBlocks.MB_SOLIDIFIER, getFluidIngredient("molten_black_brick", 2000),
                CastingBlocks.SOLIDIFIER, "black_brick/mb_solidifier", ResourceType.STORAGE_BLOCKS, getTempFromFluid("molten_black_brick"));

        simpleSolidifierRecipe(CastingMBBlocks.MB_TANK, getFluidIngredient("molten_black_brick", 2000),
                CastingBlocks.TANK, "black_brick/mb_tank", ResourceType.STORAGE_BLOCKS, getTempFromFluid("molten_black_brick"));



    }

    public int getTempFromFluid(String fluidName) {
        return FluidData.FLUID_DEFINITIONS.stream().filter(data -> data.name().equals(fluidName)).findFirst().orElseThrow().fluidProduceType().temp();
    }

    public void simpleSolidifierRecipe(ItemLike block, SizedFluidIngredient fluidStack, ItemLike mold, String id, ResourceType resourceType, int temp) {
        SolidifierRecipeBuilder.solidifierRecipesBuilder(
                SizedIngredient.of(mold, 1),
                SizedIngredient.of(block.asItem(), 1),
                fluidStack,
                temp,
                Optional.of(getDurationModifier(resourceType))).save(output, id);
    }

    private double getDurationModifier(ResourceType type) {
        return switch (type) {
            case NUGGETS -> 0.2;
            case RODS, WIRES -> 0.4;
            case INGOTS, PLATES, DUSTS, GEMS -> 0.5;
            case GEARS -> 1.2;
            case STORAGE_BLOCKS -> 2.5;
            case ORES -> 1.5;
            case RAW_MATERIALS -> 1.25;
            case RAW_STORAGE_BLOCKS -> 3.0;
            default -> 1.0;
        };
    }
}
