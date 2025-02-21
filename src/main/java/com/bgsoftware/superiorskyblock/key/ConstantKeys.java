package com.bgsoftware.superiorskyblock.key;

import com.bgsoftware.superiorskyblock.utils.legacy.Materials;

public final class ConstantKeys {

    public static final Key HOPPER = Key.of("HOPPER");
    public static final Key WATER = Key.of("WATER");
    public static final Key LAVA = Key.of("LAVA");
    public static final Key DRAGON_EGG = Key.of("DRAGON_EGG");
    public static final Key OBSIDIAN = Key.of("OBSIDIAN");
    public static final Key COMMAND_BLOCK = Key.of("COMMAND_BLOCK");
    public static final Key COMMAND = Key.of("COMMAND");
    public static final Key TNT = Key.of("TNT");
    public static final Key FURNACE = Key.of("FURNACE");
    public static final Key CHEST = Key.of("CHEST");
    public static final Key AIR = Key.of("AIR");
    public static final Key CAULDRON = Key.of("CAULDRON");
    public static final Key EGG_MOB_SPAWNER = Key.of(Materials.SPAWNER.toBukkitType() + ":EGG");
    public static final Key MOB_SPAWNER = Key.of(Materials.SPAWNER.toBukkitType().name());
    public static final Key END_PORTAL_FRAME_WITH_EYE = Key.of(Materials.END_PORTAL_FRAME.toBukkitType(), (short) 7);
    public static final Key END_PORTAL_FRAME = Key.of(Materials.END_PORTAL_FRAME.toBukkitType().name());
    public static final Key WET_SPONGE = Key.of("WET_SPONGE");
    private ConstantKeys() {

    }

}
