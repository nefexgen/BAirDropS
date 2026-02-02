package org.by1337.bairdrop.listeners.util;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.scheduler.BukkitRunnable;

import org.by1337.bairdrop.AirDrop;
import org.by1337.bairdrop.BAirDrop;
import org.by1337.bairdrop.menu.EditAirMenu;
import org.by1337.bairdrop.util.Message;
import org.by1337.bairdrop.util.TimeParser;

public class ListenChat implements Listener {
    public static ListenChat ListenChat = null;
    private final AirDrop airDrop;
    private final String changeNameString;

    private final Player pl;

    public void unReg(){
        HandlerList.unregisterAll(this);
        ListenChat = null;
    }
    public ListenChat(AirDrop airDrop, String changeNameString, Player pl) {
        this.airDrop = airDrop;
        this.changeNameString = changeNameString;
        this.pl = pl;
        ListenChat = this;
        Message.sendMsg(pl, BAirDrop.getConfigMessage().getMessage("edit-chat"));
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent e) {
        if (e.getPlayer().equals(pl)) {
            if (e.getMessage().equalsIgnoreCase("отмена") || e.getMessage().equalsIgnoreCase("cancel")) {
                Message.sendMsg(pl, BAirDrop.getConfigMessage().getMessage("edit-canceled"));
                HandlerList.unregisterAll(this);
                e.setCancelled(true);
                return;
            }
            if (changeNameString.equalsIgnoreCase("invname")) {
                if (airDrop.isAirDropStarted()){
                    Message.sendMsg(pl, BAirDrop.getConfigMessage().getMessage("stop-event-for-edit"));
                    e.setCancelled(true);
                    return;
                }
                airDrop.setInventoryTitle(e.getMessage());
                airDrop.save();
                Message.sendMsg(pl, String.format(BAirDrop.getConfigMessage().getMessage("named-changed"), e.getMessage()));
            }
            if (changeNameString.equalsIgnoreCase("airname")) {
                if (airDrop.isAirDropStarted()){
                    Message.sendMsg(pl, BAirDrop.getConfigMessage().getMessage("stop-event-for-edit"));
                    e.setCancelled(true);
                    return;
                }
                airDrop.setDisplayName(e.getMessage());
                airDrop.save();
                Message.sendMsg(pl, String.format(BAirDrop.getConfigMessage().getMessage("named-changed"), e.getMessage()));
            }
            try {
                if (changeNameString.equalsIgnoreCase("spawnmin")) {
                    if (airDrop.isAirDropStarted()){
                        Message.sendMsg(pl, BAirDrop.getConfigMessage().getMessage("stop-event-for-edit"));
                        e.setCancelled(true);
                        return;
                    }
                    int x = Integer.parseInt(e.getMessage());
                    airDrop.setSpawnRadiusMin(x);
                    airDrop.save();
                    Message.sendMsg(pl, String.format(BAirDrop.getConfigMessage().getMessage("min-spawn-changed"), e.getMessage()));
                }
                if (changeNameString.equalsIgnoreCase("spawnmax")) {
                    if (airDrop.isAirDropStarted()){
                        Message.sendMsg(pl, BAirDrop.getConfigMessage().getMessage("stop-event-for-edit"));
                        e.setCancelled(true);
                        return;
                    }
                    int x = Integer.parseInt(e.getMessage());
                    if(airDrop.getSpawnRadiusMin() >= x){
                        Message.sendMsg(pl, BAirDrop.getConfigMessage().getMessage("max-limit"));
                        e.setCancelled(true);
                        return;
                    }
                    airDrop.setSpawnRadiusMax(x);
                    airDrop.save();
                    Message.sendMsg(pl, String.format(BAirDrop.getConfigMessage().getMessage("max-spawn-changed"), e.getMessage()));
                }
                if (changeNameString.equalsIgnoreCase("airprotect")) {
                    if (airDrop.isAirDropStarted()){
                        Message.sendMsg(pl, BAirDrop.getConfigMessage().getMessage("stop-event-for-edit"));
                        e.setCancelled(true);
                        return;
                    }
                    int x = Integer.parseInt(e.getMessage());
                    airDrop.setRegionRadius(x);
                    airDrop.save();
                    Message.sendMsg(pl, String.format(BAirDrop.getConfigMessage().getMessage("protect-changed"), e.getMessage()));
                }
                if (changeNameString.equalsIgnoreCase("timetostart") || changeNameString.equalsIgnoreCase("timetostartcons")) {
                    boolean forceFlag = TimeParser.hasForceFlag(e.getMessage());
                    if (airDrop.isAirDropStarted() && !forceFlag){
                        Message.sendMsg(pl, BAirDrop.getConfigMessage().getMessage("stop-event-for-edit"));
                        e.setCancelled(true);
                        return;
                    }
                    int seconds = TimeParser.parseToSeconds(e.getMessage());
                    if (forceFlag && airDrop.isAirDropStarted()) {
                        airDrop.setTimeToStartCons(seconds / 60.0);
                        airDrop.save();
                        Message.sendMsg(pl, String.format(BAirDrop.getConfigMessage().getMessage("time-force-changed"), TimeParser.formatSeconds(seconds)));
                    } else {
                        airDrop.setTimeToStart(seconds);
                        airDrop.setTimeToStartCons(seconds / 60.0);
                        airDrop.save();
                        Message.sendMsg(pl, String.format(BAirDrop.getConfigMessage().getMessage("time-to-start-changed"), TimeParser.formatSeconds(seconds)));
                    }
                }
                if (changeNameString.equalsIgnoreCase("searchbeforestart") || changeNameString.equalsIgnoreCase("searchbeforestartcons")) {
                    boolean forceFlag = TimeParser.hasForceFlag(e.getMessage());
                    if (airDrop.isAirDropStarted() && !forceFlag){
                        Message.sendMsg(pl, BAirDrop.getConfigMessage().getMessage("stop-event-for-edit"));
                        e.setCancelled(true);
                        return;
                    }
                    int seconds = TimeParser.parseToSeconds(e.getMessage());
                    if (forceFlag && airDrop.isAirDropStarted()) {
                        airDrop.setSearchBeforeStartCons(seconds / 60.0);
                        airDrop.save();
                        Message.sendMsg(pl, String.format(BAirDrop.getConfigMessage().getMessage("time-force-changed"), TimeParser.formatSeconds(seconds)));
                    } else {
                        airDrop.setSearchBeforeStart(seconds);
                        airDrop.setSearchBeforeStartCons(seconds / 60.0);
                        airDrop.save();
                        Message.sendMsg(pl, String.format(BAirDrop.getConfigMessage().getMessage("search-before-start-changed"), TimeParser.formatSeconds(seconds)));
                    }
                }
                if (changeNameString.equalsIgnoreCase("timetoopen") || changeNameString.equalsIgnoreCase("timetounlockcons")) {
                    boolean forceFlag = TimeParser.hasForceFlag(e.getMessage());
                    if (airDrop.isAirDropStarted() && !forceFlag){
                        Message.sendMsg(pl, BAirDrop.getConfigMessage().getMessage("stop-event-for-edit"));
                        e.setCancelled(true);
                        return;
                    }
                    int seconds = TimeParser.parseToSeconds(e.getMessage());
                    if (forceFlag && airDrop.isAirDropStarted()) {
                        airDrop.setTimeToUnlockCons(seconds / 60.0);
                        airDrop.save();
                        Message.sendMsg(pl, String.format(BAirDrop.getConfigMessage().getMessage("time-force-changed"), TimeParser.formatSeconds(seconds)));
                    } else {
                        airDrop.setTimeToOpen(seconds);
                        airDrop.setTimeToUnlockCons(seconds / 60.0);
                        airDrop.save();
                        Message.sendMsg(pl, String.format(BAirDrop.getConfigMessage().getMessage("time-to-open-changed"), TimeParser.formatSeconds(seconds)));
                    }
                }
                if (changeNameString.equalsIgnoreCase("timestop") || changeNameString.equalsIgnoreCase("timetostopcons")) {
                    boolean forceFlag = TimeParser.hasForceFlag(e.getMessage());
                    if (airDrop.isAirDropStarted() && !forceFlag){
                        Message.sendMsg(pl, BAirDrop.getConfigMessage().getMessage("stop-event-for-edit"));
                        e.setCancelled(true);
                        return;
                    }
                    int seconds = TimeParser.parseToSeconds(e.getMessage());
                    if (forceFlag && airDrop.isAirDropStarted()) {
                        airDrop.setTimeToStopCons(seconds / 60.0);
                        airDrop.save();
                        Message.sendMsg(pl, String.format(BAirDrop.getConfigMessage().getMessage("time-force-changed"), TimeParser.formatSeconds(seconds)));
                    } else {
                        airDrop.setTimeStop(seconds);
                        airDrop.setTimeToStopCons(seconds / 60.0);
                        airDrop.save();
                        Message.sendMsg(pl, String.format(BAirDrop.getConfigMessage().getMessage("time-to-stop-changed"), TimeParser.formatSeconds(seconds)));
                    }
                }
                if (changeNameString.equalsIgnoreCase("minonlineplayers")) {
                    if (airDrop.isAirDropStarted()){
                        Message.sendMsg(pl, BAirDrop.getConfigMessage().getMessage("stop-event-for-edit"));
                        e.setCancelled(true);
                        return;
                    }
                    int x = Integer.parseInt(e.getMessage());
                    airDrop.setMinPlayersToStart(x);
                    airDrop.save();
                    Message.sendMsg(pl, String.format(BAirDrop.getConfigMessage().getMessage("min-online-players-changed"), e.getMessage()));
                }
            } catch (NumberFormatException var3) {
                Message.sendMsg(pl, BAirDrop.getConfigMessage().getMessage("isn-t-number"));
                e.setCancelled(true);
                return;
            }
            e.setCancelled(true);
            HandlerList.unregisterAll(this);
            new BukkitRunnable() {
                @Override
                public void run() {
                    EditAirMenu em = new EditAirMenu(airDrop);
                    airDrop.setEditAirMenu(em);
                    pl.openInventory(em.getInventory());
                    cancel();
                }
            }.runTaskTimer(BAirDrop.getInstance(), 1, 1);
        }
    }


}

