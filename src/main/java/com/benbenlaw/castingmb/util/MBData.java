package com.benbenlaw.castingmb.util;

import com.benbenlaw.core.multiblock.MultiblockData;
import com.mojang.datafixers.util.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.storage.ValueInput;

import java.util.ArrayList;
import java.util.HashSet;

public class MBData {

    public static MultiblockData from(ValueInput input) {
        MultiblockData data = new MultiblockData(
                BlockPos.ZERO,
                Pair.of(BlockPos.ZERO, BlockPos.ZERO),
                new ArrayList<>(),
                new HashSet<>(),
                0,
                0
        );
        data.deserialize(input);
        return data;
    }
}
