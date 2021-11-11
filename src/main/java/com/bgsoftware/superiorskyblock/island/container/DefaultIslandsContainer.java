package com.bgsoftware.superiorskyblock.island.container;

import com.bgsoftware.superiorskyblock.SuperiorSkyblockPlugin;
import com.bgsoftware.superiorskyblock.api.island.Island;
import com.bgsoftware.superiorskyblock.api.island.SortingType;
import com.bgsoftware.superiorskyblock.island.IslandPosition;
import com.bgsoftware.superiorskyblock.island.container.cache.IslandsCacheFile;
import com.bgsoftware.superiorskyblock.structure.SortedRegistry;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalCause;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import org.bukkit.Location;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Predicate;

@SuppressWarnings("UnstableApiUsage")
public final class DefaultIslandsContainer implements IslandsContainer {

    private static final Predicate<Island> ISLANDS_PREDICATE = island -> !island.isIgnored();

    // TODO: Should not be saved as Island objects anymore.
    private final SortedRegistry<UUID, Island, SortingType> sortedIslands = new SortedRegistry<>();
    private final Map<IslandPosition, Island> islandsByPositions = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> islandSessionIds = new HashMap<>();
    private final LoadingCache<UUID, Island> islandsByUUID;
    private final IslandsCacheFile islandsCacheFile;

    private final SuperiorSkyblockPlugin plugin;

    private int lastSessionId = 0;

    public DefaultIslandsContainer(SuperiorSkyblockPlugin plugin) {
        this.plugin = plugin;
        this.islandsCacheFile = IslandsCacheFile.open(plugin.getDataFolder(), ".cache");

        CacheBuilder<Object, Object> cacheBuilder = CacheBuilder.newBuilder();
        if (this.islandsCacheFile != null) {
            cacheBuilder.expireAfterAccess(10, TimeUnit.MINUTES);
        }
        this.islandsByUUID = cacheBuilder.removalListener(new IslandsCacheRemovalListener())
                .build(new IslandsCacheLoader());

        SortingType.values().forEach(sortingType -> addSortingType(sortingType, false));
    }

    @Override
    public void addIsland(Island island) {
        Location islandLocation = island.getCenter(plugin.getSettings().getWorlds().getDefaultWorld());
        this.islandsByPositions.put(IslandPosition.of(islandLocation), island);

        if (plugin.getProviders().hasCustomWorldsSupport()) {
            runWithCustomWorld(islandLocation, island, World.Environment.NORMAL,
                    location -> this.islandsByPositions.put(IslandPosition.of(location), island));
            runWithCustomWorld(islandLocation, island, World.Environment.NETHER,
                    location -> this.islandsByPositions.put(IslandPosition.of(location), island));
            runWithCustomWorld(islandLocation, island, World.Environment.THE_END,
                    location -> this.islandsByPositions.put(IslandPosition.of(location), island));
        }

        this.islandsByUUID.put(island.getUniqueId(), island);
        this.islandSessionIds.put(island.getUniqueId(), island.getSessionId());
        this.sortedIslands.put(island.getOwner().getUniqueId(), island);
    }

    @Override
    public void removeIsland(Island island) {
        Location islandLocation = island.getCenter(plugin.getSettings().getWorlds().getDefaultWorld());

        sortedIslands.remove(island.getOwner().getUniqueId());
        islandsByUUID.invalidate(island.getUniqueId());
        islandsByPositions.remove(IslandPosition.of(islandLocation));

        if (plugin.getProviders().hasCustomWorldsSupport()) {
            runWithCustomWorld(islandLocation, island, World.Environment.NORMAL,
                    location -> islandsByPositions.remove(IslandPosition.of(location)));
            runWithCustomWorld(islandLocation, island, World.Environment.NETHER,
                    location -> islandsByPositions.remove(IslandPosition.of(location)));
            runWithCustomWorld(islandLocation, island, World.Environment.THE_END,
                    location -> islandsByPositions.remove(IslandPosition.of(location)));
        }
    }

    @Nullable
    @Override
    public Island getIslandByUUID(UUID uuid) {
        return this.islandsByUUID.getUnchecked(uuid);
    }

    @Nullable
    @Override
    public Island getIslandByOwner(UUID uuid) {
        return this.sortedIslands.get(uuid);
    }

    @Nullable
    @Override
    public Island getIslandAtPosition(int position, SortingType sortingType) {
        return position < 0 || position > getIslandsAmount() ? null : this.sortedIslands.get(position, sortingType);
    }

    @Override
    public int getIslandPosition(Island island, SortingType sortingType) {
        return this.sortedIslands.indexOf(island, sortingType);
    }

    @Override
    public int getIslandsAmount() {
        return (int) this.islandsByUUID.size();
    }

    @Nullable
    @Override
    public Island getIslandAt(Location location) {
        Island island = this.islandsByPositions.get(IslandPosition.of(location));
        return island == null || !island.isInside(location) ? null : island;
    }

    @Override
    public void transferIsland(UUID oldOwner, UUID newOwner) {
        Island island = sortedIslands.get(oldOwner);
        sortedIslands.remove(oldOwner);
        sortedIslands.put(newOwner, island);
    }

    @Override
    public void sortIslands(SortingType sortingType, Runnable onFinish) {
        this.sortedIslands.sort(sortingType, ISLANDS_PREDICATE, onFinish);
    }

    @Override
    public List<Island> getSortedIslands(SortingType sortingType) {
        return this.sortedIslands.getIslands(sortingType);
    }

    @Override
    public List<Island> getIslandsUnsorted() {
        return Collections.unmodifiableList(new ArrayList<>(this.islandsByUUID.asMap().values()));
    }

    @Override
    public void addSortingType(SortingType sortingType, boolean sort) {
        this.sortedIslands.registerSortingType(sortingType, sort, ISLANDS_PREDICATE);
    }

    @Override
    public int nextSessionId() {
        return ++lastSessionId;
    }

    private void runWithCustomWorld(Location islandLocation, Island island, World.Environment environment, Consumer<Location> onSuccess) {
        try {
            Location location = island.getCenter(environment);
            if (!location.getWorld().equals(islandLocation.getWorld()))
                onSuccess.accept(location);
        } catch (Exception ignored) {
        }
    }

    private class IslandsCacheLoader extends CacheLoader<UUID, Island> {

        @Override
        public Island load(@NotNull UUID uuid) throws Exception {
            if (islandsCacheFile == null)
                throw new NullPointerException("Islands cache is not valid.");

            Island loadedIsland = islandsCacheFile.readIsland(islandSessionIds.getOrDefault(uuid, 0))
                    .orElse(null);

            if (loadedIsland == null)
                throw new NullPointerException("Island was not found in cache file.");

            return loadedIsland;
        }
    }

    private class IslandsCacheRemovalListener implements RemovalListener<UUID, Island> {

        @Override
        public void onRemoval(@NotNull RemovalNotification<UUID, Island> removalNotification) {
            if (islandsCacheFile != null &&
                    removalNotification.getCause() == RemovalCause.EXPIRED &&
                    removalNotification.getValue() != null)
                islandsCacheFile.saveIsland(removalNotification.getValue());
        }

    }

}
