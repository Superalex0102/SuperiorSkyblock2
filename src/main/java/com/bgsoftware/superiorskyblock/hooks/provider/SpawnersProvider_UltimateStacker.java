package com.bgsoftware.superiorskyblock.hooks.provider;

import com.bgsoftware.superiorskyblock.Locale;
import com.bgsoftware.superiorskyblock.SuperiorSkyblockPlugin;
import com.bgsoftware.superiorskyblock.api.island.Island;
import com.bgsoftware.superiorskyblock.api.objects.Pair;
import com.bgsoftware.superiorskyblock.key.Key;
import com.bgsoftware.superiorskyblock.utils.StringUtils;
import com.bgsoftware.superiorskyblock.utils.legacy.Materials;
import com.google.common.base.Preconditions;
import com.songoda.ultimatestacker.UltimateStacker;
import com.songoda.ultimatestacker.events.SpawnerBreakEvent;
import com.songoda.ultimatestacker.events.SpawnerPlaceEvent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public final class SpawnersProvider_UltimateStacker implements SpawnersProviderItemMetaSpawnerType {

    private static boolean registered = false;

    private final UltimateStacker instance = UltimateStacker.getInstance();

    public SpawnersProvider_UltimateStacker() {
        if (!registered) {
            Bukkit.getPluginManager().registerEvents(new StackerListener(), SuperiorSkyblockPlugin.getPlugin());
            registered = true;
            SuperiorSkyblockPlugin.log("Using UltimateStacker as a spawners provider.");
        }
    }

    @Override
    public Pair<Integer, String> getSpawner(Location location) {
        Preconditions.checkNotNull(location, "location parameter cannot be null.");

        int blockCount = -1;
        if (Bukkit.isPrimaryThread()) {
            blockCount = instance.getSpawnerStackManager().getSpawner(location).getAmount();
        }

        return new Pair<>(blockCount, null);
    }

    @SuppressWarnings("unused")
    private static class StackerListener implements Listener {

        private final SuperiorSkyblockPlugin plugin = SuperiorSkyblockPlugin.getPlugin();

        @EventHandler(priority = EventPriority.HIGHEST)
        public void onSpawnerStack(SpawnerPlaceEvent e) {
            Island island = plugin.getGrid().getIslandAt(e.getBlock().getLocation());

            if (island == null)
                return;

            Key blockKey = Key.of(Materials.SPAWNER.toBukkitType() + "", e.getSpawnerType().name());
            int increaseAmount = e.getAmount();

            if (island.hasReachedBlockLimit(blockKey, increaseAmount)) {
                e.setCancelled(true);
                Locale.REACHED_BLOCK_LIMIT.send(e.getPlayer(), StringUtils.format(blockKey.toString()));
            } else {
                island.handleBlockPlace(blockKey, increaseAmount);
            }
        }

        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onSpawnerUnstack(SpawnerBreakEvent e) {
            Island island = plugin.getGrid().getIslandAt(e.getBlock().getLocation());
            if (island != null)
                island.handleBlockBreak(e.getBlock(), e.getAmount());
        }

    }

}
