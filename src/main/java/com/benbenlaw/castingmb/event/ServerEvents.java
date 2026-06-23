package com.benbenlaw.castingmb.event;

import com.benbenlaw.castingmb.CastingMB;
import com.benbenlaw.castingmb.block.CastingMBBlockEntities;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

@EventBusSubscriber(modid = CastingMB.MOD_ID)
public class ServerEvents {


    @SubscribeEvent
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {

        //MB Controller
        event.registerBlockEntity(Capabilities.Item.BLOCK, CastingMBBlockEntities.MB_CONTROLLER_BLOCK_ENTITY.get(),
                (blockEntity, side) -> blockEntity.getItemHandler());

        event.registerBlockEntity(Capabilities.Fluid.BLOCK, CastingMBBlockEntities.MB_CONTROLLER_BLOCK_ENTITY.get(),
                (blockEntity, side) -> blockEntity.getFluidHandler());

        //MB Solidifier
        event.registerBlockEntity(Capabilities.Item.BLOCK, CastingMBBlockEntities.MB_SOLIDIFIER_BLOCK_ENTITY.get(),
                (blockEntity, side) -> blockEntity.getItemHandler());

        //MB Tank
        event.registerBlockEntity(Capabilities.Fluid.BLOCK, CastingMBBlockEntities.MB_TANK_BLOCK_ENTITY.get(),
                (blockEntity, side) -> blockEntity.getFluidHandler());





    }
}
