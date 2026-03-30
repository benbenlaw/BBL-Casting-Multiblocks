package com.benbenlaw.castingmb.data;


import com.benbenlaw.casting.Casting;
import com.benbenlaw.casting.data.*;
import com.benbenlaw.casting.data.CastingBlockTags;
import com.benbenlaw.castingmb.CastingMB;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@EventBusSubscriber(modid = CastingMB.MOD_ID)
public class CastingMBDataGenerators {

    @SubscribeEvent
    public static void gatherData(GatherDataEvent.Client event) {

        DataGenerator generator = event.getGenerator();
        PackOutput packOutput = generator.getPackOutput();
        CompletableFuture<HolderLookup.Provider> lookupProvider = event.getLookupProvider();

        generator.addProvider(true, new CastingMBBlockTags(packOutput, lookupProvider));
        generator.addProvider(true, new CastingMBModelProvider(packOutput));

        generator.addProvider(true, new CastingMBItemTags(packOutput, lookupProvider));
        generator.addProvider(true, new CastingMBLangProvider(packOutput));
        generator.addProvider(true, new LootTableProvider(packOutput, Collections.emptySet(),
                List.of(new LootTableProvider.SubProviderEntry(CastingMBLootTableProvider::new, LootContextParamSets.BLOCK)), lookupProvider));
        generator.addProvider(true, new CastingModelProvider(packOutput));

        //Recipes
        generator.addProvider(true, new CastingMBRecipeProvider.Runner(packOutput, lookupProvider));

    }
}
