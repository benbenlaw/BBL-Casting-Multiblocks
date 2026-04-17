package com.benbenlaw.castingmb;

import com.benbenlaw.casting.Casting;
import com.benbenlaw.casting.block.CastingBlockEntities;
import com.benbenlaw.casting.block.CastingBlocks;
import com.benbenlaw.casting.block.CastingCapabilities;
import com.benbenlaw.casting.block.entity.renderer.TankBlockEntityRenderer;
import com.benbenlaw.casting.config.CastingConfig;
import com.benbenlaw.casting.fluid.CastingFluids;
import com.benbenlaw.casting.item.CastingCreativeModeTab;
import com.benbenlaw.casting.item.CastingDataComponents;
import com.benbenlaw.casting.item.CastingItems;
import com.benbenlaw.casting.recipe.CastingRecipeTypes;
import com.benbenlaw.casting.screen.CastingMenuTypes;
import com.benbenlaw.casting.screen.ControllerScreen;
import com.benbenlaw.casting.screen.MixerScreen;
import com.benbenlaw.casting.screen.SolidifierScreen;
import com.benbenlaw.castingmb.block.CastingMBBlockEntities;
import com.benbenlaw.castingmb.block.CastingMBBlocks;
import com.benbenlaw.castingmb.block.CastingMBCapabilities;
import com.benbenlaw.castingmb.block.entity.renderer.MBControllerBlockEntityRenderer;
import com.benbenlaw.castingmb.block.entity.renderer.MBTankBlockEntityRenderer;
import com.benbenlaw.castingmb.block.entity.renderer.MBTankSpecialRenderer;
import com.benbenlaw.castingmb.item.CastingMBCreativeModeTab;
import com.benbenlaw.castingmb.item.CastingMBItems;
import com.benbenlaw.castingmb.network.CastingMBNetworking;
import com.benbenlaw.castingmb.screen.CastingMBMenuTypes;
import com.benbenlaw.castingmb.screen.MBControllerScreen;
import com.benbenlaw.castingmb.screen.MBSolidifierScreen;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RegisterSpecialModelRendererEvent;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

@Mod(CastingMB.MOD_ID)
public class CastingMB {
    public static final String MOD_ID = "castingmb";

    public CastingMB(IEventBus modEventBus, final ModContainer modContainer) {

        CastingMBBlocks.BLOCKS.register(modEventBus);
        CastingMBBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        CastingMBItems.ITEMS.register(modEventBus);
        CastingMBCreativeModeTab.CREATIVE_MODE_TABS.register(modEventBus);
        CastingMBMenuTypes.MENUS.register(modEventBus);

        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::registerCapabilities);
    }

    public void registerCapabilities(RegisterCapabilitiesEvent event) {
        CastingMBCapabilities.registerCapabilities(event);
    }

    public void commonSetup(RegisterPayloadHandlersEvent event) {
        CastingMBNetworking.registerNetworking(event);
    }

    @EventBusSubscriber(modid = CastingMB.MOD_ID, value = Dist.CLIENT)
    public static class ClientModEvents {

        @SubscribeEvent
        public static void registerSpecialModel(RegisterSpecialModelRendererEvent event) {
            event.register(CastingMB.identifier("block/mb_tank_fluid"), MBTankSpecialRenderer.Unbaked.CODEC);
        }

        @SubscribeEvent
        public static void registerRenderers(final EntityRenderersEvent.RegisterRenderers event) {
            event.registerBlockEntityRenderer(CastingMBBlockEntities.MB_TANK_BLOCK_ENTITY.get(), MBTankBlockEntityRenderer::new);
            event.registerBlockEntityRenderer(CastingMBBlockEntities.MB_CONTROLLER_BLOCK_ENTITY.get(), MBControllerBlockEntityRenderer::new);
        }

        @SubscribeEvent
        public static void registerScreens(RegisterMenuScreensEvent event) {
            event.register(CastingMBMenuTypes.MB_CONTROLLER_MENU.get(), MBControllerScreen::new);
            event.register(CastingMBMenuTypes.MB_SOLIDIFIER_MENU.get(), MBSolidifierScreen::new);
        }

        /*
        @SubscribeEvent
        public static void onClientExtensions(RegisterClientExtensionsEvent event) {
            CastingFluids.FLUIDS_MAP.values().forEach(fluid -> {
                var fluidType = fluid.getFluidType();
                var extensions = IClientFluidTypeExtensions.of(fluidType);
                event.registerFluidType(extensions, fluidType);
            });
        }

        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {

            event.enqueueWork(() -> {
                // Only Needed if i need a translucent fluid otherwise should be ok? //
                //     ItemBlockRenderTypes.setRenderLayer(ModFluids.MOLTEN_URANIUM_FLOWING.get(), RenderType.translucent());

            });
        }

        @SubscribeEvent
        public static void onKeyInput(RegisterKeyMappingsEvent event) {
            //event.register(KeyBinds.HELMET_HOTKEY);
            //event.register(KeyBinds.CHESTPLATE_HOTKEY);
            //event.register(KeyBinds.LEGGINGS_HOTKEY);
            //event.register(KeyBinds.BOOTS_HOTKEY);
        }
    }

 */

    }
    public static Identifier identifier(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}
