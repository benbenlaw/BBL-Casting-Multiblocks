package com.benbenlaw.castingmb.event.client;

import com.benbenlaw.castingmb.recipe.EntityMeltingRecipe;
import net.minecraft.resources.Identifier;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class ClientRecipeCache {

    //Entity Melting
    public static Map<Identifier, EntityMeltingRecipe> entityMeltingRecipe = new HashMap<>();

    public static void setCachedEntityMeltingRecipes(Map<Identifier, EntityMeltingRecipe> recipes) {
        entityMeltingRecipe = recipes;
    }

    public static Collection<EntityMeltingRecipe> getCachedEntityMeltingRecipes() {
        return entityMeltingRecipe.values();
    }
}