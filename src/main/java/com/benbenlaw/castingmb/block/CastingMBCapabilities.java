package com.benbenlaw.castingmb.block;

import com.benbenlaw.casting.block.CastingBlockEntities;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

public class CastingMBCapabilities {

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {

        //MB Controller
        event.registerBlockEntity(Capabilities.Item.BLOCK, CastingMBBlockEntities.MB_CONTROLLER_BLOCK_ENTITY.get(),
                (blockEntity, side) -> blockEntity.getItemCapability());

        event.registerBlockEntity(Capabilities.Fluid.BLOCK, CastingMBBlockEntities.MB_CONTROLLER_BLOCK_ENTITY.get(),
                (blockEntity, side) -> blockEntity.getFluidCapability());

        //MB Solidifier
        event.registerBlockEntity(Capabilities.Item.BLOCK, CastingMBBlockEntities.MB_SOLIDIFIER_BLOCK_ENTITY.get(),
                (blockEntity, side) -> blockEntity.getItemCapability());

        //MB Tank
        event.registerBlockEntity(Capabilities.Fluid.BLOCK, CastingMBBlockEntities.MB_TANK_BLOCK_ENTITY.get(),
                (blockEntity, side) -> blockEntity.getFluidCapability());





    }
}
