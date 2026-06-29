package com.benbenlaw.castingmb.recipe;

import com.benbenlaw.castingmb.CastingMB;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class CastingMBRecipeTypes {

    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZER =
            DeferredRegister.create(BuiltInRegistries.RECIPE_SERIALIZER, CastingMB.MOD_ID);
    public static final DeferredRegister<RecipeType<?>> TYPES =
            DeferredRegister.create(BuiltInRegistries.RECIPE_TYPE, CastingMB.MOD_ID);


    //Entity Melting
    public static final Supplier<RecipeSerializer<EntityMeltingRecipe>> ENTITY_MELTING_SERIALIZER =
            SERIALIZER.register("entity_melting", () -> EntityMeltingRecipe.SERIALIZER);
    public static final Supplier<RecipeType<EntityMeltingRecipe>> ENTITY_MELTING_TYPE =
            TYPES.register("entity_melting", () -> EntityMeltingRecipe.TYPE);



}