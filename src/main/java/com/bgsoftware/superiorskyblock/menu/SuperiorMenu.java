package com.bgsoftware.superiorskyblock.menu;

import com.bgsoftware.common.reflection.ReflectField;
import com.bgsoftware.superiorskyblock.Locale;
import com.bgsoftware.superiorskyblock.SuperiorSkyblockPlugin;
import com.bgsoftware.superiorskyblock.api.menu.ISuperiorMenu;
import com.bgsoftware.superiorskyblock.api.objects.Pair;
import com.bgsoftware.superiorskyblock.api.wrappers.SuperiorPlayer;
import com.bgsoftware.superiorskyblock.hooks.support.PlaceholderHook;
import com.bgsoftware.superiorskyblock.menu.file.MenuPatternSlots;
import com.bgsoftware.superiorskyblock.utils.items.ItemBuilder;
import com.bgsoftware.superiorskyblock.utils.threads.Executor;
import com.bgsoftware.superiorskyblock.wrappers.SoundWrapper;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressWarnings({"unused", "WeakerAccess"})
public abstract class SuperiorMenu implements ISuperiorMenu {

    public static final char[] itemChars = new char[]{
            '!', '@', '#', '$', '%', '^', '&', '*', '-', '_', '+', '=',
            '~', '1', '2', '3', '4', '5', '6', '7', '8', '9', '0', '>',
            '<', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K',
            'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W',
            'X', 'Y', 'Z'
    };
    protected static final String[] MENU_IGNORED_SECTIONS = new String[]{
            "items", "sounds", "commands", "back"
    };
    protected static final SuperiorSkyblockPlugin plugin = SuperiorSkyblockPlugin.getPlugin();
    protected static final Map<String, MenuData> dataMap = new HashMap<>();
    private static final Pattern COMMAND_PATTERN_ARGS = Pattern.compile("\\[(.+)](.+)");
    private static final Pattern COMMAND_PATTERN = Pattern.compile("\\[(.+)]");
    private static final ReflectField<Object> INVENTORY =
            new ReflectField<>("org.bukkit.craftbukkit.VERSION.inventory.CraftInventory", Object.class, "inventory");
    protected final SuperiorPlayer superiorPlayer;
    private final String identifier;
    protected SuperiorPlayer targetPlayer = null;

    protected ISuperiorMenu previousMenu;
    protected boolean previousMove = true, closeButton = false, nextMove = false;
    private boolean refreshing = false;

    public SuperiorMenu(String identifier, SuperiorPlayer superiorPlayer) {
        this.identifier = identifier;
        this.superiorPlayer = superiorPlayer;
    }

    protected static String[] additionalMenuSections(String... ignoredSections) {
        String[] sections = Arrays.copyOf(MENU_IGNORED_SECTIONS, MENU_IGNORED_SECTIONS.length + ignoredSections.length);
        System.arraycopy(ignoredSections, 0, sections, MENU_IGNORED_SECTIONS.length, ignoredSections.length);
        return sections;
    }

    public static void killMenu(SuperiorPlayer superiorPlayer) {
        superiorPlayer.runIfOnline(player -> {
            Inventory inventory = player.getOpenInventory().getTopInventory();
            InventoryHolder inventoryHolder = inventory == null ? null : inventory.getHolder();
            if (inventoryHolder instanceof SuperiorMenu)
                ((SuperiorMenu) inventoryHolder).previousMove = false;

            player.closeInventory();
        });
    }

    protected static <T extends SuperiorMenu> void refreshMenus(Class<T> menuClazz, Predicate<T> predicate) {
        runActionOnMenus(menuClazz, predicate, ((player, superiorMenu) -> {
            superiorMenu.previousMove = false;
            superiorMenu.open(superiorMenu.previousMenu);
        }));
    }

    protected static <T extends SuperiorMenu> void destroyMenus(Class<T> menuClazz) {
        destroyMenus(menuClazz, superiorMenu -> true);
    }

    protected static <T extends SuperiorMenu> void destroyMenus(Class<T> menuClazz, Predicate<T> predicate) {
        runActionOnMenus(menuClazz, predicate, ((player, superiorMenu) -> player.closeInventory()));
    }

    private static <T extends SuperiorMenu> void runActionOnMenus(Class<T> menuClazz, Predicate<T> predicate, BiConsumer<Player, SuperiorMenu> callback) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            try {
                InventoryHolder inventoryHolder = player.getOpenInventory().getTopInventory().getHolder();
                //noinspection unchecked
                if (menuClazz.isInstance(inventoryHolder) && predicate.test((T) inventoryHolder)) {
                    SuperiorMenu superiorMenu = (SuperiorMenu) inventoryHolder;
                    callback.accept(player, superiorMenu);
                }
            } catch (Exception error) {
                SuperiorSkyblockPlugin.debug(error);
            }
        }
    }

    protected static List<Integer> getSlots(ConfigurationSection section, String key, MenuPatternSlots menuPatternSlots) {
        return !section.contains(key) ? Collections.singletonList(-1) :
                menuPatternSlots.getSlots(section.getString(key), -1);
    }

    protected void updateTargetPlayer(SuperiorPlayer targetPlayer) {
        this.targetPlayer = targetPlayer;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void addSound(int slot, SoundWrapper sound) {
        if (sound != null)
            getData().sounds.put(slot, sound);
    }

    public void addCommands(int slot, List<String> commands) {
        if (commands != null && !commands.isEmpty())
            getData().commands.put(slot, commands);
    }

    public void addPermission(int slot, String permission, SoundWrapper noAccessSound) {
        if (permission != null && !permission.isEmpty())
            getData().permissions.put(slot, new Pair<>(permission, noAccessSound));
    }

    public void addFillItem(int slot, ItemBuilder itemBuilder) {
        if (itemBuilder != null)
            getData().fillItems.put(slot, itemBuilder);
    }

    public ItemBuilder getFillItem(int slot) {
        return getData().fillItems.get(slot);
    }

    public void setBackButton(int slot) {
        addData("backSlot", slot);
    }

    public void resetData() {
        dataMap.put(identifier, new MenuData());
    }

    public void setTitle(String title) {
        getData().title = title;
    }

    public int getRowsSize() {
        return getData().rowsSize;
    }

    public void setRowsSize(int rowsSize) {
        getData().rowsSize = rowsSize;
    }

    public void setOpeningSound(SoundWrapper openingSound) {
        getData().openingSound = openingSound;
    }

    public void setInventoryType(InventoryType inventoryType) {
        getData().inventoryType = inventoryType;
    }

    public void addData(String key, Object value) {
        getData().data.put(key, value);
    }

    public Object getData(String key) {
        return getData(key, null);
    }

    public Object getData(String key, Object def) {
        return getData().data.getOrDefault(key, def);
    }

    public boolean containsData(String key) {
        return getData().data.containsKey(key);
    }

    public void setPreviousMoveAllowed(boolean isPreviousMoveAllowed) {
        addData("previous-menu", isPreviousMoveAllowed);
    }

    @Override
    public Inventory getInventory() {
        return buildInventory(null);
    }

    @Override
    public void setPreviousMove(boolean previousMove) {
        this.previousMove = previousMove;
    }

    public abstract void cloneAndOpen(ISuperiorMenu previousMenu);

    @Nullable
    @Override
    public ISuperiorMenu getPreviousMenu() {
        return this.previousMenu;
    }

    public final void onClick(InventoryClickEvent e) {
        if (refreshing)
            return;

        if (e.getCurrentItem() != null) {
            SoundWrapper sound = getSound(e.getRawSlot());
            if (sound != null)
                sound.playSound(e.getWhoClicked());

            List<String> commands = getCommands(e.getRawSlot());
            if (commands != null)
                commands.forEach(command -> runCommand(command, e, Bukkit.getConsoleSender()));

            Pair<String, SoundWrapper> permission = getPermission(e.getRawSlot());
            if (permission != null && !superiorPlayer.hasPermission(permission.getKey())) {
                if (permission.getValue() != null)
                    permission.getValue().playSound(e.getWhoClicked());

                return;
            }

        }

        if (e.getRawSlot() == getBackSlot()) {
            closeButton = true;
            e.getWhoClicked().closeInventory();
        }

        SuperiorSkyblockPlugin.debug("Action: Menu Click, Target: " + superiorPlayer.getName() + ", Item: " +
                (e.getCurrentItem() == null ? "AIR" : e.getCurrentItem().getType()) + ", Slot: " + e.getRawSlot());

        onPlayerClick(e);
    }

    private void runCommand(String command, InventoryClickEvent e, CommandSender sender) {
        Matcher matcher = COMMAND_PATTERN_ARGS.matcher(command);

        if (matcher.matches()) {
            String subCommand = matcher.group(1), args = matcher.group(2).trim();
            handleSubCommand(subCommand, args, e, sender);
        } else if ((matcher = COMMAND_PATTERN.matcher(command)).matches()) {
            String subCommand = matcher.group(1);
            handleSubCommand(subCommand, "", e, sender);
        } else if (command.equalsIgnoreCase("close")) {
            closeButton = true;
            previousMove = false;
            e.getWhoClicked().closeInventory();
        } else if (command.equalsIgnoreCase("back")) {
            closeButton = true;
            e.getWhoClicked().closeInventory();
        } else {
            if (targetPlayer != null)
                command = PlaceholderHook.parse(targetPlayer, command);
            else if (sender instanceof Player)
                command = PlaceholderHook.parse(plugin.getPlayers().getSuperiorPlayer(sender), command);

            Bukkit.dispatchCommand(sender instanceof Player || command.startsWith("PLAYER:") ? e.getWhoClicked() : Bukkit.getConsoleSender(),
                    command.replace("PLAYER:", "").replace("%player%", e.getWhoClicked().getName()));
        }
    }

    private void handleSubCommand(String subCommand, String args, InventoryClickEvent e, CommandSender sender) {
        switch (subCommand.toLowerCase()) {
            case "player":
                runCommand(args, e, e.getWhoClicked());
                break;
            case "admin":
                String commandLabel = plugin.getSettings().getIslandCommand().split(",")[0];
                runCommand(commandLabel + " admin " + args, e, sender);
                break;
            case "close":
                closeButton = true;
                previousMove = false;
                e.getWhoClicked().closeInventory();
                break;
            case "back":
                closeButton = true;
                e.getWhoClicked().closeInventory();
                break;
            default:
                plugin.getCommands().dispatchSubCommand(sender, subCommand, args);
                break;
        }
    }

    protected abstract void onPlayerClick(InventoryClickEvent e);

    public void open(ISuperiorMenu previousMenu) {
        if (Bukkit.isPrimaryThread()) {
            Executor.async(() -> open(previousMenu));
            return;
        }

        Player player = superiorPlayer.asPlayer();

        if (player == null)
            return;

        if (player.isSleeping()) {
            Locale.OPEN_MENU_WHILE_SLEEPING.send(superiorPlayer);
            return;
        }

        SuperiorSkyblockPlugin.debug("Action: Open Menu, Target: " + superiorPlayer.getName() + ", Menu: " + identifier);

        if (!(this instanceof SuperiorMenuBlank) && !isCompleted()) {
            SuperiorMenuBlank.openInventory(superiorPlayer, previousMenu);
            return;
        }

        Inventory inventory;

        try {
            inventory = getInventory();
        } catch (Exception ex) {
            if (!(this instanceof SuperiorMenuBlank)) {
                addData("completed", false);
                SuperiorMenuBlank.openInventory(superiorPlayer, previousMenu);
            }

            SuperiorSkyblockPlugin.debug(ex);
            ex.printStackTrace();
            return;
        }

        Executor.sync(() -> {
            if (!superiorPlayer.isOnline())
                return;

            SuperiorMenu currentMenu = null;
            InventoryHolder inventoryHolder = player.getOpenInventory().getTopInventory().getHolder();
            if (inventoryHolder instanceof SuperiorMenu) {
                currentMenu = (SuperiorMenu) inventoryHolder;
                currentMenu.nextMove = true;
            }

            if (Arrays.equals(player.getOpenInventory().getTopInventory().getContents(), inventory.getContents()))
                return;

            if (previousMenu != null)
                previousMenu.setPreviousMove(false);

            player.openInventory(inventory);

            SoundWrapper openingSound = getData().openingSound;

            if (openingSound != null)
                openingSound.playSound(player);

            refreshing = false;

            this.previousMenu = previousMenu != null ? previousMenu : previousMove ? currentMenu : null;
        });
    }

    public void closeInventory(SuperiorPlayer superiorPlayer) {
        Executor.sync(() -> {
            if (!nextMove && !closeButton && plugin.getSettings().isOnlyBackButton()) {
                open(previousMenu);
            } else if (previousMenu != null && (boolean) getData("previous-menu", true)) {
                if (previousMove)
                    previousMenu.cloneAndOpen(previousMenu.getPreviousMenu());
                else
                    previousMove = true;
            }

            closeButton = false;
            nextMove = false;
        });
    }

    protected Inventory buildInventory(Function<String, String> titleReplacer) {
        MenuData menuData = getData();
        Inventory inventory;

        String title = menuData.title;
        if (titleReplacer != null)
            title = titleReplacer.apply(title);

        //placeholder with titleReplacer
        title = PlaceholderHook.parse(superiorPlayer, title);

        if (menuData.inventoryType != InventoryType.CHEST) {
            inventory = Bukkit.createInventory(this, menuData.inventoryType, title);
        } else {
            inventory = Bukkit.createInventory(this, menuData.rowsSize * 9, title);
        }

        if (inventory.getHolder() == null)
            INVENTORY.set(inventory, plugin.getNMSAlgorithms().getCustomHolder(menuData.inventoryType, this, title));

        //noinspection all
        List<Integer> slots = containsData("slots") ? (List<Integer>) getData("slots") : new ArrayList<>();

        for (Map.Entry<Integer, ItemBuilder> itemStackEntry : menuData.fillItems.entrySet()) {
            ItemBuilder itemBuilder = itemStackEntry.getValue().clone();
            if (itemStackEntry.getKey() >= 0)
                inventory.setItem(itemStackEntry.getKey(), slots.contains(itemStackEntry.getKey()) ?
                        itemBuilder.build() : itemBuilder.build(targetPlayer == null ? superiorPlayer : targetPlayer));
        }

        return inventory;
    }

    private SoundWrapper getSound(int slot) {
        return getData().sounds.get(slot);
    }

    private List<String> getCommands(int slot) {
        return getData().commands.get(slot);
    }

    private Pair<String, SoundWrapper> getPermission(int slot) {
        return getData().permissions.get(slot);
    }

    private MenuData getData() {
        if (!dataMap.containsKey(identifier)) {
            dataMap.put(identifier, new MenuData());
        }

        return dataMap.get(identifier);
    }

    private int getBackSlot() {
        return (Integer) getData("backSlot");
    }

    protected void markCompleted() {
        addData("completed", true);
    }

    protected boolean isCompleted() {
        return (Boolean) getData("completed", false);
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof SuperiorMenu && ((SuperiorMenu) obj).getIdentifier().equals(getIdentifier());
    }

    protected static class MenuData {

        private final Map<Integer, SoundWrapper> sounds = new HashMap<>();
        private final Map<Integer, List<String>> commands = new HashMap<>();
        private final Map<Integer, Pair<String, SoundWrapper>> permissions = new HashMap<>();
        private final Map<Integer, ItemBuilder> fillItems = new HashMap<>();
        private final Map<String, Object> data = new HashMap<>();
        private String title = "";
        private InventoryType inventoryType = InventoryType.CHEST;
        private int rowsSize = 6;
        private SoundWrapper openingSound = null;

    }

}
