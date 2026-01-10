package org.by1337.bairdrop.util;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.by1337.bairdrop.AirDrop;
import org.by1337.bairdrop.BAirDrop;

import java.util.*;

public class DecoyManager implements Listener {
    private final AirDrop airDrop;
    private final Map<UUID, Inventory> playerDecoyInventories = new HashMap<>();
    private final Map<UUID, Map<Integer, ItemStack>> playerRealItems = new HashMap<>();
    private static final Map<String, Map<UUID, ChestStealData>> globalChestStealDataMap = new HashMap<>();
    private final List<Material> fakeItems = new ArrayList<>();
    private final List<String> fakeNames = new ArrayList<>();
    private final Random random = new Random();
    private static final NamespacedKey DECOY_KEY = NamespacedKey.fromString("bairdrop_decoy");
    private final boolean antiStealEnabled;

    public DecoyManager(AirDrop airDrop) {
        this.airDrop = airDrop;
        this.antiStealEnabled = BAirDrop.getInstance().getConfig().getBoolean("anti-steal.enable", false);
        loadConfig();
        Bukkit.getServer().getPluginManager().registerEvents(this, BAirDrop.getInstance());
    }

    private Map<UUID, ChestStealData> getChestStealDataMap() {
        return globalChestStealDataMap.computeIfAbsent(airDrop.getSuperName(), k -> new HashMap<>());
    }

    private void loadConfig() {
        List<String> items = BAirDrop.getInstance().getConfig().getStringList("decoy-protection.fake-items");
        for (String item : items) {
            try {
                Material mat = Material.valueOf(item.toUpperCase());
                if (mat.isItem()) {
                    fakeItems.add(mat);
                }
            } catch (IllegalArgumentException ignored) {
            }
        }
        if (fakeItems.isEmpty()) {
            fakeItems.add(Material.GUNPOWDER);
            fakeItems.add(Material.PHANTOM_MEMBRANE);
            fakeItems.add(Material.NAUTILUS_SHELL);
            fakeItems.add(Material.GRAY_DYE);
        }

        fakeNames.addAll(BAirDrop.getInstance().getConfig().getStringList("decoy-protection.fake-names"));
        if (fakeNames.isEmpty()) {
            fakeNames.add("&eКость пирата");
            fakeNames.add("&6Рога мамонта");
            fakeNames.add("&6Битая ваза");
            fakeNames.add("&cБогатое сокровище");
        }
    }

    public Inventory createDecoyInventory(Player player) {
        Inventory realInventory = airDrop.getInventory();
        Inventory decoyInventory = Bukkit.createInventory(null, realInventory.getSize(), 
                Message.messageBuilder(airDrop.getInventoryTitle()));

        Map<Integer, ItemStack> realItemsMap = new HashMap<>();

        for (int slot = 0; slot < realInventory.getSize(); slot++) {
            ItemStack realItem = realInventory.getItem(slot);
            if (realItem != null && !realItem.getType().isAir()) {
                realItemsMap.put(slot, realItem.clone());
                ItemStack decoyItem = createDecoyItem(slot);
                decoyInventory.setItem(slot, decoyItem);
            }
        }

        playerDecoyInventories.put(player.getUniqueId(), decoyInventory);
        playerRealItems.put(player.getUniqueId(), realItemsMap);

        return decoyInventory;
    }

    private ItemStack createDecoyItem(int slot) {
        Material fakeMaterial = fakeItems.get(random.nextInt(fakeItems.size()));
        String fakeName = fakeNames.get(random.nextInt(fakeNames.size()));

        ItemStack decoyItem = new ItemStack(fakeMaterial);
        ItemMeta meta = decoyItem.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(Message.messageBuilder(fakeName));
            meta.getPersistentDataContainer().set(DECOY_KEY, PersistentDataType.INTEGER, slot);
            decoyItem.setItemMeta(meta);
        }
        return decoyItem;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        Inventory decoyInventory = playerDecoyInventories.get(player.getUniqueId());
        if (decoyInventory == null || !event.getInventory().equals(decoyInventory)) return;

        event.setCancelled(true);

        if (event.getCurrentItem() == null || event.getCurrentItem().getType().isAir()) return;
        if (event.getClickedInventory() == null || !event.getClickedInventory().equals(decoyInventory)) return;

        if (antiStealEnabled) {
            ChestStealData chestStealData = getChestStealDataMap().getOrDefault(player.getUniqueId(), new ChestStealData());
            long currentTime = System.currentTimeMillis();
            int cooldownMs = BAirDrop.getInstance().getConfig().getInt("anti-steal.сooldown");
            int cooldownTicks = Math.abs(cooldownMs / 50);

            if (chestStealData.getLastSteal() != -1 && currentTime - chestStealData.getLastSteal() <= cooldownMs) {
                if (BAirDrop.getInstance().getConfig().getBoolean("anti-steal.show-message", true)) {
                    Message.sendMsg(player, BAirDrop.getConfigMessage().getMessage("anti-steal-limit-speed"));
                }
                getChestStealDataMap().put(player.getUniqueId(), chestStealData);
                return;
            }

            if (chestStealData.getLastTime() != 0) {
                long lastActionTime = chestStealData.getLastTime();
                long interval = currentTime - lastActionTime;
                if (interval != 0) {
                    chestStealData.addTime(interval);
                }
                if (chestStealData.getWarnings() >= BAirDrop.getInstance().getConfig().getInt("anti-steal.max-warnings")) {
                    airDrop.notifyObservers(org.by1337.bairdrop.customListeners.CustomEvent.PLAYER_STEAL, player);
                    chestStealData.reset();
                    getChestStealDataMap().put(player.getUniqueId(), chestStealData);
                    return;
                }
            }

            chestStealData.setLastTime(currentTime);
            chestStealData.setLastSteal(currentTime);
            if (BAirDrop.getInstance().getConfig().getBoolean("anti-steal.show-cooldown-on-click", true)) {
                applyCooldownToAllItems(player, decoyInventory, cooldownTicks);
            }
            getChestStealDataMap().put(player.getUniqueId(), chestStealData);
        }

        int slot = event.getSlot();
        Map<Integer, ItemStack> realItems = playerRealItems.get(player.getUniqueId());
        if (realItems == null) return;

        ItemStack realItem = realItems.get(slot);
        if (realItem == null) return;

        Inventory realInventory = airDrop.getInventory();
        ItemStack currentRealItem = realInventory.getItem(slot);
        if (currentRealItem == null || currentRealItem.getType().isAir()) {
            decoyInventory.setItem(slot, null);
            realItems.remove(slot);
            return;
        }

        HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(realItem.clone());
        if (leftover.isEmpty()) {
            realInventory.setItem(slot, null);
            decoyInventory.setItem(slot, null);
            realItems.remove(slot);
        } else {
            Message.sendMsg(player, BAirDrop.getConfigMessage().getMessage("inventory-full"));
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;

        Inventory decoyInventory = playerDecoyInventories.get(player.getUniqueId());
        if (decoyInventory != null && event.getInventory().equals(decoyInventory)) {
            playerDecoyInventories.remove(player.getUniqueId());
            playerRealItems.remove(player.getUniqueId());
        }
    }

    public boolean hasDecoyInventory(Player player) {
        return playerDecoyInventories.containsKey(player.getUniqueId());
    }

    public void unregister() {
        HandlerList.unregisterAll(this);
        playerDecoyInventories.clear();
        playerRealItems.clear();
    }

    public static boolean isEnabled() {
        return BAirDrop.getInstance().getConfig().getBoolean("decoy-protection.enable", false);
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
