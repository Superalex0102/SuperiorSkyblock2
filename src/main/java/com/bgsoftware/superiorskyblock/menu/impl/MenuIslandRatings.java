package com.bgsoftware.superiorskyblock.menu.impl;

import com.bgsoftware.common.config.CommentedConfiguration;
import com.bgsoftware.superiorskyblock.SuperiorSkyblockPlugin;
import com.bgsoftware.superiorskyblock.api.enums.Rating;
import com.bgsoftware.superiorskyblock.api.island.Island;
import com.bgsoftware.superiorskyblock.api.menu.ISuperiorMenu;
import com.bgsoftware.superiorskyblock.api.wrappers.SuperiorPlayer;
import com.bgsoftware.superiorskyblock.menu.PagedMappedSuperiorMenu;
import com.bgsoftware.superiorskyblock.menu.SuperiorMenu;
import com.bgsoftware.superiorskyblock.menu.converter.MenuConverter;
import com.bgsoftware.superiorskyblock.menu.file.MenuPatternSlots;
import com.bgsoftware.superiorskyblock.utils.FileUtils;
import com.bgsoftware.superiorskyblock.utils.StringUtils;
import com.bgsoftware.superiorskyblock.utils.items.ItemBuilder;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;

public final class MenuIslandRatings extends PagedMappedSuperiorMenu<UUID, Rating> {

    private final Island island;

    private MenuIslandRatings(SuperiorPlayer superiorPlayer, Island island) {
        super("menuIslandRatings", superiorPlayer);
        this.island = island;
    }

    public static void init() {
        MenuIslandRatings menuIslandRatings = new MenuIslandRatings(null, null);

        File file = new File(plugin.getDataFolder(), "menus/island-ratings.yml");

        if (!file.exists())
            FileUtils.saveResource("menus/island-ratings.yml");

        CommentedConfiguration cfg = CommentedConfiguration.loadConfiguration(file);

        if (convertOldGUI(cfg)) {
            try {
                cfg.save(file);
            } catch (Exception ex) {
                ex.printStackTrace();
                SuperiorSkyblockPlugin.debug(ex);
            }
        }

        MenuPatternSlots menuPatternSlots = FileUtils.loadGUI(menuIslandRatings, "island-ratings.yml", cfg);

        menuIslandRatings.setPreviousSlot(getSlots(cfg, "previous-page", menuPatternSlots));
        menuIslandRatings.setCurrentSlot(getSlots(cfg, "current-page", menuPatternSlots));
        menuIslandRatings.setNextSlot(getSlots(cfg, "next-page", menuPatternSlots));
        menuIslandRatings.setSlots(getSlots(cfg, "slots", menuPatternSlots));

        menuIslandRatings.markCompleted();
    }

    public static void openInventory(SuperiorPlayer superiorPlayer, ISuperiorMenu previousMenu, Island island) {
        new MenuIslandRatings(superiorPlayer, island).open(previousMenu);
    }

    public static void refreshMenus(Island island) {
        SuperiorMenu.refreshMenus(MenuIslandRatings.class, superiorMenu -> superiorMenu.island.equals(island));
    }

    private static boolean convertOldGUI(YamlConfiguration newMenu) {
        File oldFile = new File(plugin.getDataFolder(), "guis/ratings-gui.yml");

        if (!oldFile.exists())
            return false;

        //We want to reset the items of newMenu.
        ConfigurationSection itemsSection = newMenu.createSection("items");
        ConfigurationSection soundsSection = newMenu.createSection("sounds");
        ConfigurationSection commandsSection = newMenu.createSection("commands");

        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(oldFile);

        newMenu.set("title", cfg.getString("ratings-gui.title"));

        int size = cfg.getInt("ratings-gui.size");

        char[] patternChars = new char[size * 9];
        Arrays.fill(patternChars, '\n');

        int charCounter = 0;

        if (cfg.contains("ratings-gui.fill-items")) {
            charCounter = MenuConverter.convertFillItems(cfg.getConfigurationSection("ratings-gui.fill-items"),
                    charCounter, patternChars, itemsSection, commandsSection, soundsSection);
        }

        char slotsChar = itemChars[charCounter++];

        MenuConverter.convertPagedButtons(cfg.getConfigurationSection("ratings-gui"),
                cfg.getConfigurationSection("ratings-gui.rate-item"), newMenu, patternChars,
                slotsChar, itemChars[charCounter++], itemChars[charCounter++], itemChars[charCounter++],
                itemsSection, commandsSection, soundsSection);

        newMenu.set("pattern", MenuConverter.buildPattern(size, patternChars, itemChars[charCounter]));

        return true;
    }

    @Override
    protected void onPlayerClick(InventoryClickEvent event, UUID clickedObjectKey, Rating clickedObjectValue) {

    }

    @Override
    protected ItemStack getObjectItem(ItemStack clickedItem, UUID uuid, Rating rating) {
        SuperiorPlayer superiorPlayer = plugin.getPlayers().getSuperiorPlayer(uuid);
        try {
            return new ItemBuilder(clickedItem)
                    .replaceAll("{0}", superiorPlayer.getName())
                    .replaceAll("{1}", StringUtils.formatRating(superiorPlayer.getUserLocale(), rating.getValue()))
                    .asSkullOf(superiorPlayer).build(superiorPlayer);
        } catch (Exception ex) {
            SuperiorSkyblockPlugin.log("Failed to load menu because of player: " + superiorPlayer.getName());
            SuperiorSkyblockPlugin.debug(ex);
            throw ex;
        }
    }

    @Override
    protected Map<UUID, Rating> requestMappedObjects() {
        return island.getRatings();
    }

    @Override
    public void cloneAndOpen(ISuperiorMenu previousMenu) {
        openInventory(superiorPlayer, previousMenu, island);
    }

}
