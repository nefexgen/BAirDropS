package org.by1337.bairdrop.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.by1337.bairdrop.AirDrop;
import org.by1337.bairdrop.BAirDrop;
import org.by1337.bairdrop.util.Message;
import org.by1337.bairdrop.util.TimeParser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

public class DelayCommand implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("delay")) {
            return executeDelay(sender);
        }
        return true;
    }

    private static void sendMessage(CommandSender sender, String msg) {
        if (sender instanceof Player pl) {
            Message.sendMsg(pl, msg);
        } else {
            Message.logger(msg);
        }
    }

    public static boolean executeDelay(CommandSender sender) {
        if (!sender.hasPermission("bair.delay")) {
            sendMessage(sender, BAirDrop.getConfigMessage().getMessage("no-prem"));
            return true;
        }
        sendMessage(sender, BAirDrop.getConfigMessage().getMessage("event-delay-header"));
        int num = 1;
        boolean hasEvents = false;
        if (BAirDrop.globalTimer != null && BAirDrop.globalTimer.getTimeToStart() > 0) {
            String time = TimeParser.formatSecondsRu(BAirDrop.globalTimer.getTimeToStart());
            String line = BAirDrop.getConfigMessage().getMessage("event-delay-line")
                    .replace("{num}", String.valueOf(num++))
                    .replace("{time}", time);
            sendMessage(sender, line);
            hasEvents = true;
        }
        for (AirDrop air : BAirDrop.airDrops.values()) {
            if (air.isScheduledTimeEnabled() && !air.isAirDropStarted()) {
                continue;
            }
            if (air.isAirDropStarted()) {
                String nameLine = BAirDrop.getConfigMessage().getMessage("event-delay-spawned-name")
                        .replace("{num}", String.valueOf(num++))
                        .replace("{name}", air.getEventListName());
                sendMessage(sender, nameLine);

                String statusLine;
                if (air.isStartCountdownAfterClick() && !air.isActivated()) {
                    if (air.isAutoActivateEnabled()) {
                        statusLine = BAirDrop.getConfigMessage().getMessage("event-delay-spawned-status-inactive")
                                .replace("{time}", TimeParser.formatSecondsRu(air.getAutoActivateTimer()));
                    } else {
                        statusLine = BAirDrop.getConfigMessage().getMessage("event-delay-spawned-status-not-activated");
                    }
                } else if (!air.isAirDropLocked()) {
                    statusLine = BAirDrop.getConfigMessage().getMessage("event-delay-spawned-status-open")
                            .replace("{time}", TimeParser.formatSecondsRu(air.getTimeStop()));
                } else {
                    statusLine = BAirDrop.getConfigMessage().getMessage("event-delay-spawned-status-closed")
                            .replace("{time}", TimeParser.formatSecondsRu(air.getTimeToOpen()));
                }
                sendMessage(sender, statusLine);

                if (air.getAirDropLocation() != null) {
                    String coordsLine = BAirDrop.getConfigMessage().getMessage("event-delay-spawned-coords")
                            .replace("{x}", String.valueOf(air.getAirDropLocation().getBlockX()))
                            .replace("{y}", String.valueOf(air.getAirDropLocation().getBlockY()))
                            .replace("{z}", String.valueOf(air.getAirDropLocation().getBlockZ()));
                    sendMessage(sender, coordsLine);
                }
                hasEvents = true;
                continue;
            }
            if (air.isTimeCountingEnabled() && air.getTimeToStart() > 0 && !air.isScheduledTimeEnabled()) {
                String time = TimeParser.formatSecondsRu(air.getTimeToStart());
                String line = BAirDrop.getConfigMessage().getMessage("event-delay-line")
                        .replace("{num}", String.valueOf(num++))
                        .replace("{time}", time);
                sendMessage(sender, line);
                hasEvents = true;
            }
        }
        if (!hasEvents) {
            sendMessage(sender, BAirDrop.getConfigMessage().getMessage("event-delay-no-events"));
        }
        return true;
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return List.of("delay");
        }
        return Collections.emptyList();
    }
}
