package com.bgsoftware.superiorskyblock.menu.impl;

import com.bgsoftware.common.config.CommentedConfiguration;
import com.bgsoftware.superiorskyblock.SuperiorSkyblockPlugin;
import com.bgsoftware.superiorskyblock.api.island.Island;
import com.bgsoftware.superiorskyblock.api.island.IslandChest;
import com.bgsoftware.superiorskyblock.api.menu.ISuperiorMenu;
import com.bgsoftware.superiorskyblock.api.wrappers.SuperiorPlayer;
import com.bgsoftware.superiorskyblock.menu.PagedSuperiorMenu;
import com.bgsoftware.superiorskyblock.menu.SuperiorMenu;
import com.bgsoftware.superiorskyblock.menu.file.MenuPatternSlots;
import com.bgsoftware.superiorskyblock.utils.FileUtils;
import com.bgsoftware.superiorskyblock.utils.items.ItemBuilder;
import com.bgsoftware.superiorskyblock.wrappers.SoundWrapper;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.Arrays;
import java.util.List;

public final class MenuIslandChest extends PagedSuperiorMenu<IslandChest> {

    private static ItemBuilder validPage, invalidPage;

    private final Island island;

    private MenuIslandChest(SuperiorPlayer superiorPlayer, Island island) {
        super("menuIslandChest", superiorPlayer);
        this.island = island;
    }

    public static void init() {
        MenuIslandChest menuIslandChest = new MenuIslandChest(null, null);

        File file = new File(plugin.getDataFolder(), "menus/island-chest.yml");

        if (!file.exists())
            FileUtils.saveResource("menus/island-chest.yml");

        CommentedConfiguration cfg = CommentedConfiguration.loadConfiguration(file);

        MenuPatternSlots menuPatternSlots = FileUtils.loadGUI(menuIslandChest, "island-chest.yml", cfg);

        menuIslandChest.setPreviousSlot(getSlots(cfg, "previous-page", menuPatternSlots));
        menuIslandChest.setCurrentSlot(getSlots(cfg, "current-page", menuPatternSlots));
        menuIslandChest.setNextSlot(getSlots(cfg, "next-page", menuPatternSlots));

        {
            char slotChar = cfg.getString("slots", "").toCharArray()[0];

            List<Integer> slots = menuPatternSlots.getSlots(slotChar);
            menuIslandChest.setSlots(slots);

            ConfigurationSection validPageSection = cfg.getConfigurationSection("items." + slotChar + ".valid-page");
            ConfigurationSection invalidPageSection = cfg.getConfigurationSection("items." + slotChar + ".invalid-page");

            if (validPageSection == null)
                throw new IllegalArgumentException("The slot char " + slotChar + " is missing the valid-page section.");

            if (invalidPageSection == null)
                throw new IllegalArgumentException("The slot char " + slotChar + " is missing the invalid-page section.");

            validPage = FileUtils.getItemStack("island-chest.yml", validPageSection);
            invalidPage = FileUtils.getItemStack("island-chest.yml", invalidPageSection);

            List<String> commands = cfg.getStringList("commands." + slotChar);
            SoundWrapper sound = FileUtils.getSound(cfg.getConfigurationSection("sounds." + slotChar));
            String permission = cfg.getString("permissions." + slotChar + ".permission");
            SoundWrapper noAccessSound = FileUtils.getSound(cfg.getConfigurationSection("permissions." + slotChar + ".no-access-sound"));

            slots.forEach(i -> {
                menuIslandChest.addCommands(i, commands);
                menuIslandChest.addSound(i, sound);
                menuIslandChest.addPermission(i, permission, noAccessSound);
            });
        }

        menuIslandChest.markCompleted();
    }

    public static void openInventory(SuperiorPlayer superiorPlayer, ISuperiorMenu previousMenu, Island island) {
        MenuIslandChest menuIslandChest = new MenuIslandChest(superiorPlayer, island);
        if (plugin.getSettings().isSkipOneItemMenus() && island.getChest().length == 1) {
            menuIslandChest.onPlayerClick(null, island.getChest()[0]);
        } else {
            menuIslandChest.open(previousMenu);
        }
    }

    public static void refreshMenus(Island island) {
        SuperiorMenu.refreshMenus(MenuIslandChest.class, superiorMenu -> superiorMenu.island.equals(island));
    }

    @Override
    protected void onPlayerClick(InventoryClickEvent event, IslandChest islandChest) {
        previousMove = false;
        islandChest.openChest(superiorPlayer);
    }

    @Override
    protected ItemStack getObjectItem(ItemStack clickedItem, IslandChest islandChest) {
        try {
            return validPage.clone()
                    .replaceAll("{0}", (islandChest.getIndex() + 1) + "")
                    .replaceAll("{1}", (islandChest.getRows() * 9) + "")
                    .build(superiorPlayer);
        } catch (Exception ex) {
            ex.printStackTrace();
            SuperiorSkyblockPlugin.debug(ex);
            return getNullItem();
        }
    }

    @Override
    protected ItemStack getNullItem() {
        return invalidPage.clone().build();
    }

    @Override
    protected List<IslandChest> requestObjects() {
        return Arrays.asList(island.getChest());
    }

    @Override
    public void cloneAndOpen(ISuperiorMenu previousMenu) {
        openInventory(superiorPlayer, previousMenu, island);
    }

}
