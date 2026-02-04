package org.by1337.bairdrop;

import java.util.Arrays;
import com.sk89q.worldedit.EditSession;
import org.bukkit.*;
import org.bukkit.block.Lidded;
import org.bukkit.block.data.type.RespawnAnchor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import org.by1337.bairdrop.ItemUtil.EnchantMaterial;
import org.by1337.bairdrop.ItemUtil.Items;
import org.by1337.bairdrop.locationGenerator.Generator;
import org.by1337.bairdrop.locationGenerator.CGenerator;
import org.by1337.bairdrop.locationGenerator.GeneratorUtils;
import org.by1337.bairdrop.worldGuardHook.RegionManager;
import org.by1337.bairdrop.worldGuardHook.SchematicsManager;
import org.by1337.bairdrop.api.event.AirDropEndEvent;
import org.by1337.bairdrop.api.event.AirDropStartEvent;
import org.by1337.bairdrop.api.event.AirDropUnlockEvent;
import org.by1337.bairdrop.customListeners.CustomEvent;
import org.by1337.bairdrop.customListeners.observer.Observer;
import org.by1337.bairdrop.effect.EffectFactory;
import org.by1337.bairdrop.effect.IEffect;
import org.by1337.bairdrop.serializable.EffectDeserialize;
import org.by1337.bairdrop.serializable.EffectSerializable;
import org.by1337.bairdrop.serializable.StateSerializable;
import org.by1337.bairdrop.hologram.HologramManager;
import org.by1337.bairdrop.hologram.HologramSettings;
import org.by1337.bairdrop.hologram.HologramType;
import org.by1337.bairdrop.bossbar.AirDropBossBar;
import org.by1337.bairdrop.util.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import static org.by1337.bairdrop.BAirDrop.*;

import org.by1337.bairdrop.util.Message;
import org.by1337.bairdrop.menu.EditAirMenu;

public class CAirDrop implements AirDrop, StateSerializable {
    private String inventoryTitle;
    private String displayName;
    private String eventListName;
    private int inventorySize;
    private World world;
    private int spawnRadiusMin;
    private int spawnRadiusMax;
    private int regionRadius;
    private int timeToStart;
    private int searchBeforeStart;
    private int timeToOpen;
    private boolean startCountdownAfterClick;
    private boolean autoActivateEnabled;
    private int autoActivateTimer;
    private boolean timeStopEventMustGo;
    private int timeStop;
    private Material materialLocked;
    private Material materialUnlocked;
    private final HashMap<String, List<Items>> listItems = new HashMap<>();
    private boolean airDropLocked = true;
    private Inventory inventory;
    private Location airDropLocation = null;
    private Location futureLocation = null;
    private FileConfiguration fileConfiguration;
    private int minPlayersToStart;
    private String id;
    private boolean wasOpened = false;
    private boolean airDropStarted = false;
    private File airDropFile;
    private List<String> signedListener;
    private EditAirMenu EditAirMenu;
    private boolean activated;
    private double timeToStartCons;
    private double timeToStopCons;
    private double timeToUnlockCons;
    private double searchBeforeStartCons;
    private boolean flatnessCheck;
    private Location staticLocation;
    private boolean useStaticLoc;
    private HashMap<String, IEffect> loadedEffect = new HashMap<>();
    private int spawnChance;
    private boolean timeCountingEnabled;
    private EditSession editSession = null;
    private String generatorSettings;
    private List<String> airHolo = new ArrayList<>();
    private List<String> airHoloOpen = new ArrayList<>();
    private List<String> airHoloClickWait = new ArrayList<>();
    private List<String> airHoloToStart = new ArrayList<>();
    private Vector holoOffsets;
    private boolean canceled;
    private boolean clone;
    private boolean kill;
    private boolean holoTimeToStartEnabled;
    private boolean holoTimeToStartMinusOffsets;
    private boolean usePlayerLocation;
    private boolean stopWhenEmpty;
    private boolean stopWhenEmpty_event;
    private boolean summoner;
    private boolean randomizeSlot;
    private boolean hideInCompleter;
    private final CAirDrop CAirDropInstance;
    private boolean useOnlyStaticLoc;
    private final List<Observer> observers = new ArrayList<>();
    private AntiSteal antiSteal = null;
    private DecoyManager decoyManager = null;
    private Generator generator;
    private String superName;
    private List<String> dec = new ArrayList<>();
    private boolean decoyProtectionEnabled = false;
    private List<String> decoyFakeItems = new ArrayList<>();
    private List<String> decoyFakeNames = new ArrayList<>();
    private boolean itemRevealEnabled = false;
    private int itemRevealMinPerStep = 3;
    private int itemRevealMaxPerStep = 5;
    private double itemRevealInterval = 0.5;
    private boolean itemRevealStepSoundEnabled = false;
    private String itemRevealStepSound = "block.amethyst_cluster.fall";
    private float itemRevealSoundVolume = 0.8f;
    private float itemRevealSoundPitchMin = 0.9f;
    private float itemRevealSoundPitchMax = 1.1f;
    private int itemRevealSoundRadius = 16;
    private List<ItemStack> pendingRevealItems = new ArrayList<>();
    private BukkitRunnable itemRevealTask = null;
    private HologramType hologramType;
    private HologramSettings hologramSettings;
    private boolean topLooterGlowEnabled = false;
    private int topLooterGlowDuration = 10;
    private boolean scheduledTimeEnabled = false;
    private List<String> scheduledTimes = new ArrayList<>();
    private String lastScheduledTrigger = "";
    private org.by1337.bairdrop.bossbar.AirDropBossBar airDropBossBar;
    private org.by1337.bairdrop.worldGuardHook.SavedBlocksData savedBlocksData = null;

    CAirDrop(FileConfiguration fileConfiguration, File airDropFile) {
        CAirDropInstance = this;
        try {
            this.airDropFile = airDropFile;
            this.fileConfiguration = fileConfiguration;
            id = fileConfiguration.getString("air-id");
            superName = id;

            InvalidCharactersChecker invalidCharactersChecker = new InvalidCharacters();
            String invalidChars = invalidCharactersChecker.getInvalidCharacters(id);

            if (!invalidChars.isEmpty()) {
                Message.error(String.format("Недопустимые символы: %s", invalidChars));
                Message.error("АирДроп не загружен!");
                airDrops.remove(id);
                return;
            }

            useStaticLoc = fileConfiguration.getBoolean("use-static-loc");
            stopWhenEmpty = fileConfiguration.getBoolean("stop-when-empty");
            randomizeSlot = fileConfiguration.getBoolean("random-slot");

            if (fileConfiguration.getString("static-location.world") != null) {
                double x = fileConfiguration.getDouble("static-location.x");
                double y = fileConfiguration.getDouble("static-location.y");
                double z = fileConfiguration.getDouble("static-location.z");
                World world1 = Bukkit.getWorld(fileConfiguration.getString("static-location.world"));
                if (world1 == null) {
                    Message.error(String.format(BAirDrop.getConfigMessage().getMessage("static-loc-error"), id));
                } else {
                    staticLocation = new Location(world1, x, y, z);
                }
            }

            spawnChance = fileConfiguration.getInt("spawn-chance");
            flatnessCheck = fileConfiguration.getBoolean("flatness-check");
            minPlayersToStart = fileConfiguration.getInt("min-online-players");
            inventoryTitle = fileConfiguration.getString("inv-name");

            displayName = fileConfiguration.getString("air-name");
            eventListName = fileConfiguration.getString("event-list-name", displayName);
            inventorySize = fileConfiguration.getInt("chest-inventory-size");
            world = Bukkit.getWorld(Objects.requireNonNull(fileConfiguration.getString("air-spawn-world")));
            spawnRadiusMin = fileConfiguration.getInt("air-spawn-radius-min");
            spawnRadiusMax = fileConfiguration.getInt("air-spawn-radius-max");
            if (spawnRadiusMin > spawnRadiusMax) {
                Message.error("air-spawn-radius-min не может быть больше air-spawn-radius-max");
                spawnRadiusMax = Math.abs(spawnRadiusMax);
                spawnRadiusMin = -spawnRadiusMax;
            }
            regionRadius = fileConfiguration.getInt("air-radius-protect");
            generatorSettings = fileConfiguration.getString("generator-settings");


            timeToStart = TimeParser.parseToSeconds(fileConfiguration.getString("timeToStart", "2m"));
            timeToStartCons = timeToStart / 60.0;

            searchBeforeStart = TimeParser.parseToSeconds(fileConfiguration.getString("search-before-start", "1m"));
            searchBeforeStartCons = searchBeforeStart / 60.0;

            timeToOpen = TimeParser.parseToSeconds(fileConfiguration.getString("openingTime", "1m"));
            timeToUnlockCons = timeToOpen / 60.0;

            timeStop = TimeParser.parseToSeconds(fileConfiguration.getString("time-stop-event", "1m"));
            timeToStopCons = timeStop / 60.0;

            startCountdownAfterClick = fileConfiguration.getBoolean("start-countdown-after-click");
            autoActivateEnabled = fileConfiguration.getBoolean("auto-activate", false);
            timeStopEventMustGo = fileConfiguration.getBoolean("time-stop-event-must-go");

            materialLocked = Material.valueOf(fileConfiguration.getString("chest-material-locked"));
            materialUnlocked = Material.valueOf(fileConfiguration.getString("chest-material-unlocked"));
            signedListener = fileConfiguration.getStringList("signed-events");

            signedListener = signedListener.stream().map(String::toLowerCase).collect(Collectors.toList());

            inventory = Bukkit.createInventory(null, inventorySize, Message.messageBuilderComponent(inventoryTitle));

            airHolo = fileConfiguration.getStringList("air-holo");
            airHoloOpen = fileConfiguration.getStringList("air-holo-open");
            airHoloClickWait = fileConfiguration.getStringList("air-holo-click-wait");
            airHoloToStart = fileConfiguration.getStringList("air-holo-to-start");
            useOnlyStaticLoc = fileConfiguration.getBoolean("use-only-static-loc");
            decoyProtectionEnabled = fileConfiguration.getBoolean("decoy-protection.enable", false);
            decoyFakeItems = fileConfiguration.getStringList("decoy-protection.fake-items");
            decoyFakeNames = fileConfiguration.getStringList("decoy-protection.fake-names");
            itemRevealEnabled = fileConfiguration.getBoolean("item-reveal.enabled", false);
            String itemsPerStep = fileConfiguration.getString("item-reveal.items-per-step", "3-5");
            if (itemsPerStep.contains("-")) {
                String[] parts = itemsPerStep.split("-");
                itemRevealMinPerStep = Integer.parseInt(parts[0].trim());
                itemRevealMaxPerStep = Integer.parseInt(parts[1].trim());
            } else {
                itemRevealMinPerStep = Integer.parseInt(itemsPerStep.trim());
                itemRevealMaxPerStep = itemRevealMinPerStep;
            }
            itemRevealInterval = fileConfiguration.getDouble("item-reveal.interval", 0.5);
            itemRevealStepSoundEnabled = fileConfiguration.getBoolean("item-reveal.step-sound.enabled", false);
            itemRevealStepSound = fileConfiguration.getString("item-reveal.step-sound.sound", "block.amethyst_cluster.fall");
            itemRevealSoundVolume = (float) fileConfiguration.getDouble("item-reveal.step-sound.volume", 0.8);
            String pitchStr = fileConfiguration.getString("item-reveal.step-sound.pitch", "0.9-1.1");
            if (pitchStr.contains("-")) {
                String[] pitchParts = pitchStr.split("-");
                itemRevealSoundPitchMin = Float.parseFloat(pitchParts[0].trim());
                itemRevealSoundPitchMax = Float.parseFloat(pitchParts[1].trim());
            } else {
                itemRevealSoundPitchMin = Float.parseFloat(pitchStr.trim());
                itemRevealSoundPitchMax = itemRevealSoundPitchMin;
            }
            itemRevealSoundRadius = fileConfiguration.getInt("item-reveal.step-sound.radius", 16);
            holoOffsets = new Vector(
                    fileConfiguration.getDouble("holo-offsets.x"),
                    fileConfiguration.getDouble("holo-offsets.y"),
                    fileConfiguration.getDouble("holo-offsets.z")
            );

            String hologramTypeStr = fileConfiguration.getString("holograms");
            hologramType = HologramType.fromString(hologramTypeStr);
            if (hologramType == null) {
                Message.error("Отсутствует тип голограммы для аирдропа " + id);
            }
            hologramSettings = HologramSettings.fromConfig(fileConfiguration);

            topLooterGlowEnabled = fileConfiguration.getBoolean("top-looter-glow.enabled", false);
            topLooterGlowDuration = fileConfiguration.getInt("top-looter-glow.duration", 10);

            scheduledTimeEnabled = fileConfiguration.getBoolean("scheduled-time.enabled", false);
            scheduledTimes = fileConfiguration.getStringList("scheduled-time.times");

            airDropBossBar = new org.by1337.bairdrop.bossbar.AirDropBossBar(this);
            airDropBossBar.setEnabled(fileConfiguration.getBoolean("bossbar.enabled", false));
            airDropBossBar.setVisibility(fileConfiguration.getString("bossbar.visibility", "global"));
            airDropBossBar.setRadius(fileConfiguration.getInt("bossbar.radius", 30));
            try {
                airDropBossBar.setColor(org.bukkit.boss.BarColor.valueOf(fileConfiguration.getString("bossbar.color", "RED")));
            } catch (IllegalArgumentException e) {
                airDropBossBar.setColor(org.bukkit.boss.BarColor.RED);
            }
            try {
                airDropBossBar.setStyle(org.bukkit.boss.BarStyle.valueOf(fileConfiguration.getString("bossbar.style", "SOLID")));
            } catch (IllegalArgumentException e) {
                airDropBossBar.setStyle(org.bukkit.boss.BarStyle.SOLID);
            }
            airDropBossBar.setTitleClosed(fileConfiguration.getString("bossbar.title-closed", "{air-name} &7откроется через &c{time-to-open} сек"));
            airDropBossBar.setTitleOpen(fileConfiguration.getString("bossbar.title-open", "{air-name} &7открыт. До удаления &c{time-stop} сек"));
            airDropBossBar.setTitleActivate(fileConfiguration.getString("bossbar.title-activate", "{air-name} &7активируется через &c{auto-activate-timer} сек"));
            airDropBossBar.setTitleNotActivated(fileConfiguration.getString("bossbar.title-not-activated", "{air-name} &7не активирован"));

            generator = new CGenerator();
            if (fileConfiguration.getConfigurationSection("inv") != null) {
                for (String inv : fileConfiguration.getConfigurationSection("inv").getKeys(false)) {
                    for (String slot : fileConfiguration.getConfigurationSection("inv." + inv).getKeys(false)) {
                        List<String> chance = fileConfiguration.getConfigurationSection("inv." + inv + "." + slot).getKeys(false).stream().toList();
                        if (chance.size() == 0) {
                            Message.warning(BAirDrop.getConfigMessage().getMessage("item-error").replace("{slot}", slot));
                            continue;
                        }

                        if (chance.size() >= 2)
                            Message.warning(BAirDrop.getConfigMessage().getMessage("slot-more").replace("{slot}", slot).replace("{id}", id).replace("{size}", String.valueOf(chance.size())));


                        ItemStack item = fileConfiguration.getItemStack("inv." + inv + "." + slot + "." + chance.get(0));
                        if (item == null) {
                            Message.warning(BAirDrop.getConfigMessage().getMessage("item-null").replace("{slot}", slot));
                            continue;
                        }

                        Items items = new Items(Integer.parseInt(slot), Integer.parseInt(chance.get(0)), item, inv);
                        List<Items> list = new ArrayList<>(listItems.getOrDefault(inv, new ArrayList<>()));
                        list.add(items);
                        listItems.put(inv, list);
                    }
                }
            }

            Message.logger(BAirDrop.getConfigMessage().getMessage("air-loaded").replace("{id}", id));
        } catch (Exception e) {
            e.printStackTrace();
            Message.error(BAirDrop.getConfigMessage().getMessage("not-load"));
        }
        notifyObservers(CustomEvent.LOAD, null);
        run();
    }

    public CAirDrop(String id) {
        CAirDropInstance = this;
        try {
            this.id = id;
            superName = id;
            createFile();
            
            inventoryTitle = "new air";
            displayName = "new air name";
            eventListName = "new air name";
            inventorySize = 54;
            
            world = Bukkit.getWorld("world") == null ? Bukkit.getWorlds().get(0) : Bukkit.getWorld("world");
            spawnRadiusMin = -2000;
            spawnRadiusMax = 2000;
            spawnChance = 50;
            minPlayersToStart = 1;
            generatorSettings = "default";
            flatnessCheck = false;
            regionRadius = 15;
            
            useStaticLoc = false;
            useOnlyStaticLoc = false;
            staticLocation = null;
            
            timeToStartCons = 2;
            searchBeforeStartCons = 1;
            timeToUnlockCons = 1;
            timeToStopCons = 1;
            timeToStart = 2 * 60;
            searchBeforeStart = 60;
            timeToOpen = 60;
            timeStop = 60;
            
            startCountdownAfterClick = false;
            autoActivateEnabled = false;
            timeStopEventMustGo = false;
            stopWhenEmpty = false;
            randomizeSlot = true;
            
            topLooterGlowEnabled = false;
            topLooterGlowDuration = 10;
            
            materialLocked = Material.RESPAWN_ANCHOR;
            materialUnlocked = Material.CHEST;
            
            hologramType = HologramType.ARMORSTAND;
            holoOffsets = new Vector(0.5, 1.5, 0.5);
            hologramSettings = new HologramSettings();
            
            airHoloToStart = BAirDrop.getConfigMessage().getList("air-holo-to-start");
            airHolo = BAirDrop.getConfigMessage().getList("air-holo");
            airHoloOpen = BAirDrop.getConfigMessage().getList("air-holo-open");
            airHoloClickWait = BAirDrop.getConfigMessage().getList("air-holo-click-wait");
            
            decoyProtectionEnabled = false;
            decoyFakeItems = new ArrayList<>(Arrays.asList("GUNPOWDER", "PHANTOM_MEMBRANE", "NAUTILUS_SHELL", "GRAY_DYE"));
            decoyFakeNames = new ArrayList<>(Arrays.asList("&eКость пирата", "&6Рога мамонта", "&6Битая ваза", "&cБогатое сокровище"));
            
            scheduledTimeEnabled = false;
            scheduledTimes = new ArrayList<>(Arrays.asList("12:00", "18:00"));
            
            itemRevealEnabled = false;
            itemRevealMinPerStep = 3;
            itemRevealMaxPerStep = 5;
            itemRevealInterval = 0.5;
            
            airDropBossBar = new AirDropBossBar(this);
            
            signedListener = new ArrayList<>();
            generator = new CGenerator();
            inventory = Bukkit.createInventory(null, inventorySize, Message.messageBuilderComponent(inventoryTitle));
            Message.logger(BAirDrop.getConfigMessage().getMessage("air-loaded").replace("{id}", this.id));
        } catch (Exception var3) {
            var3.printStackTrace();
        }
        save();
        notifyObservers(CustomEvent.LOAD, null);
        run();
    }

    private void run() {
        timeCountingEnabled = !BAirDrop.getInstance().getConfig().getBoolean("global-time.enable");
        new BukkitRunnable() {
            @Override
            public void run() {
                locationSearch();
                synchronized (this) {
                    if (canceled) {
                        cancel();
                        if (EditAirMenu != null) {
                            EditAirMenu.unReg();
                            EditAirMenu.getInventory().clear();
                        }
                        if (isAirDropStarted())
                            End();
                        if (clone) {
                            notifyObservers(CustomEvent.UNLOAD, null);
                            airDrops.remove(id);
                        }
                        return;
                    }
                    if (!airDropStarted && (timeToStart <= 0 || isScheduledTimeNow())) {
                        start();
                        updateEditAirMenu("stats");
                    } else if (Bukkit.getOnlinePlayers().size() >= minPlayersToStart && !airDropStarted && (timeCountingEnabled || summoner) && !scheduledTimeEnabled) {
                        timeToStart--;
                        if (holoTimeToStartEnabled) {
                            List<String> lines = new ArrayList<>(airHoloToStart);
                            lines.replaceAll(s -> replaceInternalPlaceholder(s));

                            if (hologramType != null) {
                                if (!holoTimeToStartMinusOffsets) {
                                    HologramManager.createOrUpdateHologram(lines, getAnyLoc().clone().add(holoOffsets), id, hologramType, hologramSettings);
                                } else {
                                    HologramManager.createOrUpdateHologram(lines, getAnyLoc().clone().add(holoOffsets).add(
                                            -GeneratorUtils.getSettings(getGeneratorSettings(), String.format("%s.offsets.x", GeneratorUtils.getWorldKeyByWorld(getAnyLoc().getWorld()))),
                                            -GeneratorUtils.getSettings(getGeneratorSettings(), String.format("%s.offsets.y", GeneratorUtils.getWorldKeyByWorld(getAnyLoc().getWorld()))),
                                            -GeneratorUtils.getSettings(getGeneratorSettings(), String.format("%s.offsets.z", GeneratorUtils.getWorldKeyByWorld(getAnyLoc().getWorld())))).add(0, 1, 0), id, hologramType, hologramSettings);
                                }
                            }
                        }

                        updateEditAirMenu("stats");
                    }

                    if (airDropStarted && airDropLocked && timeToOpen <= 0) {
                        unlock();
                        updateEditAirMenu("stats");
                    } else if (airDropStarted && airDropLocked && (!startCountdownAfterClick || activated)) {
                        timeToOpen--;
                        List<String> lines = new ArrayList<>(airHolo);
                        lines.replaceAll(s -> replaceInternalPlaceholder(s));
                        if (hologramType != null) {
                            HologramManager.createOrUpdateHologram(lines, airDropLocation.clone().add(holoOffsets), id, hologramType, hologramSettings);
                        }
                        updateEditAirMenu("stats");
                    } else if (startCountdownAfterClick && airDropLocked && airDropStarted && !activated) {
                        if (autoActivateEnabled) {
                            autoActivateTimer--;
                            if (autoActivateTimer <= 0) {
                                activated = true;
                            }
                        }
                        List<String> lines = new ArrayList<>(airHoloClickWait);
                        lines.replaceAll(s -> replaceInternalPlaceholder(s));
                        if (hologramType != null) {
                            HologramManager.createOrUpdateHologram(lines, airDropLocation.clone().add(holoOffsets), id, hologramType, hologramSettings);
                        }

                    }

                    if (timeStop <= 0) {
                        End();
                        updateEditAirMenu("stats");

                    } else if (!airDropLocked || timeStopEventMustGo && airDropStarted) {
                        timeStop--;
                        updateEditAirMenu("stats");
                    }
                    notifyObservers(CustomEvent.TIMER, null);
                    if (airDropBossBar != null && airDropBossBar.isEnabled()) {
                        airDropBossBar.update();
                    }
                    if (isStopWhenEmpty() && isAirDropStarted()) {
                        boolean stop = false;
                        for (ItemStack itemStack : inventory) {
                            if (itemStack != null) {
                                stop = false;
                                break;
                            } else stop = true;
                        }
                        if (stop) {
                            stopWhenEmpty_event = true;
                            if (antiSteal != null) {
                                antiSteal.applyTopLooterGlow();
                            }
                            notifyObservers(CustomEvent.STOP_WHEN_EMPTY, null);
                            End();
                        }
                    }
                    if (airDropStarted) {
                        List<HumanEntity> heList = new ArrayList<>(getInventory().getViewers());
                        for (HumanEntity he : heList) {
                            if (he instanceof Player pl) {
                                if (getAirDropLocation().distance(pl.getLocation()) > 10D) {
                                    pl.closeInventory();
                                }
                            }
                        }
                    }
                }
            }
        }.runTaskTimer(BAirDrop.getInstance(), 20, 20);//20 20
    }

    private BukkitTask bukkitTaskStart = null;

    @Override
    public void startCommand(@Nullable Player player) {
        if (bukkitTaskStart != null && !bukkitTaskStart.isCancelled()) {
            bukkitTaskStart.cancel();
        }
        bukkitTaskStart = new BukkitRunnable() {
            int x = 0;

            @Override
            public void run() {
                locationSearch();
                if (airDropLocation != null) {
                    Message.sendMsg(player, "&aStarted");
                    start();
                    cancel();
                } else
                    Message.sendMsg(player, "&cFail start: " + x);
                x++;
            }
        }.runTaskTimer(BAirDrop.getInstance(), 1L, 10L);
    }

    @Override
    public void save() {
        if (clone) return;
        
        fileConfiguration.set("air-id", id);
        fileConfiguration.set("air-name", displayName);
        fileConfiguration.set("event-list-name", eventListName);
        fileConfiguration.set("inv-name", inventoryTitle);
        fileConfiguration.set("chest-inventory-size", inventorySize);
        
        fileConfiguration.set("spawn-chance", spawnChance);
        fileConfiguration.set("air-spawn-world", world.getName());
        fileConfiguration.set("air-spawn-radius-min", spawnRadiusMin);
        fileConfiguration.set("air-spawn-radius-max", spawnRadiusMax);
        fileConfiguration.set("air-radius-protect", regionRadius);
        fileConfiguration.set("generator-settings", generatorSettings);
        fileConfiguration.set("flatness-check", flatnessCheck);
        
        fileConfiguration.set("use-static-loc", useStaticLoc);
        fileConfiguration.set("use-only-static-loc", useOnlyStaticLoc);
        if (staticLocation != null) {
            fileConfiguration.set("static-location.world", staticLocation.getWorld().getName());
            fileConfiguration.set("static-location.x", staticLocation.getX());
            fileConfiguration.set("static-location.y", staticLocation.getY());
            fileConfiguration.set("static-location.z", staticLocation.getZ());
        }
        
        fileConfiguration.set("timeToStart", TimeParser.formatSeconds((int)(timeToStartCons * 60)));
        fileConfiguration.set("search-before-start", TimeParser.formatSeconds((int)(searchBeforeStartCons * 60)));
        fileConfiguration.set("openingTime", TimeParser.formatSeconds((int)(timeToUnlockCons * 60)));
        fileConfiguration.set("time-stop-event", TimeParser.formatSeconds((int)(timeToStopCons * 60)));
        
        fileConfiguration.set("start-countdown-after-click", startCountdownAfterClick);
        fileConfiguration.set("auto-activate", autoActivateEnabled);
        fileConfiguration.set("time-stop-event-must-go", timeStopEventMustGo);
        fileConfiguration.set("stop-when-empty", stopWhenEmpty);
        fileConfiguration.set("min-online-players", minPlayersToStart);
        fileConfiguration.set("random-slot", randomizeSlot);
        
        fileConfiguration.set("chest-material-locked", materialLocked.toString());
        fileConfiguration.set("chest-material-unlocked", materialUnlocked.toString());
        
        fileConfiguration.set("holo-offsets.x", holoOffsets.getX());
        fileConfiguration.set("holo-offsets.y", holoOffsets.getY());
        fileConfiguration.set("holo-offsets.z", holoOffsets.getZ());
        
        if (hologramType != null) {
            fileConfiguration.set("holograms", hologramType.name().toLowerCase());
        }
        
        fileConfiguration.set("holo-settings.text-shadow", hologramSettings.isTextShadow());
        fileConfiguration.set("holo-settings.text-opacity", (hologramSettings.getTextOpacity() & 0xFF) * 100 / 255);
        fileConfiguration.set("holo-settings.background-color", String.format("#%02X%02X%02X", 
            hologramSettings.getBackgroundColor().getRed(),
            hologramSettings.getBackgroundColor().getGreen(),
            hologramSettings.getBackgroundColor().getBlue()));
        fileConfiguration.set("holo-settings.background-opacity", hologramSettings.getBackgroundOpacity());
        fileConfiguration.set("holo-settings.see-through", hologramSettings.isSeeThrough());
        fileConfiguration.set("holo-settings.view-range", hologramSettings.getViewRange());
        fileConfiguration.set("holo-settings.brightness", hologramSettings.getBrightness());
        fileConfiguration.set("holo-settings.scale", hologramSettings.getScale());
        fileConfiguration.set("holo-settings.text-alignment", hologramSettings.getTextAlignment().name());
        fileConfiguration.set("holo-settings.billboard", hologramSettings.getBillboard().name());
        fileConfiguration.set("holo-settings.yaw", hologramSettings.getYaw());
        fileConfiguration.set("holo-settings.pitch", hologramSettings.getPitch());
        
        fileConfiguration.set("air-holo-to-start", airHoloToStart);
        fileConfiguration.set("air-holo", airHolo);
        fileConfiguration.set("air-holo-open", airHoloOpen);
        fileConfiguration.set("air-holo-click-wait", airHoloClickWait);
        
        fileConfiguration.set("decoy-protection.enable", decoyProtectionEnabled);
        fileConfiguration.set("decoy-protection.fake-items", decoyFakeItems);
        fileConfiguration.set("decoy-protection.fake-names", decoyFakeNames);
        
        fileConfiguration.set("item-reveal.enabled", itemRevealEnabled);
        fileConfiguration.set("item-reveal.items-per-step", itemRevealMinPerStep + "-" + itemRevealMaxPerStep);
        fileConfiguration.set("item-reveal.interval", itemRevealInterval);
        fileConfiguration.set("item-reveal.step-sound.enabled", itemRevealStepSoundEnabled);
        fileConfiguration.set("item-reveal.step-sound.sound", itemRevealStepSound);
        fileConfiguration.set("item-reveal.step-sound.volume", itemRevealSoundVolume);
        String pitchValue = itemRevealSoundPitchMin == itemRevealSoundPitchMax 
            ? String.valueOf(itemRevealSoundPitchMin) 
            : itemRevealSoundPitchMin + "-" + itemRevealSoundPitchMax;
        fileConfiguration.set("item-reveal.step-sound.pitch", pitchValue);
        fileConfiguration.set("item-reveal.step-sound.radius", itemRevealSoundRadius);
        
        fileConfiguration.set("top-looter-glow.enabled", topLooterGlowEnabled);
        fileConfiguration.set("top-looter-glow.duration", topLooterGlowDuration);
        
        fileConfiguration.set("scheduled-time.enabled", scheduledTimeEnabled);
        fileConfiguration.set("scheduled-time.times", scheduledTimes);

        fileConfiguration.set("bossbar.enabled", airDropBossBar.isEnabled());
        fileConfiguration.set("bossbar.visibility", airDropBossBar.getVisibility());
        fileConfiguration.set("bossbar.radius", airDropBossBar.getRadius());
        fileConfiguration.set("bossbar.color", airDropBossBar.getColor().name());
        fileConfiguration.set("bossbar.style", airDropBossBar.getStyle().name());
        fileConfiguration.set("bossbar.title-closed", airDropBossBar.getTitleClosed());
        fileConfiguration.set("bossbar.title-open", airDropBossBar.getTitleOpen());
        fileConfiguration.set("bossbar.title-activate", airDropBossBar.getTitleActivate());
        fileConfiguration.set("bossbar.title-not-activated", airDropBossBar.getTitleNotActivated());
        
        fileConfiguration.set("signed-events", signedListener);

        if (!getListItems().isEmpty()) {
            fileConfiguration.set("inv", null);
            for (String invName : getListItems().keySet()) {
                for (Items item : getListItems().get(invName)) {
                    fileConfiguration.set("inv." + invName + "." + item.getSlot() + "." + item.getChance(), item.getItem());
                }
            }
        }
        try {
            fileConfiguration.save(airDropFile);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void start() {

        if (listItems.isEmpty()) {
            Message.error(BAirDrop.getConfigMessage().getMessage("items-is-empty"));
            inventory.setItem(0, new ItemStack(Material.DIRT));
        }

        if (airDropLocation == null) {
            if (staticLocation == null) {
                Message.error(BAirDrop.getConfigMessage().getMessage("loc-is-null"));
                End();
                return;
            } else airDropLocation = staticLocation.clone();

        }
        AirDropStartEvent airDropStartEvent = new AirDropStartEvent(this);
        Bukkit.getServer().getPluginManager().callEvent(airDropStartEvent);
        if (airDropStartEvent.isCancelled())
            return;

        RegionManager.SetRegion(this);
        timeToStart = 0;
        futureLocation = null;
        stopWhenEmpty_event = false;

        try {
            airDropLocation.getBlock().setType(materialLocked);
            if (materialLocked == Material.RESPAWN_ANCHOR) {
                RespawnAnchor ra = (RespawnAnchor) airDropLocation.getBlock().getBlockData();
                ra.setCharges(4);
                airDropLocation.getBlock().setBlockData(ra);
            }
        } catch (IllegalArgumentException e) {
            Message.error(String.format(BAirDrop.getConfigMessage().getMessage("material-error"), materialLocked));
            airDropLocation.getBlock().setType(Material.DIRT);
        }


        if (listItems.size() == 1) {//old system
            String key = null;
            for (String str : listItems.keySet()) {
                key = str;
                break;
            }
            if (key != null)
                for (Items items : listItems.get(key)) {
                    ItemStack itemStack = items.getItem();
                    if (!EnchantMaterial.materialHashMap.isEmpty()) {
                        for (String str : EnchantMaterial.materialHashMap.keySet()) {
                            EnchantMaterial em = EnchantMaterial.materialHashMap.get(str);
                            if (em.getMaterial() == itemStack.getType()) {
                                itemStack = em.enchant(itemStack);
                            }
                        }
                    }
                    if (ThreadLocalRandom.current().nextInt(0, 100) <= items.getChance()) {
                        if (randomizeSlot)
                            inventory.setItem(getEmptyRandomSlot(), itemStack);
                        else
                            inventory.setItem(items.getSlot(), itemStack);
                    }
                }
        } else {//new system
            List<Items> list = new ArrayList<>();
            for (List<Items> items : listItems.values()) {
                list.addAll(items);
            }
            for (int x = 0; x < inventory.getSize(); x++) {
                if (list.isEmpty()) break;
                Items items1 = list.get(ThreadLocalRandom.current().nextInt(list.size()));
                ItemStack itemStack = items1.getItem();
                if (ThreadLocalRandom.current().nextInt(0, 100) <= items1.getChance()) {
                    if (!EnchantMaterial.materialHashMap.isEmpty()) {
                        for (String str : EnchantMaterial.materialHashMap.keySet()) {
                            EnchantMaterial em = EnchantMaterial.materialHashMap.get(str);
                            if (em.getMaterial() == itemStack.getType()) {
                                itemStack = em.enchant(itemStack);
                            }
                        }
                    }

                    if (randomizeSlot) {
                        inventory.setItem(getEmptyRandomSlot(), itemStack);
                    } else {
                        inventory.setItem(x, itemStack);
                    }
                }
                list.remove(items1);
            }
        }
        airDropStarted = true;
        autoActivateTimer = timeToOpen;
        updateEditAirMenu("stats");
        if (antiSteal != null) antiSteal.unregister();
        antiSteal = new AntiSteal(this);

        if (airDropBossBar != null && airDropBossBar.isEnabled()) {
            airDropBossBar.create();
        }

        notifyObservers(CustomEvent.START_EVENT, null);
    }

    private int getEmptyRandomSlot() {
        int next = 0;
        while (next <= 200) {//200
            int slot = ThreadLocalRandom.current().nextInt(inventory.getSize());
            if (inventory.getItem(slot) == null) {
                return slot;
            }
            next++;
        }
        return 0;
    }

    @Override
    public File getAirDropFile() {
        return airDropFile;
    }

    @Override
    public void unlock() {
        if (!airDropStarted) {
            throw new IllegalStateException("airdrop is not started!");
        }
        AirDropUnlockEvent airDropUnlockEvent = new AirDropUnlockEvent(this);
        Bukkit.getServer().getPluginManager().callEvent(airDropUnlockEvent);
        if (airDropUnlockEvent.isCancelled())
            return;

        airDropLocked = false;
        timeToOpen = 0;
        
        if (itemRevealEnabled) {
            pendingRevealItems.clear();
            for (int i = 0; i < inventory.getSize(); i++) {
                ItemStack item = inventory.getItem(i);
                if (item != null && !item.getType().isAir()) {
                    pendingRevealItems.add(item.clone());
                    inventory.setItem(i, null);
                }
            }
            java.util.Collections.shuffle(pendingRevealItems);
            startItemRevealTask();
        }
        
        try {
            airDropLocation.getBlock().setType(materialUnlocked);
            if (materialUnlocked == Material.RESPAWN_ANCHOR) {
                RespawnAnchor ra = (RespawnAnchor) airDropLocation.getBlock().getBlockData();
                ra.setCharges(4);
                airDropLocation.getBlock().setBlockData(ra);
            }
            if (materialUnlocked == Material.CHEST || materialUnlocked == Material.ENDER_CHEST) {
                if (airDropLocation.getBlock().getState() instanceof Lidded lidded) {
                    lidded.open();
                }
            }

        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            Message.error(String.format(BAirDrop.getConfigMessage().getMessage("material-error"), materialUnlocked));
            airDropLocation.getBlock().setType(Material.DIRT);
        }

        List<String> lines = new ArrayList<>(airHoloOpen);
        lines.replaceAll(this::replaceInternalPlaceholder);
        if (hologramType != null) {
            HologramManager.createOrUpdateHologram(lines, airDropLocation.clone().add(holoOffsets), id, hologramType, hologramSettings);
        }
        notifyObservers(CustomEvent.UNLOCK_EVENT, null);
    }

    private void startItemRevealTask() {
        if (itemRevealTask != null) {
            itemRevealTask.cancel();
        }
        long intervalTicks = Math.max(1, (long) (itemRevealInterval * 20));
        itemRevealTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (pendingRevealItems.isEmpty() || !airDropStarted) {
                    cancel();
                    itemRevealTask = null;
                    return;
                }
                int itemsToReveal = itemRevealMinPerStep + new java.util.Random().nextInt(Math.max(1, itemRevealMaxPerStep - itemRevealMinPerStep + 1));
                List<Integer> emptySlots = new ArrayList<>();
                for (int slot = 0; slot < inventory.getSize(); slot++) {
                    ItemStack slotItem = inventory.getItem(slot);
                    if (slotItem == null || slotItem.getType().isAir()) {
                        emptySlots.add(slot);
                    }
                }
                java.util.Collections.shuffle(emptySlots);
                int itemsRevealed = 0;
                for (int i = 0; i < itemsToReveal && !pendingRevealItems.isEmpty() && !emptySlots.isEmpty(); i++) {
                    ItemStack item = pendingRevealItems.remove(0);
                    int randomSlot = emptySlots.remove(0);
                    inventory.setItem(randomSlot, item);
                    itemsRevealed++;
                }
                if (itemsRevealed > 0 && itemRevealStepSoundEnabled && itemRevealStepSound != null && !itemRevealStepSound.isEmpty() && airDropLocation != null) {
                    float pitch = itemRevealSoundPitchMin == itemRevealSoundPitchMax 
                        ? itemRevealSoundPitchMin 
                        : itemRevealSoundPitchMin + new java.util.Random().nextFloat() * (itemRevealSoundPitchMax - itemRevealSoundPitchMin);
                    if (itemRevealSoundRadius == 0) {
                        Set<Player> viewers = new java.util.HashSet<>();
                        for (org.bukkit.entity.HumanEntity viewer : inventory.getViewers()) {
                            if (viewer instanceof Player p) {
                                viewers.add(p);
                            }
                        }
                        if (decoyManager != null) {
                            viewers.addAll(decoyManager.getViewers());
                        }
                        for (Player p : viewers) {
                            p.playSound(airDropLocation, itemRevealStepSound, itemRevealSoundVolume, pitch);
                        }
                    } else {
                        int radius = Math.max(1, itemRevealSoundRadius);
                        for (org.bukkit.entity.Entity entity : airDropLocation.getWorld().getNearbyEntities(
                                airDropLocation, radius, radius, radius)) {
                            if (entity instanceof Player p) {
                                if (p.getLocation().distance(airDropLocation) <= radius) {
                                    p.playSound(airDropLocation, itemRevealStepSound, itemRevealSoundVolume, pitch);
                                }
                            }
                        }
                    }
                }
                if (decoyManager != null) {
                    decoyManager.refreshDecoyInventories();
                }
            }
        };
        itemRevealTask.runTaskTimer(BAirDrop.getInstance(), intervalTicks, intervalTicks);
    }

    @Override
    public void End() {
        AirDropEndEvent airDropEndEvent = new AirDropEndEvent(this);
        Bukkit.getServer().getPluginManager().callEvent(airDropEndEvent);
        if (airDropEndEvent.isCancelled())
            return;

        notifyObservers(CustomEvent.END_EVENT, null);
        schematicsUndo();
        if (airDropLocation != null)
            airDropLocation.getBlock().setType(Material.AIR);
        RegionManager.RemoveRegion(this);
        airDropLocation = null;
        List<HumanEntity> list = new ArrayList<>(inventory.getViewers());
        for (HumanEntity he : list) {
            if (he instanceof Player pl) {
                pl.closeInventory();
            }
        }
        inventory.clear();
        if (itemRevealTask != null) {
            itemRevealTask.cancel();
            itemRevealTask = null;
        }
        pendingRevealItems.clear();
        wasOpened = false;
        airDropStarted = false;
        activated = false;
        timeToStart = (int) (timeToStartCons * 60);
        searchBeforeStart = (int) (searchBeforeStartCons * 60);
        timeToOpen = (int) (timeToUnlockCons * 60);
        timeStop = (int) (timeToStopCons * 60);
        airDropLocked = true;
        HologramManager.remove(id);
        updateEditAirMenu("stats");
        if (kill) canceled = true;
        setUsePlayerLocation(false);
        summoner = false;
        if (antiSteal != null) {
            antiSteal.unregister();
            antiSteal = null;
        }
        if (decoyManager != null) {
            decoyManager.unregister();
            decoyManager = null;
        }
        if (airDropBossBar != null) {
            airDropBossBar.remove();
        }
    }

    private BukkitTask bukkitTask = null;

    private void locationSearch() {
        if (useOnlyStaticLoc && !airDropStarted) {
            if (staticLocation == null) {
                Message.error("use-only-static-loc = true, но статическая локация равна null!");
            } else {
                futureLocation = staticLocation;
                airDropLocation = staticLocation;
                return;
            }
        }
        if (futureLocation != null && !airDropStarted) {
            airDropLocation = futureLocation;
            return;
        }
        if (bukkitTask == null || bukkitTask.isCancelled()) {
            bukkitTask = new BukkitRunnable() {
                @Override
                public void run() {
                    try {
                        if (futureLocation == null) {
                            futureLocation = generator.getLocation(CAirDropInstance);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    CAirDropInstance.setBukkitTask(null);
                }
            }.runTaskAsynchronously(getInstance());
        }
    }

    private synchronized void setBukkitTask(BukkitTask bukkitTask) {
        this.bukkitTask = bukkitTask;
    }

    @Override
    public String replaceInternalPlaceholder(String str) {//clown method 🤡
        if (!str.contains("{"))
            return str;
        if (str.contains("\\{")){
            return str.replace("\\", "");
        }
        StringBuilder sb = new StringBuilder(str);


        boolean b = true;
        while (b){
            b = false;
            if (str.contains("{!")) {
                if (sb.indexOf("{!clone}") != -1){
                    sb.replace(sb.indexOf("{!clone}"), sb.indexOf("{!clone}") + 8, String.valueOf(!isClone()));
                    b = true;
                    continue;
                }
                if (sb.indexOf("{!airdrop-is-open}") != -1){
                    sb.replace(sb.indexOf("{!airdrop-is-open}"), sb.indexOf("{!airdrop-is-open}") + 18, String.valueOf(airDropLocked));
                    b = true;
                    continue;
                }
                if (sb.indexOf("{!airdrop-is-start}") != -1){
                    sb.replace(sb.indexOf("{!airdrop-is-start}"), sb.indexOf("{!airdrop-is-start}") + 19, String.valueOf(!airDropStarted));
                    b = true;
                    continue;
                }
                if (sb.indexOf("{!it-was-open}") != -1){
                    sb.replace(sb.indexOf("{!it-was-open}"), sb.indexOf("{!it-was-open}") + 14, String.valueOf(!wasOpened));
                    b = true;
                    continue;
                }
                if (sb.indexOf("{!time-stop-event-must-go}") != -1){
                    sb.replace(sb.indexOf("{!time-stop-event-must-go}"), sb.indexOf("{!time-stop-event-must-go}") + 26, String.valueOf(!timeStopEventMustGo));
                    b = true;
                    continue;
                }
                if (sb.indexOf("{!use-static-loc}") != -1){
                    sb.replace(sb.indexOf("{!use-static-loc}"), sb.indexOf("{!use-static-loc}") + 17, String.valueOf(!useStaticLoc));
                    b = true;
                    continue;
                }
                if (sb.indexOf("{!flatness-check}") != -1){
                    sb.replace(sb.indexOf("{!flatness-check}"), sb.indexOf("{!flatness-check}") + 17, String.valueOf(!flatnessCheck));
                    b = true;
                    continue;
                }
            }
            //activated
            if (sb.indexOf("{activated}") != -1){
                sb.replace(sb.indexOf("{activated}"), sb.indexOf("{activated}") + "{activated}".length(), String.valueOf(activated));
                b = true;
                continue;
            }
            if (sb.indexOf("{use-only-static-loc}") != -1){
                sb.replace(sb.indexOf("{use-only-static-loc}"), sb.indexOf("{use-only-static-loc}") + 21, String.valueOf(useOnlyStaticLoc));
                b = true;
                continue;
            }
            if (sb.indexOf("{time-to-open}") != -1){
                sb.replace(sb.indexOf("{time-to-open}"), sb.indexOf("{time-to-open}") + 14, String.valueOf(timeToOpen));
                b = true;
                continue;
            }
            if (sb.indexOf("{time-stop}") != -1){
                sb.replace(sb.indexOf("{time-stop}"), sb.indexOf("{time-stop}") + 11, String.valueOf(timeStop));
                b = true;
                continue;
            }
            if (sb.indexOf("{auto-activate-timer}") != -1){
                sb.replace(sb.indexOf("{auto-activate-timer}"), sb.indexOf("{auto-activate-timer}") + 21, String.valueOf(autoActivateTimer));
                b = true;
                continue;
            }
            if (sb.indexOf("{time-to-start}") != -1){
                sb.replace(sb.indexOf("{time-to-start}"), sb.indexOf("{time-to-start}") + 15, String.valueOf(timeToStart));
                b = true;
                continue;
            }
            if (sb.indexOf("{time-to-end}") != -1){
                sb.replace(sb.indexOf("{time-to-end}"), sb.indexOf("{time-to-end}") + 13, String.valueOf(timeStop));
                b = true;
                continue;
            }
            if (sb.indexOf("{time-to-open-format}") != -1){
                sb.replace(sb.indexOf("{time-to-open-format}"), sb.indexOf("{time-to-open-format}") + 21, AirManager.getFormat(timeToOpen));
                b = true;
                continue;
            }
            if (sb.indexOf("{time-to-start-format}") != -1){
                sb.replace(sb.indexOf("{time-to-start-format}"), sb.indexOf("{time-to-start-format}") + 22, AirManager.getFormat(timeToStart));
                b = true;
                continue;
            }
            if (sb.indexOf("{time-to-end-format}") != -1){
                sb.replace(sb.indexOf("{time-to-end-format}"), sb.indexOf("{time-to-end-format}") + 20, AirManager.getFormat(timeStop));
                b = true;
                continue;
            }
            if (sb.indexOf("{rnd-1}") != -1){
                sb.replace(sb.indexOf("{rnd-1}"), sb.indexOf("{rnd-1}") + 7, String.valueOf(ThreadLocalRandom.current().nextInt(0, 1)));
                b = true;
                continue;
            }
            if (sb.indexOf("{rnd-10}") != -1){
                sb.replace(sb.indexOf("{rnd-10}"), sb.indexOf("{rnd-10}") + 8, String.valueOf(ThreadLocalRandom.current().nextInt(0, 10)));
                b = true;
                continue;
            }
            if (sb.indexOf("{rnd-50}") != -1){
                sb.replace(sb.indexOf("{rnd-50}"), sb.indexOf("{rnd-50}") + 8, String.valueOf(ThreadLocalRandom.current().nextInt(0, 50)));
                b = true;
                continue;
            }
            if (sb.indexOf("{rnd-100}") != -1){
                sb.replace(sb.indexOf("{rnd-100}"), sb.indexOf("{rnd-100}") + 9, String.valueOf(ThreadLocalRandom.current().nextInt(0, 100)));
                b = true;
                continue;
            }
            if (sb.indexOf("{airdrop-is-open}") != -1){
                sb.replace(sb.indexOf("{airdrop-is-open}"), sb.indexOf("{airdrop-is-open}") + 17, String.valueOf(!airDropLocked));
                b = true;
                continue;
            }
            if (sb.indexOf("{airdrop-is-start}") != -1){
                sb.replace(sb.indexOf("{airdrop-is-start}"), sb.indexOf("{airdrop-is-start}") + 18, String.valueOf(airDropStarted));
                b = true;
                continue;
            }
            if (sb.indexOf("{it-was-open}") != -1){
                sb.replace(sb.indexOf("{it-was-open}"), sb.indexOf("{it-was-open}") + 13, String.valueOf(wasOpened));
                b = true;
                continue;
            }
            if (sb.indexOf("{time-stop-event-must-go}") != -1){
                sb.replace(sb.indexOf("{time-stop-event-must-go}"), sb.indexOf("{time-stop-event-must-go}") + 25, String.valueOf(timeStopEventMustGo));
                b = true;
                continue;
            }
            if (sb.indexOf("{use-static-loc}") != -1){
                sb.replace(sb.indexOf("{use-static-loc}"), sb.indexOf("{use-static-loc}") + 16, String.valueOf(useStaticLoc));
                b = true;
                continue;
            }
            if (sb.indexOf("{flatness-check}") != -1){
                sb.replace(sb.indexOf("{flatness-check}"), sb.indexOf("{flatness-check}") + 16, String.valueOf(flatnessCheck));
                b = true;
                continue;
            }
            if (sb.indexOf("{decoy-protection}") != -1){
                sb.replace(sb.indexOf("{decoy-protection}"), sb.indexOf("{decoy-protection}") + 18, String.valueOf(decoyProtectionEnabled));
                b = true;
                continue;
            }
            if (sb.indexOf("{hologram-type}") != -1){
                sb.replace(sb.indexOf("{hologram-type}"), sb.indexOf("{hologram-type}") + 15, hologramType != null ? hologramType.name().toLowerCase() : "null");
                b = true;
                continue;
            }
            if (sb.indexOf("{summoner}") != -1){
                sb.replace(sb.indexOf("{summoner}"), sb.indexOf("{summoner}") + 10, String.valueOf(summoner));
                b = true;
                continue;
            }
            if (sb.indexOf("{id}") != -1){
                sb.replace(sb.indexOf("{id}"), sb.indexOf("{id}") + 4, id);
                b = true;
                continue;
            }
            if (sb.indexOf("{world}") != -1){
                sb.replace(sb.indexOf("{world}"), sb.indexOf("{world}") + 7, world.getName());
                b = true;
                continue;
            }
            if (sb.indexOf("{air-name}") != -1){
                sb.replace(sb.indexOf("{air-name}"), sb.indexOf("{air-name}") + 10, getDisplayName());
                b = true;
                continue;
            }
            if (sb.indexOf("{inv-name}") != -1){
                sb.replace(sb.indexOf("{inv-name}"), sb.indexOf("{inv-name}") + 10, getInventoryTitle());
                b = true;
                continue;
            }
            if (sb.indexOf("{spawn-min}") != -1){
                sb.replace(sb.indexOf("{spawn-min}"), sb.indexOf("{spawn-min}") + 11, String.valueOf(getSpawnRadiusMin()));
                b = true;
                continue;
            }
            if (sb.indexOf("{spawn-max}") != -1){
                sb.replace(sb.indexOf("{spawn-max}"), sb.indexOf("{spawn-max}") + 11, String.valueOf(getSpawnRadiusMax()));
                b = true;
                continue;
            }
            if (sb.indexOf("{air-protect}") != -1){
                sb.replace(sb.indexOf("{air-protect}"), sb.indexOf("{air-protect}") + 13, String.valueOf(getRegionRadius()));
                b = true;
                continue;
            }
            if (sb.indexOf("{search-before-start}") != -1){
                sb.replace(sb.indexOf("{search-before-start}"), sb.indexOf("{search-before-start}") + 21, String.valueOf(getSearchBeforeStart()));
                b = true;
                continue;
            }
            if (sb.indexOf("{min-online-players}") != -1){
                sb.replace(sb.indexOf("{min-online-players}"), sb.indexOf("{min-online-players}") + 20, String.valueOf(getMinPlayersToStart()));
                b = true;
                continue;
            }
            if (sb.indexOf("{material-locked}") != -1){
                sb.replace(sb.indexOf("{material-locked}"), sb.indexOf("{material-locked}") + 17, String.valueOf(materialLocked));
                b = true;
                continue;
            }
            if (sb.indexOf("{material-unlocked}") != -1){
                sb.replace(sb.indexOf("{material-unlocked}"), sb.indexOf("{material-unlocked}") + 19, String.valueOf(materialUnlocked));
                b = true;
                continue;
            }
            if (sb.indexOf("{start-countdown-after-click}") != -1){
                sb.replace(sb.indexOf("{start-countdown-after-click}"), sb.indexOf("{start-countdown-after-click}") + 29, String.valueOf(isStartCountdownAfterClick()));
                b = true;
                continue;
            }
            if (sb.indexOf("{time-to-start-cons}") != -1){
                sb.replace(sb.indexOf("{time-to-start-cons}"), sb.indexOf("{time-to-start-cons}") + 20, TimeParser.formatSeconds((int)(timeToStartCons * 60)));
                b = true;
                continue;
            }
            if (sb.indexOf("{search-before-start-cons}") != -1){
                sb.replace(sb.indexOf("{search-before-start-cons}"), sb.indexOf("{search-before-start-cons}") + 26, TimeParser.formatSeconds((int)(searchBeforeStartCons * 60)));
                b = true;
                continue;
            }
            if (sb.indexOf("{time-to-open-cons}") != -1){
                sb.replace(sb.indexOf("{time-to-open-cons}"), sb.indexOf("{time-to-open-cons}") + 19, TimeParser.formatSeconds((int)(timeToUnlockCons * 60)));
                b = true;
                continue;
            }

            if (sb.indexOf("{time-to-end-cons}") != -1){
                sb.replace(sb.indexOf("{time-to-end-cons}"), sb.indexOf("{time-to-end-cons}") + 18, TimeParser.formatSeconds((int)(timeToStopCons * 60)));
                b = true;
                continue;
            }

            if (sb.indexOf("{clone}") != -1){
                sb.replace(sb.indexOf("{clone}"), sb.indexOf("{clone}") + 7, String.valueOf(isClone()));
                b = true;
                continue;
            }
            if (sb.indexOf("{stopWhenEmpty}") != -1){
                sb.replace(sb.indexOf("{stopWhenEmpty}"), sb.indexOf("{stopWhenEmpty}") + 15, String.valueOf(stopWhenEmpty_event));
                b = true;
                continue;
            }
            if (sb.indexOf("{use-player-location}") != -1){
                sb.replace(sb.indexOf("{use-player-location}"), sb.indexOf("{use-player-location}") + 21, String.valueOf(isUsePlayerLocation()));
                b = true;
                continue;
            }
            if (sb.indexOf("{global-timer}") != -1){
                sb.replace(sb.indexOf("{global-timer}"), sb.indexOf("{global-timer}") + 14, BAirDrop.globalTimer != null ? String.valueOf(BAirDrop.globalTimer.getTimeToStart()) : "var");
                b = true;
                continue;
            }
            if (staticLocation == null) {
                if (sb.indexOf("{stat-world}") != -1){
                    sb.replace(sb.indexOf("{stat-world}"), sb.indexOf("{stat-world}") + 12, "?");
                    b = true;
                    continue;
                }
                if (sb.indexOf("{stat-x}") != -1){
                    sb.replace(sb.indexOf("{stat-x}"), sb.indexOf("{stat-x}") + 8, "?");
                    b = true;
                    continue;
                }
                if (sb.indexOf("{stat-y}") != -1){
                    sb.replace(sb.indexOf("{stat-y}"), sb.indexOf("{stat-y}") + 8, "?");
                    b = true;
                    continue;
                }
                if (sb.indexOf("{stat-z}") != -1){
                    sb.replace(sb.indexOf("{stat-z}"), sb.indexOf("{stat-z}") + 8, "?");
                    b = true;
                    continue;
                }
            } else {
                if (sb.indexOf("{stat-world}") != -1){
                    sb.replace(sb.indexOf("{stat-world}"), sb.indexOf("{stat-world}") + 12, staticLocation.getWorld().getName());
                    b = true;
                    continue;
                }
                if (sb.indexOf("{stat-x}") != -1){
                    sb.replace(sb.indexOf("{stat-x}"), sb.indexOf("{stat-x}") + 8, (String.valueOf(staticLocation.getX())).replace(".0", ""));
                    b = true;
                    continue;
                }
                if (sb.indexOf("{stat-y}") != -1){
                    sb.replace(sb.indexOf("{stat-y}"), sb.indexOf("{stat-y}") + 8, (String.valueOf(staticLocation.getY())).replace(".0", ""));
                    b = true;
                    continue;
                }
                if (sb.indexOf("{stat-z}") != -1){
                    sb.replace(sb.indexOf("{stat-z}"), sb.indexOf("{stat-z}") + 8, (String.valueOf(staticLocation.getZ())).replace(".0", ""));
                    b = true;
                    continue;
                }
            }
            if (getAnyLoc() == null) {
                if (sb.indexOf("{x}") != -1){
                    sb.replace(sb.indexOf("{x}"), sb.indexOf("{x}") + 3, "?");
                    b = true;
                    continue;
                }
                if (sb.indexOf("{y}") != -1){
                    sb.replace(sb.indexOf("{y}"), sb.indexOf("{y}") + 3, "?");
                    b = true;
                    continue;
                }
                if (sb.indexOf("{z}") != -1){
                    sb.replace(sb.indexOf("{z}"), sb.indexOf("{z}") + 3, "?");
                    b = true;
                    continue;
                }
                if (sb.indexOf("{biome}") != -1){
                    sb.replace(sb.indexOf("{biome}"), sb.indexOf("{biome}") + 7, "NONE");
                    b = true;
                    continue;
                }
                if (sb.indexOf("{GET_BLOCK_MATERIAL}") != -1){
                    sb.replace(sb.indexOf("{GET_BLOCK_MATERIAL}"), sb.indexOf("{GET_BLOCK_MATERIAL}") + 20, "AIR");
                    b = true;
                    continue;
                }
            } else {
                if (sb.indexOf("{x}") != -1){
                    sb.replace(sb.indexOf("{x}"), sb.indexOf("{x}") + 3, (String.valueOf(getAnyLoc().getX())).replace(".0", ""));
                    b = true;
                    continue;
                }
                if (sb.indexOf("{y}") != -1){
                    sb.replace(sb.indexOf("{y}"), sb.indexOf("{y}") + 3, (String.valueOf(getAnyLoc().getY())).replace(".0", ""));
                    b = true;
                    continue;
                }
                if (sb.indexOf("{z}") != -1){
                    sb.replace(sb.indexOf("{z}"), sb.indexOf("{z}") + 3, (String.valueOf(getAnyLoc().getZ())).replace(".0", ""));
                    b = true;
                    continue;
                }
                if (sb.indexOf("{biome}") != -1){
                    sb.replace(sb.indexOf("{biome}"), sb.indexOf("{biome}") + 7, GeneratorUtils.getBiome(getAnyLoc()));
                    b = true;
                    continue;
                }
                if (sb.indexOf("{GET_BLOCK_MATERIAL}") != -1){
                    sb.replace(sb.indexOf("{GET_BLOCK_MATERIAL}"), sb.indexOf("{GET_BLOCK_MATERIAL}") + 20, String.valueOf(GeneratorUtils.getBlock(this).getType()));
                    b = true;
                    continue;
                }
            }

        }
        return sb.toString();
    }


    @Override
    public void registerObserver(Observer observer) {
        if (observers.contains(observer)) {
            throw new IllegalArgumentException("this observer is already registered");
        }
        observers.add(observer);
    }

    @Override
    public void saveObserver(String observerKey) {
        if (!signedListener.contains(observerKey))
            signedListener.add(observerKey);
        else
            throw new IllegalArgumentException("this observer is already saved");
    }

    @Override
    public void removeSaveObserver(String observerKey) {
        if (!signedListener.remove(observerKey)) {
            throw new IllegalArgumentException("this observer is not saved yet");
        }
    }

    @Override
    public boolean hasSavedObserver(String observerKey) {
        return signedListener.contains(observerKey);
    }

    @Override
    public void unregisterObserver(Observer observer) {
        if (!observers.remove(observer)) {
            throw new IllegalArgumentException("this observer is not registered yet");
        }
    }

    @Override
    public void notifyObservers(CustomEvent customEvent, @Nullable Player pl) {
        long x = System.currentTimeMillis();

        List<Observer> tempObservers = new ArrayList<>(observers);

        tempObservers.forEach(o -> o.update(pl, this, customEvent, false));

        AirDropUtils.getStaticObservers().forEach(o -> o.update(pl, this, customEvent, false));

        if (System.currentTimeMillis() - x < 50)
            Message.debug(String.format(BAirDrop.getConfigMessage().getMessage("event-time"), customEvent.getKey().getKey(), (System.currentTimeMillis() - x)), LogLevel.HARD);
        else if (System.currentTimeMillis() - x > 50 && System.currentTimeMillis() - x < 75)
            Message.debug(String.format(BAirDrop.getConfigMessage().getMessage("event-time-50"), customEvent.getKey().getKey(), (System.currentTimeMillis() - x)), LogLevel.MEDIUM);
        else if (System.currentTimeMillis() - x > 75)
            Message.debug(String.format(BAirDrop.getConfigMessage().getMessage("event-time-75"), customEvent.getKey().getKey(), (System.currentTimeMillis() - x)), LogLevel.LOW);
    }

    @Override
    public boolean hasObserver(Observer observer) {
        return observers.contains(observer);
    }

    @Override
    public List<Observer> getObservers() {
        return observers;
    }

    @Override
    public void InvokeListener(NamespacedKey listener, @Nullable Player player, CustomEvent customEvent) {
        invokeListener(listener, player, customEvent);
    }

    @Override
    public void invokeListener(NamespacedKey listener, @Nullable Player player, CustomEvent customEvent) {
        try {
            if (!BAirDrop.customEventListeners.containsKey(listener)) {
                Message.error(String.format(BAirDrop.getConfigMessage().getMessage("unknown-listener"), listener));
                return;
            }

            BAirDrop.customEventListeners.get(listener).update(player, this, customEvent, true);
        } catch (StackOverflowError e) {
            Message.error(BAirDrop.getConfigMessage().getMessage("too-many-call"));
        }

    }

    @Override
    public Inventory getEditorItemsInventory(Inventory inv, String invName) {
        Inventory inventory1 = Bukkit.createInventory(inv.getHolder(), inv.getSize(), Message.messageBuilderComponent(invName));

        for (Items items : getListItems().getOrDefault(invName, new ArrayList<>())) {
            ItemStack itemStack = items.getItem();
            ItemMeta im = itemStack.getItemMeta();
            im.getPersistentDataContainer().set(NamespacedKey.fromString("chance"), PersistentDataType.INTEGER, items.getChance());
            itemStack.setItemMeta(im);
            inventory1.setItem(items.getSlot(), itemStack);
        }
        return inventory1;
    }

    @Override
    public org.by1337.bairdrop.menu.EditAirMenu getEditAirMenu() {
        return EditAirMenu;
    }

    @Override
    public void setEditAirMenu(org.by1337.bairdrop.menu.EditAirMenu editAirMenu) {
        EditAirMenu = editAirMenu;
    }

    @Override
    public void updateEditAirMenu() {
        if (EditAirMenu != null)
            EditAirMenu.menuGenerate();
    }

    @Override
    public void updateEditAirMenu(String tag) {
        if (EditAirMenu != null)
            EditAirMenu.menuGenerate(tag);
    }

    @Override
    public void loadEffect(String name, String id) {
        IEffect ie = EffectFactory.getEffect(name);
        if (ie == null) {
            throw new IllegalArgumentException(String.format(BAirDrop.getConfigMessage().getMessage("unknown-effect"), name));
        }
        if (loadedEffect.containsKey(id)){
            throw new IllegalArgumentException(String.format(BAirDrop.getConfigMessage().getMessage("effect-replace-error"), id));
        }
        loadedEffect.put(id, ie);
    }

    @Override
    public void startEffect(String id) {
        IEffect ie = loadedEffect.getOrDefault(id, null);
        if (ie == null) {
            throw new IllegalArgumentException(String.format(BAirDrop.getConfigMessage().getMessage("unknown-effect"), id));
        }
        if (isEffectStarted(id)) {
            throw new IllegalArgumentException(String.format(BAirDrop.getConfigMessage().getMessage("there-is-already-an-effect"), id));
        }
        ie.Start(this);
    }

    @Override
    public boolean isEffectStarted(String id) {
        return loadedEffect.get(id).isUsed();
    }

    @Override
    public void StopEffect(String id) {
        if (!loadedEffect.containsKey(id)) {
            throw new IllegalArgumentException(String.format(BAirDrop.getConfigMessage().getMessage("effect-not-stated"), id));
        }
        IEffect ie = loadedEffect.get(id);
        ie.End();
        loadedEffect.remove(id);
    }

    @Override
    public void StopAllEffects() {
        for (IEffect ie : loadedEffect.values())
            ie.End();
    }

    @Override
    public HashMap<String, IEffect> getLoadedEffect() {
        return loadedEffect;
    }

    @Override
    public void setLoadedEffect(HashMap<String, IEffect> loadedEffect) {
        this.loadedEffect = loadedEffect;
    }

    @Override
    public void schematicsUndo() {
        if (savedBlocksData != null && !savedBlocksData.isEmpty()) {
            savedBlocksData.restore();
            savedBlocksData = null;
        }
        if (editSession != null) {
            editSession.close();
            editSession = null;
        }
    }

    @Override
    public String getGeneratorSettings() {
        return generatorSettings == null ? "default" : generatorSettings;
    }

    @Override
    public void setEditSession(EditSession editSession) {
        this.editSession = editSession;
    }

    @Override
    public org.by1337.bairdrop.worldGuardHook.SavedBlocksData getSavedBlocksData() {
        return savedBlocksData;
    }

    @Override
    public void setSavedBlocksData(org.by1337.bairdrop.worldGuardHook.SavedBlocksData savedBlocksData) {
        this.savedBlocksData = savedBlocksData;
    }

    @Override
    public AirDrop clone(String id) {
        CAirDrop air = new CAirDrop(fileConfiguration, airDropFile);
        air.setId(id);
        for (Observer observer : observers)
            air.registerObserver(observer);
        air.setGenerator(generator);
        air.setSuperName(this.id);
        return air;
    }


    @Override
    public void createFile() {
        File air = new File(getInstance().getDataFolder() + File.separator + "airdrops" + File.separator + getId() + ".yml");
        this.airDropFile = air;
        this.fileConfiguration = YamlConfiguration.loadConfiguration(air);
    }

    @Override
    public boolean delete() {
        File air = new File(getInstance().getDataFolder() + File.separator + "airdrops" + File.separator + getId() + ".yml");
        return air.delete();
    }

    @Override
    public void unload() {
        if (airDropStarted) {
            End();
        }
        notifyObservers(CustomEvent.UNLOAD, null);
        airDrops.remove(getId());
    }

    @Override
    @Nullable
    public Location getAnyLoc() {
        if (airDropLocation == null) {
            if (futureLocation == null)
                return null;
            else
                return futureLocation.clone();
        } else {
            return airDropLocation.clone();
        }
    }

    @Override
    public boolean isUseOnlyStaticLoc() {
        return useOnlyStaticLoc;
    }

    @Override
    public void setUseOnlyStaticLoc(boolean useOnlyStaticLoc) {
        this.useOnlyStaticLoc = useOnlyStaticLoc;
    }

    @Override
    public void registerAllSignedObservers() {
        List<String> list = new ArrayList<>(signedListener);
        for (String listener : list) {
            NamespacedKey nKey = NamespacedKey.fromString(listener);
            if (customEventListeners.containsKey(nKey)) {
                Observer observer = customEventListeners.get(nKey);
                if (!this.hasObserver(observer)) {
                    this.registerObserver(observer);
                } else {
                    Message.warning("the observer: " + observer.getKey().getKey() + " is already subscribed to " + this.getId());
                }
            } else {
                Message.warning("unknown observer: " + listener);
            }
        }
    }

    @Override
    public void stateSerialize() {
        Map<String, Object> map = new HashMap<>();
        map.put("version", 1);
        map.put("timeToStart", timeToStart);
        map.put("timeToOpen", timeToOpen);
        map.put("timeStop", timeStop);

        map.put("airDropLocked", airDropLocked);
        map.put("wasOpened", wasOpened);
        map.put("airDropStarted", airDropStarted);
        map.put("activated", activated);

        map.put("airDropLocation", airDropLocation);

        for (int x = 0; x < inventory.getSize(); x++) {
            ItemStack itemStack = inventory.getItem(x);
            if (itemStack == null) continue;
            map.put("item-" + x, itemStack);
        }

     //   List<Object> effects = new ArrayList<>();

        Map<String, Object> effects = new HashMap<>();

        for (String key : loadedEffect.keySet()) {

            IEffect effect = loadedEffect.get(key);
            if (!effect.isUsed()) continue;
            if (effect instanceof EffectSerializable effectSerializable) {
                effects.put(key, effectSerializable.serialize());
            }
        }
        map.put("effects", effects);
//        Map<String, Object> mm = new HashMap<>();
//        for (String key : map.keySet()){
//            mm.put(key, map.get(key));
//        }
     //   mm.put("oke", new ArrayList<>(effects));
//        mm.put("item", new ItemStack(Material.CREEPER_HEAD));
//        map.put("okeeeey", mm);
        fileConfiguration.set("state", map);
    }

    public static final int STATE_VERSION = 1;

    @Override
    public void stateDeserialize() {
        try {
            if (fileConfiguration.getConfigurationSection("state") == null) return;
            Map<String, Object> map = fileConfiguration.getConfigurationSection("state").getValues(false);

            int version = (int) map.get("version");
            if (version < STATE_VERSION) {
                Message.error("&cУстарелые данные! Невозможно загрузить состояние аирдропа");
                return;
            }
            int S_timeToStart = (int) map.get("timeToStart");
            int S_timeToOpen = (int) map.get("timeToOpen");
            int S_timeStop = (int) map.get("timeStop");

            boolean S_airDropLocked = (boolean) map.get("airDropLocked");
            boolean S_wasOpened = (boolean) map.get("wasOpened");
            boolean S_airDropStarted = (boolean) map.get("airDropStarted");
            boolean S_activated = (boolean) map.get("activated");

            Location S_airDropLocation = (Location) map.get("airDropLocation");


            for (String key : map.keySet()) {
                if (key.contains("item-")) {
                    ItemStack itemStack = (ItemStack) map.get(key);
                    inventory.setItem(Integer.parseInt(key.replace("item-", "")), itemStack);
                }
            }
            timeToStart = S_timeToStart;
            timeToOpen = S_timeToOpen;
            timeStop = S_timeStop;

            wasOpened = S_wasOpened;
            activated = S_activated;
            if (S_airDropLocation != null) {
                airDropLocation = S_airDropLocation;
                if (S_airDropStarted) {

                    RegionManager.SetRegion(this);
                    timeToStart = 0;
                    futureLocation = null;
                    stopWhenEmpty_event = false;


                    airDropLocation.getBlock().setType(materialLocked);
                    if (materialLocked == Material.RESPAWN_ANCHOR) {
                        RespawnAnchor ra = (RespawnAnchor) airDropLocation.getBlock().getBlockData();
                        ra.setCharges(4);
                        airDropLocation.getBlock().setBlockData(ra);
                    }


                    airDropStarted = true;
                    autoActivateTimer = timeToOpen;
                    updateEditAirMenu("stats");
                    if (antiSteal != null) antiSteal.unregister();
                    antiSteal = new AntiSteal(this);

                    if (!S_airDropLocked) {
                        airDropLocked = false;
                        timeToOpen = 0;

                        airDropLocation.getBlock().setType(materialUnlocked);
                        if (materialUnlocked == Material.RESPAWN_ANCHOR) {
                            RespawnAnchor ra = (RespawnAnchor) airDropLocation.getBlock().getBlockData();
                            ra.setCharges(4);
                            airDropLocation.getBlock().setBlockData(ra);
                        }


                        List<String> lines = new ArrayList<>(airHoloOpen);
                        lines.replaceAll(this::replaceInternalPlaceholder);
                        if (hologramType != null) {
                            HologramManager.createOrUpdateHologram(lines, airDropLocation.clone().add(holoOffsets), id, hologramType, hologramSettings);
                        }
                    }
                }
                Map<String, Object> effects = ((ConfigurationSection) map.get("effects")).getValues(false);
                for (String key : effects.keySet()) {
                    Map<String, Object> ef = ((ConfigurationSection) effects.get(key)).getValues(false);
                    IEffect effect = EffectDeserialize.deserialize(ef);
                    if (effect == null){
                        Message.error(key + " Не получилось десериализовать эффект");
                        continue;
                    }
                    loadedEffect.put(key, effect);
                }
            }
            notifyObservers(CustomEvent.DESERIALIZE, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setRegion() {
        RegionManager.SetRegion(this);
    }

    @Override
    public boolean isHoloTimeToStartEnabled() {
        return holoTimeToStartEnabled;
    }

    @Override
    public void setHoloTimeToStartEnabled(boolean holoTimeToStartEnabled) {
        this.holoTimeToStartEnabled = holoTimeToStartEnabled;
    }

    @Override
    public boolean isUsePlayerLocation() {
        return usePlayerLocation;
    }

    @Override
    public void setUsePlayerLocation(boolean usePlayerLocation) {
        this.usePlayerLocation = usePlayerLocation;
    }

    @Override
    public boolean isStopWhenEmpty() {
        return stopWhenEmpty;
    }

    @Override
    public void setStopWhenEmpty(boolean stopWhenEmpty) {
        this.stopWhenEmpty = stopWhenEmpty;
    }

    @Override
    public boolean isSummoner() {
        return summoner;
    }

    @Override
    public void setSummoner(boolean summoner) {
        this.summoner = summoner;
    }

    @Override
    public boolean isHideInCompleter() {
        return hideInCompleter;
    }

    @Override
    public void setHideInCompleter(boolean hideInCompleter) {
        this.hideInCompleter = hideInCompleter;
    }

    @Override
    public HashMap<String, List<Items>> getListItems() {
        return listItems;
    }

    @Override
    public void setHoloTimeToStartMinusOffsets(boolean holoTimeToStartMinusOffsets) {
        this.holoTimeToStartMinusOffsets = holoTimeToStartMinusOffsets;
    }

    @Override
    public boolean isCanceled() {
        return canceled;
    }

    @Override
    public void setCanceled(boolean canceled) {
        this.canceled = canceled;
    }

    @Override
    public boolean isClone() {
        return clone;
    }

    @Override
    public void setClone(boolean clone) {
        this.clone = clone;
    }

    @Override
    public boolean isKill() {
        return kill;
    }

    @Override
    public void setKill(boolean kill) {
        this.kill = kill;
    }

    private void setId(String id) {
        this.id = id;
    }

    @Override
    public Location getStaticLocation() {
        return staticLocation;
    }

    @Override
    public void setStaticLocation(Location staticLocation) {
        this.staticLocation = staticLocation;
    }

    @Override
    public boolean isUseStaticLoc() {
        return useStaticLoc;
    }

    @Override
    public void setUseStaticLoc(boolean useStaticLoc) {
        this.useStaticLoc = useStaticLoc;
    }

    @Override
    public int getSpawnChance() {
        return spawnChance;
    }

    @Override
    public void setTimeCountingEnabled(boolean timeCountingEnabled) {
        this.timeCountingEnabled = timeCountingEnabled;
    }

    @Override
    public boolean isTimeCountingEnabled() {
        return timeCountingEnabled;
    }

    @Override
    @Nullable
    public EditSession getEditSession() {
        return editSession;
    }

    @Override
    public void schematicsPaste(SchematicsManager manager, String name) {
        manager.PasteSchematics(name, this);
    }

    @Override
    public boolean isActivated() {
        return activated;
    }

    @Override
    public void setActivated(boolean activated) {
        this.activated = activated;
    }

    @Override
    public boolean isFlatnessCheck() {
        return flatnessCheck;
    }

    @Override
    public void setFlatnessCheck(boolean flatnessCheck) {
        this.flatnessCheck = flatnessCheck;
    }

    @Override
    public double getTimeToStartCons() {
        return timeToStartCons;
    }

    @Override
    public double getTimeToStopCons() {
        return timeToStopCons;
    }

    @Override
    public double getTimeToUnlockCons() {
        return timeToUnlockCons;
    }

    @Override
    public double getSearchBeforeStartCons() {
        return searchBeforeStartCons;
    }

    @Override
    public void setTimeToStartCons(double timeToStartCons) {
        this.timeToStartCons = timeToStartCons;
    }

    @Override
    public void setTimeToStopCons(double timeToStopCons) {
        this.timeToStopCons = timeToStopCons;
    }

    @Override
    public void setTimeToUnlockCons(double timeToUnlockCons) {
        this.timeToUnlockCons = timeToUnlockCons;
    }

    @Override
    public void setSearchBeforeStartCons(double searchBeforeStartCons) {
        this.searchBeforeStartCons = searchBeforeStartCons;
    }

    @Override
    public String getInventoryTitle() {
        return inventoryTitle;
    }

    @Override
    public void setInventoryTitle(String inventoryTitle) {
        this.inventoryTitle = inventoryTitle;
        inventory = Bukkit.createInventory(null, inventorySize, Message.messageBuilderComponent(inventoryTitle));
    }


    @Override
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String getEventListName() {
        return eventListName;
    }

    @Override
    public void setEventListName(String eventListName) {
        this.eventListName = eventListName;
    }

    @Override
    public int getInventorySize() {
        return inventorySize;
    }

    @Override
    @NotNull
    public World getWorld() {
        return world;
    }

    @Override
    public void setWorld(World world) {
        this.world = world;
    }

    @Override
    public int getSpawnRadiusMin() {
        return spawnRadiusMin;
    }

    @Override
    public void setSpawnRadiusMin(int spawnRadiusMin) {
        this.spawnRadiusMin = spawnRadiusMin;
    }

    @Override
    public int getSpawnRadiusMax() {
        return spawnRadiusMax;
    }

    @Override
    public void setSpawnRadiusMax(int spawnRadiusMax) {
        this.spawnRadiusMax = spawnRadiusMax;
    }

    @Override
    public int getRegionRadius() {
        return regionRadius;
    }

    @Override
    public void setRegionRadius(int regionRadius) {
        this.regionRadius = regionRadius;
    }

    @Override
    public int getTimeToStart() {
        return timeToStart;
    }

    @Override
    public void setTimeToStart(int timeToStart) {
        this.timeToStart = timeToStart;
    }

    @Override
    public int getSearchBeforeStart() {
        return searchBeforeStart;
    }

    @Override
    public void setSearchBeforeStart(int searchBeforeStart) {
        this.searchBeforeStart = searchBeforeStart;
    }

    @Override
    public int getTimeToOpen() {
        return timeToOpen;
    }

    @Override
    public void setTimeToOpen(int timeToOpen) {
        this.timeToOpen = timeToOpen;
    }

    @Override
    public int getAutoActivateTimer() {
        return autoActivateTimer;
    }

    @Override
    public boolean isStartCountdownAfterClick() {
        return startCountdownAfterClick;
    }

    @Override
    public void setStartCountdownAfterClick(boolean startCountdownAfterClick) {
        this.startCountdownAfterClick = startCountdownAfterClick;
    }

    @Override
    public boolean isTimeStopEventMustGo() {
        return timeStopEventMustGo;
    }

    @Override
    public void setTimeStopEventMustGo(boolean timeStopEventMustGo) {
        this.timeStopEventMustGo = timeStopEventMustGo;
    }

    @Override
    public int getTimeStop() {
        return timeStop;
    }

    @Override
    public void setTimeStop(int timeStop) {
        this.timeStop = timeStop;
    }

    @Override
    public Material getMaterialLocked() {
        return materialLocked;
    }

    @Override
    public void setMaterialLocked(Material materialLocked) {
        this.materialLocked = materialLocked;
    }

    @Override
    public Material getMaterialUnlocked() {
        return materialUnlocked;
    }

    @Override
    public void setMaterialUnlocked(Material materialUnlocked) {
        this.materialUnlocked = materialUnlocked;
    }

    @Override
    public boolean isAirDropLocked() {
        return airDropLocked;
    }

    @Override
    public void setAirDropLocked(boolean airDropLocked) {
        this.airDropLocked = airDropLocked;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    @Override
    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    @Nullable
    public Location getAirDropLocation() {
        return airDropLocation;
    }

    @Override
    public void setAirDropLocation(Location airDropLocation) {
        this.airDropLocation = airDropLocation;
    }

    @Override
    @Nullable
    public Location getFutureLocation() {
        return futureLocation;
    }

    @Override
    public void setFutureLocation(Location futureLocation) {
        this.futureLocation = futureLocation;
    }

    @Override
    public FileConfiguration getFileConfiguration() {
        return fileConfiguration;
    }

    @Override
    public int getMinPlayersToStart() {
        return minPlayersToStart;
    }

    @Override
    public void setMinPlayersToStart(int minPlayersToStart) {
        this.minPlayersToStart = minPlayersToStart;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public boolean isWasOpened() {
        return wasOpened;
    }

    @Override
    public void setWasOpened(boolean wasOpened) {
        this.wasOpened = wasOpened;
    }

    @Override
    public boolean isAirDropStarted() {
        return airDropStarted;
    }

    @Override
    public Generator getGenerator() {
        return generator;
    }

    @Override
    public void setGenerator(Generator generator) {
        this.generator = generator;
    }

    public String getSuperName() {
        return superName;
    }

    public void setSuperName(String superName) {
        this.superName = superName;
    }
    @Override
    public String toString() {
        return this.getClass().getName()  + "{" +
                "inventoryTitle='" + inventoryTitle + '\'' +
                ", displayName='" + displayName + '\'' +
                ", inventorySize=" + inventorySize +
                ", world=" + world +
                ", spawnRadiusMin=" + spawnRadiusMin +
                ", spawnRadiusMax=" + spawnRadiusMax +
                ", regionRadius=" + regionRadius +
                ", timeToStart=" + timeToStart +
                ", searchBeforeStart=" + searchBeforeStart +
                ", timeToOpen=" + timeToOpen +
                ", startCountdownAfterClick=" + startCountdownAfterClick +
                ", timeStopEventMustGo=" + timeStopEventMustGo +
                ", timeStop=" + timeStop +
                ", materialLocked=" + materialLocked +
                ", materialUnlocked=" + materialUnlocked +
                ", airDropLocked=" + airDropLocked +
                ", airDropLocation=" + airDropLocation +
                ", futureLocation=" + futureLocation +
                ", fileConfiguration=" + fileConfiguration +
                ", minPlayersToStart=" + minPlayersToStart +
                ", id='" + id + '\'' +
                ", wasOpened=" + wasOpened +
                ", airDropStarted=" + airDropStarted +
                ", activated=" + activated +
                ", timeToStartCons=" + timeToStartCons +
                ", timeToStopCons=" + timeToStopCons +
                ", timeToUnlockCons=" + timeToUnlockCons +
                ", searchBeforeStartCons=" + searchBeforeStartCons +
                ", flatnessCheck=" + flatnessCheck +
                ", staticLocation=" + staticLocation +
                ", useStaticLoc=" + useStaticLoc +
                ", spawnChance=" + spawnChance +
                ", timeCountingEnabled=" + timeCountingEnabled +
                ", generatorSettings='" + generatorSettings + '\'' +
                ", holoOffsets=" + holoOffsets +
                ", canceled=" + canceled +
                ", clone=" + clone +
                ", kill=" + kill +
                ", holoTimeToStartEnabled=" + holoTimeToStartEnabled +
                ", holoTimeToStartMinusOffsets=" + holoTimeToStartMinusOffsets +
                ", usePlayerLocation=" + usePlayerLocation +
                ", stopWhenEmpty=" + stopWhenEmpty +
                ", stopWhenEmpty_event=" + stopWhenEmpty_event +
                ", summoner=" + summoner +
                ", randomizeSlot=" + randomizeSlot +
                ", hideInCompleter=" + hideInCompleter +
                ", useOnlyStaticLoc=" + useOnlyStaticLoc +
                ", superName='" + superName + '\'' +
                '}';
    }
    public void addDec(String... s){
        dec.addAll(List.of(s));
    }

    public List<String> getDec() {
        return dec;
    }

    @Override
    public boolean isDecoyProtectionEnabled() {
        return decoyProtectionEnabled;
    }

    @Override
    public void setDecoyProtectionEnabled(boolean enabled) {
        this.decoyProtectionEnabled = enabled;
    }

    @Override
    public List<String> getDecoyFakeItems() {
        return decoyFakeItems;
    }

    @Override
    public void setDecoyFakeItems(List<String> items) {
        this.decoyFakeItems = items;
    }

    @Override
    public List<String> getDecoyFakeNames() {
        return decoyFakeNames;
    }

    @Override
    public void setDecoyFakeNames(List<String> names) {
        this.decoyFakeNames = names;
    }

    @Override
    public DecoyManager getDecoyManager() {
        return decoyManager;
    }

    @Override
    public void setDecoyManager(DecoyManager decoyManager) {
        this.decoyManager = decoyManager;
    }

    @Override
    public AntiSteal getAntiSteal() {
        return antiSteal;
    }

    @Override
    public HologramType getHologramType() {
        return hologramType;
    }

    @Override
    public void setHologramType(HologramType type) {
        this.hologramType = type;
    }

    @Override
    public HologramSettings getHologramSettings() {
        return hologramSettings;
    }

    @Override
    public boolean isTopLooterGlowEnabled() {
        return topLooterGlowEnabled;
    }

    @Override
    public void setTopLooterGlowEnabled(boolean enabled) {
        this.topLooterGlowEnabled = enabled;
    }

    @Override
    public int getTopLooterGlowDuration() {
        return topLooterGlowDuration;
    }

    @Override
    public void setTopLooterGlowDuration(int duration) {
        this.topLooterGlowDuration = duration;
    }

    @Override
    public boolean isScheduledTimeEnabled() {
        return scheduledTimeEnabled;
    }

    @Override
    public void setScheduledTimeEnabled(boolean enabled) {
        this.scheduledTimeEnabled = enabled;
    }

    @Override
    public boolean isAutoActivateEnabled() {
        return autoActivateEnabled;
    }

    public List<String> getScheduledTimes() {
        return scheduledTimes;
    }

    public void setScheduledTimes(List<String> times) {
        this.scheduledTimes = times;
    }

    private boolean isScheduledTimeNow() {
        if (!scheduledTimeEnabled || scheduledTimes.isEmpty()) {
            return false;
        }
        if (Bukkit.getOnlinePlayers().size() < minPlayersToStart) {
            return false;
        }
        java.time.LocalTime now = java.time.LocalTime.now();
        String currentTime = String.format("%02d:%02d", now.getHour(), now.getMinute());
        if (scheduledTimes.contains(currentTime) && !currentTime.equals(lastScheduledTrigger)) {
            lastScheduledTrigger = currentTime;
            return true;
        }
        return false;
    }

}
