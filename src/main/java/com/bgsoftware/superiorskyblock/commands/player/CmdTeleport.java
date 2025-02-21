package com.bgsoftware.superiorskyblock.commands.player;

import com.bgsoftware.superiorskyblock.Locale;
import com.bgsoftware.superiorskyblock.SuperiorSkyblockPlugin;
import com.bgsoftware.superiorskyblock.api.island.Island;
import com.bgsoftware.superiorskyblock.api.objects.Pair;
import com.bgsoftware.superiorskyblock.api.wrappers.SuperiorPlayer;
import com.bgsoftware.superiorskyblock.commands.CommandArguments;
import com.bgsoftware.superiorskyblock.commands.ISuperiorCommand;
import com.bgsoftware.superiorskyblock.utils.StringUtils;
import com.bgsoftware.superiorskyblock.utils.threads.Executor;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

public final class CmdTeleport implements ISuperiorCommand {

    @Override
    public List<String> getAliases() {
        return Arrays.asList("tp", "teleport", "go", "home");
    }

    @Override
    public String getPermission() {
        return "superior.island.teleport";
    }

    @Override
    public String getUsage(java.util.Locale locale) {
        return "teleport";
    }

    @Override
    public String getDescription(java.util.Locale locale) {
        return Locale.COMMAND_DESCRIPTION_TELEPORT.getMessage(locale);
    }

    @Override
    public int getMinArgs() {
        return 1;
    }

    @Override
    public int getMaxArgs() {
        return 1;
    }

    @Override
    public boolean canBeExecutedByConsole() {
        return false;
    }

    @Override
    public void execute(SuperiorSkyblockPlugin plugin, CommandSender sender, String[] args) {
        Pair<Island, SuperiorPlayer> arguments = CommandArguments.getSenderIsland(plugin, sender);

        Island island = arguments.getKey();

        if (island == null)
            return;

        SuperiorPlayer superiorPlayer = arguments.getValue();

        if (plugin.getSettings().getHomeWarmup() > 0 && !superiorPlayer.hasBypassModeEnabled() && !superiorPlayer.hasPermission("superior.admin.bypass.warmup")) {
            Locale.TELEPORT_WARMUP.send(superiorPlayer, StringUtils.formatTime(superiorPlayer.getUserLocale(),
                    plugin.getSettings().getHomeWarmup(), TimeUnit.MILLISECONDS));
            superiorPlayer.setTeleportTask(Executor.sync(() ->
                    teleportToIsland(superiorPlayer, island), plugin.getSettings().getHomeWarmup() / 50));
        } else {
            teleportToIsland(superiorPlayer, island);
        }
    }

    @Override
    public List<String> tabComplete(SuperiorSkyblockPlugin plugin, CommandSender sender, String[] args) {
        return new ArrayList<>();
    }

    private void teleportToIsland(SuperiorPlayer superiorPlayer, Island island) {
        superiorPlayer.setTeleportTask(null);
        superiorPlayer.teleport(island, result -> {
            if (result)
                Locale.TELEPORTED_SUCCESS.send(superiorPlayer);
            else
                Locale.TELEPORTED_FAILED.send(superiorPlayer);
        });
    }

}
