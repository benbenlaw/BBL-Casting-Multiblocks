package com.benbenlaw.castingmb.data;

import com.google.gson.JsonObject;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

public class FusionConnectedTextureOverridesProvider implements DataProvider {

    private final PackOutput output;

    public FusionConnectedTextureOverridesProvider(PackOutput output) {
        this.output = output;
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cachedOutput) {
        Path root = this.output.getOutputFolder().resolve("fusion-overrides");

        CompletableFuture<?> modelFuture = DataProvider.saveStable(cachedOutput, buildModelJson(),
                root.resolve("assets/castingmb/models/block/mb_black_brick_glass.json"));

        CompletableFuture<?> metaFuture = DataProvider.saveStable(cachedOutput, buildTextureMetaJson(),
                root.resolve("assets/castingmb/textures/block/mb_black_brick_glass_connected.png.mcmeta"));

        return CompletableFuture.allOf(modelFuture, metaFuture);
    }

    private JsonObject buildModelJson() {
        JsonObject model = new JsonObject();
        model.addProperty("loader", "fusion:model");
        model.addProperty("type", "connecting");
        model.addProperty("parent", "minecraft:block/cube_all");

        JsonObject textures = new JsonObject();
        textures.addProperty("all", "castingmb:block/mb_black_brick_glass_connected");
        textures.addProperty("particle", "castingmb:block/mb_black_brick_glass");
        model.add("textures", textures);

        JsonObject connections = new JsonObject();
        connections.addProperty("type", "is_same_block");
        model.add("connections", connections);

        return model;
    }

    private JsonObject buildTextureMetaJson() {
        JsonObject root = new JsonObject();
        JsonObject fusion = new JsonObject();
        fusion.addProperty("type", "connecting");
        fusion.addProperty("layout", "pieced");
        root.add("fusion", fusion);
        return root;
    }

    @Override
    public String getName() {
        return "Fusion Connected Texture Overrides";
    }
}