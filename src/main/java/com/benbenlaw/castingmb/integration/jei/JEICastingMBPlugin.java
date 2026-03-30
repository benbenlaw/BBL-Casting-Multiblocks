package com.benbenlaw.castingmb.integration.jei;

import com.benbenlaw.casting.Casting;
import com.benbenlaw.casting.screen.SolidifierScreen;
import com.benbenlaw.castingmb.CastingMB;
import com.benbenlaw.castingmb.screen.MBSolidifierScreen;
import com.benbenlaw.core.integration.jei.GhostFilter;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

@JeiPlugin
public class JEICastingMBPlugin implements IModPlugin {

    @Override
    public @NotNull Identifier getPluginUid() {
        return CastingMB.identifier("jei_plugin");
    }

    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addGhostIngredientHandler(MBSolidifierScreen.class, new GhostFilter<>());
    }
}
