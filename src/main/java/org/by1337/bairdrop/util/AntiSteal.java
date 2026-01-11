package org.by1337.bairdrop.util;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.by1337.bairdrop.AirDrop;
import org.by1337.bairdrop.BAirDrop;
import org.by1337.bairdrop.customListeners.CustomEvent;

import java.util.*;

public class AntiSteal implements Listener {
    private Map<UUID, ChestStealData> chestStealDataMap = new HashMap<>();
    private final AirDrop airDrop;

    public AntiSteal(AirDrop airDrop) {
        this.airDrop = airDrop;
        Bukkit.getServer().getPluginManager().registerEvents(this, BAirDrop.getInstance());
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory().equals(airDrop.getInventory())) {
            Player player = (Player) event.getWhoClicked();
            
            if (event.getClickedInventory() != null && event.getClickedInventory().equals(player.getInventory()) && event.isShiftClick()) {
                event.setCancelled(true);
                return;
            }
            if (event.getClickedInventory() != null && event.getClickedInventory().equals(airDrop.getInventory()) && event.getCursor() != null && !event.getCursor().getType().isAir()) {
                event.setCancelled(true);
                return;
            }
            if (event.getClickedInventory() != null && event.getClickedInventory().equals(airDrop.getInventory()) && event.getHotbarButton() != -1) {
                event.setCancelled(true);
                return;
            }
            
            if (event.getCurrentItem() == null) return;
            
            if (!BAirDrop.getInstance().getConfig().getBoolean("anti-steal.enable", false)) return;
            
            ChestStealData chestStealData = chestStealDataMap.getOrDefault(player.getUniqueId(), new ChestStealData());
            long currentTime = System.currentTimeMillis();
            int cooldownMs = BAirDrop.getInstance().getConfig().getInt("anti-steal.cooldown");
            int cooldownTicks = Math.abs(cooldownMs / 50);

            if (chestStealData.getLastSteal() != -1 && currentTime - chestStealData.getLastSteal() <= cooldownMs) {
                event.setCancelled(true);
                if (BAirDrop.getInstance().getConfig().getBoolean("anti-steal.show-message", true)) {
                    Message.sendMsg(player, BAirDrop.getConfigMessage().getMessage("anti-steal-limit-speed"));
                }
                chestStealDataMap.put(player.getUniqueId(), chestStealData);
                return;
            }

            if (chestStealData.getLastTime() != 0) {
                long lastActionTime = chestStealData.getLastTime();
                long interval = currentTime - lastActionTime;
                if (interval != 0) {
                    chestStealData.addTime(interval);
                }
                if (chestStealData.getWarnings() >= BAirDrop.getInstance().getConfig().getInt("anti-steal.max-warnings")) {
                    airDrop.notifyObservers(CustomEvent.PLAYER_STEAL, player);
                    chestStealData.reset();
                    event.setCancelled(true);
                    chestStealDataMap.put(player.getUniqueId(), chestStealData);
                    return;
                }
            }

            chestStealData.setLastTime(currentTime);
            chestStealData.setLastSteal(currentTime);
            if (BAirDrop.getInstance().getConfig().getBoolean("anti-steal.show-cooldown-on-click", true)) {
                applyCooldownToAllItems(player, event.getInventory(), cooldownTicks);
            }
            chestStealDataMap.put(player.getUniqueId(), chestStealData);
        }
    }
    public void unregister(){
        HandlerList.unregisterAll(this);
    }

    private void applyCooldownToAllItems(Player player, Inventory inventory, int ticks) {
        Set<Material> materials = new HashSet<>();
        for (ItemStack item : inventory.getContents()) {
            if (item != null && !item.getType().isAir()) {
                materials.add(item.getType());
            }
        }
        for (Material mat : materials) {
            player.setCooldown(mat, ticks);
        }
    }
}
