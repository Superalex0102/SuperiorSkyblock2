package com.bgsoftware.superiorskyblock.island.warps;

import com.bgsoftware.superiorskyblock.SuperiorSkyblockPlugin;
import com.bgsoftware.superiorskyblock.api.island.Island;
import com.bgsoftware.superiorskyblock.api.island.warps.IslandWarp;
import com.bgsoftware.superiorskyblock.api.island.warps.WarpCategory;
import com.bgsoftware.superiorskyblock.api.wrappers.SuperiorPlayer;
import com.bgsoftware.superiorskyblock.database.bridge.IslandsDatabaseBridge;
import com.bgsoftware.superiorskyblock.utils.items.ItemBuilder;
import com.bgsoftware.superiorskyblock.wrappers.SBlockPosition;
import com.google.common.base.Preconditions;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

import java.util.Objects;

public final class SIslandWarp implements IslandWarp {

    private static final SuperiorSkyblockPlugin plugin = SuperiorSkyblockPlugin.getPlugin();

    public static ItemBuilder DEFAULT_WARP_ICON;

    private final WarpCategory warpCategory;

    private String name;
    private Location location;
    private boolean privateFlag;
    private ItemStack icon;

    public SIslandWarp(String name, Location location, WarpCategory warpCategory) {
        this.name = name;
        this.location = new Location(location.getWorld(), location.getBlockX() + 0.5, location.getBlockY(),
                location.getBlockZ() + 0.5, location.getYaw(), location.getPitch());
        this.warpCategory = warpCategory;
        this.privateFlag = !plugin.getSettings().isPublicWarps();
    }

    @Override
    public Island getIsland() {
        return warpCategory.getIsland();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        Preconditions.checkNotNull(name, "name parameter cannot be null.");
        SuperiorSkyblockPlugin.debug("Action: Update Warp Name, Island: " + getOwnerName() + ", Warp: " + this.name + ", New Name: " + name);
        String oldName = this.name;
        this.name = name;
        IslandsDatabaseBridge.updateWarpName(getIsland(), this, oldName);
    }

    @Override
    public Location getLocation() {
        return location.clone();
    }

    @Override
    public void setLocation(Location location) {
        Preconditions.checkNotNull(location, "location parameter cannot be null.");
        SuperiorSkyblockPlugin.debug("Action: Update Warp Location, Island: " + getOwnerName() + ", Warp: " + this.name + ", New Location: " + SBlockPosition.of(location));
        this.location = location.clone();
        IslandsDatabaseBridge.updateWarpLocation(getIsland(), this);
    }

    @Override
    public boolean hasPrivateFlag() {
        return privateFlag;
    }

    @Override
    public void setPrivateFlag(boolean privateFlag) {
        SuperiorSkyblockPlugin.debug("Action: Update Warp Private, Island: " + getOwnerName() + ", Warp: " + this.name + ", Private: " + privateFlag);
        this.privateFlag = privateFlag;
        IslandsDatabaseBridge.updateWarpPrivateStatus(getIsland(), this);
    }

    @Override
    public ItemStack getRawIcon() {
        return icon == null ? null : icon.clone();
    }

    @Override
    public ItemStack getIcon(SuperiorPlayer superiorPlayer) {
        if (icon == null)
            return null;

        try {
            ItemBuilder itemBuilder = new ItemBuilder(icon)
                    .replaceAll("{0}", name);
            return superiorPlayer == null ? itemBuilder.build() : itemBuilder.build(superiorPlayer);
        } catch (Exception ex) {
            setIcon(null);
            SuperiorSkyblockPlugin.debug(ex);
            return null;
        }
    }

    @Override
    public void setIcon(ItemStack icon) {
        SuperiorSkyblockPlugin.debug("Action: Update Warp Icon, Island: " + getOwnerName() + ", Warp: " + this.name);
        this.icon = icon == null ? null : icon.clone();
        IslandsDatabaseBridge.updateWarpIcon(getIsland(), this);
    }

    @Override
    public WarpCategory getCategory() {
        return warpCategory;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SIslandWarp that = (SIslandWarp) o;
        return name.equals(that.name);
    }

    private String getOwnerName() {
        SuperiorPlayer superiorPlayer = getIsland().getOwner();
        return superiorPlayer == null ? "None" : superiorPlayer.getName();
    }

}
