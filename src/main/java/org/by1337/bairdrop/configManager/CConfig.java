package org.by1337.bairdrop.configManager;

import org.bukkit.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.by1337.bairdrop.BAirDrop;
import org.by1337.bairdrop.ItemUtil.EnchantInfo;
import org.by1337.bairdrop.ItemUtil.EnchantMaterial;
import org.by1337.bairdrop.listeners.Compass;
import org.by1337.bairdrop.worldGuardHook.RegionManager;
import org.by1337.bairdrop.customListeners.CustomEvent;
import org.by1337.bairdrop.customListeners.CustomEventListener;
import org.by1337.bairdrop.customListeners.util.CustomEventListenerBuilder;
import org.by1337.bairdrop.menu.util.MenuItem;
import org.by1337.bairdrop.util.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.util.*;

import static org.by1337.bairdrop.BAirDrop.getInstance;
import static org.by1337.bairdrop.BAirDrop.summoner;
import static org.by1337.bairdrop.effect.LoadEffects.LoadEffect;


public class CConfig implements Config, ConfigMessage {
    private FileConfiguration listeners;
    private File fileListeners;
    private FileConfiguration effects;
    private File fileEffects;
    private FileConfiguration menu;
    private File fileMenu;
    private FileConfiguration schemConf;
    private File fileSchemConf;
    private FileConfiguration generatorSettings;
    private File fileGeneratorSettings;
    private HashMap<String, File> Schematics = new HashMap<>();
    private HashMap<File, FileConfiguration> airDrops = new HashMap<>();
    private FileConfiguration message;
    private File fileMessage;
    private boolean loaded;
    public HashMap<String, File> scripts = new HashMap<>();
    private String language = "en";

    public void loadConfiguration() {
        language = getInstance().getConfig().getString("language", "en");
        
        initLang();
        initGlobalConfigs();
        
        fileMenu = new File(getInstance().getDataFolder() + File.separator + "lang" + File.separator + language + File.separator + "menu.yml");
        if (!fileMenu.exists()) {
            fileMenu = new File(getInstance().getDataFolder() + File.separator + "lang" + File.separator + "en" + File.separator + "menu.yml");
        }
        menu = YamlConfiguration.loadConfiguration(fileMenu);

        fileGeneratorSettings = new File(getInstance().getDataFolder() + File.separator + "generators.yml");
        if (!fileGeneratorSettings.exists()) {
            getInstance().saveResource("generators.yml", false);
        }
        generatorSettings = YamlConfiguration.loadConfiguration(fileGeneratorSettings);

        File shemDir = new File(getInstance().getDataFolder() + File.separator + "schematics");
        if (!shemDir.exists()) {
            shemDir.mkdir();
        }

        fileSchemConf = new File(getInstance().getDataFolder() + File.separator + "schematics" + File.separator + "schemConfig.yml");
        if (!fileSchemConf.exists()) {
            copyResource("schematics/schemConfig.yml", fileSchemConf);
        }
        schemConf = YamlConfiguration.loadConfiguration(fileSchemConf);

        File[] shemFiles = shemDir.listFiles();
        if (shemFiles != null) {
            for (File shemFile : shemFiles) {
                if (shemFile.getAbsolutePath().equals(fileSchemConf.getAbsolutePath()))
                    continue;
                Message.debug("load schematics " + shemFile.getAbsolutePath(), LogLevel.LOW);
                Schematics.put(shemFile.getName(), shemFile);
            }
        }

        fileEffects = new File(getInstance().getDataFolder() + File.separator + "effects.yml");
        if (!fileEffects.exists()) {
            getInstance().saveResource("effects.yml", false);
        }
        effects = YamlConfiguration.loadConfiguration(fileEffects);

        fileListeners = new File(getInstance().getDataFolder() + File.separator + "lang" + File.separator + language + File.separator + "listeners.yml");
        if (!fileListeners.exists()) {
            fileListeners = new File(getInstance().getDataFolder() + File.separator + "lang" + File.separator + "en" + File.separator + "listeners.yml");
        }
        listeners = YamlConfiguration.loadConfiguration(fileListeners);

        fileMessage = new File(getInstance().getDataFolder() + File.separator + "lang" + File.separator + language + File.separator + "messages.yml");
        if (!fileMessage.exists()) {
            fileMessage = new File(getInstance().getDataFolder() + File.separator + "lang" + File.separator + "en" + File.separator + "messages.yml");
        }
        message = YamlConfiguration.loadConfiguration(fileMessage);

        File dir = new File(getInstance().getDataFolder() + File.separator + "airdrops");
        if (!dir.exists()) {
            dir.mkdir();
            getInstance().saveResource("airdrops" + File.separator + "default.yml", false);
        }
        File dir2 = new File(getInstance().getDataFolder() + File.separator + "scripts");//diamond.js
        if (!dir2.exists()) {
            dir2.mkdir();
            getInstance().saveResource("scripts" + File.separator + "diamond.js", true);
        }
        scripts.clear();
        for (File script : Arrays.stream(Objects.requireNonNull(dir2.listFiles())).toList()) {
            scripts.put(script.getName(), script);
            Message.debug("load " + script.getName(), LogLevel.LOW);
        }

        for (File airFile : Arrays.stream(Objects.requireNonNull(dir.listFiles())).toList()) {
            Message.debug("load " + airFile.getAbsolutePath(), LogLevel.LOW);
            FileConfiguration fc = YamlConfiguration.loadConfiguration(airFile);
            airDrops.put(airFile, fc);
        }
        BAirDrop.customEventListeners.clear();
        loadListeners();
        loadMenu();
        LoadEffect(effects);
        RegionManager.LoadFlags();
        loadEnchant();
        summoner.LoadSummoner();
        BAirDrop.compass = new Compass();
        BAirDrop.compass.loadItem();
        if (getInstance().getConfig().getBoolean("custom-crafts.enable"))
            loadCustomCraft();
        loaded = true;
    }

    public void loadMenu() {
        MenuItem.menuItemHashMap.clear();
        for (String tag : menu.getConfigurationSection("main").getKeys(false)) {
            try {
                String name = Objects.requireNonNull(menu.getString("main." + tag + ".name"));
                String material = Objects.requireNonNull(menu.getString("main." + tag + ".material"));
                int slot = menu.getInt("main." + tag + ".slot");
                List<String> lore = getListOrEmpty("main." + tag + ".lore", menu);
                List<String> LEFT = getListOrEmpty("main." + tag + ".commands." + "LEFT-CLICK", menu);
                List<String> SHIFT_LEFT = getListOrEmpty("main." + tag + ".commands." + "SHIFT_LEFT-CLICK", menu);
                List<String> RIGHT = getListOrEmpty("main." + tag + ".commands." + "RIGHT-CLICK", menu);
                List<String> SHIFT_RIGHT = getListOrEmpty("main." + tag + ".commands." + "SHIFT_RIGHT-CLICK", menu);
                List<String> MIDDLE = getListOrEmpty("main." + tag + ".commands." + "MIDDLE-CLICK", menu);
                List<String> DROP = getListOrEmpty("main." + tag + ".commands." + "DROP-CLICK", menu);
                new MenuItem(tag, name, lore, slot, LEFT, SHIFT_LEFT, RIGHT, SHIFT_RIGHT, MIDDLE, DROP, material);
            } catch (NullPointerException e) {
                Message.error(String.format(getMessage("menu-error"), tag));

            } catch (IllegalArgumentException e) {
                Message.error(String.format(getMessage("menu-mat-error"), tag));
            }
        }

    }

    public void loadCustomCraft() {
        if (getInstance().getConfig().getConfigurationSection("custom-crafts.crafts") == null) {
            Message.error(getMessage("craft-list-is-empty"));
            return;
        }
        main:
        for (String key : getInstance().getConfig().getConfigurationSection("custom-crafts.crafts").getKeys(false)) {
            try {
                String summoner = Objects.requireNonNull(getInstance().getConfig().getString(String.format("custom-crafts.crafts.%s.summoner", key)));
                if (!BAirDrop.summoner.getItems().containsKey(summoner)) {
                    Message.error(String.format(getMessage("craft-unknown-item"), summoner));
                    Message.error(String.format(getMessage("craft-skip"), key));
                    continue;
                }
                String top = Objects.requireNonNull(getInstance().getConfig().getString(String.format("custom-crafts.crafts.%s.slots.top", key)));
                String middle = Objects.requireNonNull(getInstance().getConfig().getString(String.format("custom-crafts.crafts.%s.slots.middle", key)));
                String bottom = Objects.requireNonNull(getInstance().getConfig().getString(String.format("custom-crafts.crafts.%s.slots.bottom", key)));
                List<String> call = getInstance().getConfig().getStringList(String.format("custom-crafts.crafts.%s.call", key));

                if (getInstance().getConfig().getConfigurationSection(String.format("custom-crafts.crafts.%s.ingredients", key)) == null) {
                    Message.error(getMessage("craft-ingredients-is-empty"));
                    continue;
                }
                HashMap<Character, Material> ingredients = new HashMap<>();
                for (String ingred : getInstance().getConfig().getConfigurationSection(String.format("custom-crafts.crafts.%s.ingredients", key)).getKeys(false)) {
                    if (ingred.length() > 1) {
                        Message.error(ingred + " Может состоять только из одного символа!");
                        Message.error(String.format(getMessage("craft-skip"), key));
                        continue main;
                    }
                    try {
                        ingredients.put(ingred.charAt(0), Material.valueOf(Objects.requireNonNull(getInstance().getConfig().getString(String.format("custom-crafts.crafts.%s.ingredients.%s", key, ingred)))));
                    } catch (IllegalArgumentException e) {
                        Message.error(getInstance().getConfig().getString(String.format("custom-crafts.crafts.%s.ingredients.%s", key, ingred)) + " Неизвестный материал!");
                        Message.error(String.format(getMessage("craft-skip"), key));
                        continue main;
                    }
                }
                BAirDrop.crafts.put(key, new CustomCraft(key, summoner, call, ingredients, top, middle, bottom));
            } catch (NullPointerException e) {
                Message.error(String.format(getMessage("craft-load-error"), key));
                e.printStackTrace();
            } catch (Exception e) {
                Message.error(String.format(getMessage("craft-load-error"), key));
                e.printStackTrace();
            }
        }
    }

    public void loadListeners() {
        if (listeners.getConfigurationSection("listeners") == null) {
            Message.error(getMessage("list-listeners-is-empty"));
            return;
        }
        for (String key : listeners.getConfigurationSection("listeners").getKeys(false)) {
            try {
                CustomEvent customEvent = CustomEvent.getByKey(NamespacedKey.fromString(Objects.requireNonNull(listeners.getString("listeners." + key + ".event"), key + " event is null").toLowerCase()));
                if (customEvent == null) {
                    Message.error("Незарегистрированный ивент! " + listeners.getString("listeners." + key + ".event"));
                    continue;
                }
                List<String> commands = listeners.getStringList("listeners." + key + ".commands");
                List<String> denyCommands = listeners.getStringList("listeners." + key + ".deny-commands");
                String description = Objects.requireNonNull(listeners.getString("listeners." + key + ".description"), key + " description is null");
                HashMap<String, HashMap<String, String>> requirement = new HashMap<>();
                if (listeners.getConfigurationSection("listeners." + key + ".requirement") != null) {
                    for (String checkId : listeners.getConfigurationSection("listeners." + key + ".requirement").getKeys(false)) {
                        String checkType = Objects.requireNonNull(listeners.getString("listeners." + key + ".requirement." + checkId + ".type"), key + " requirement type is null");
                        String input = Objects.requireNonNull(listeners.getString("listeners." + key + ".requirement." + checkId + ".input"), key + " requirement input is null");
                        HashMap<String, String> check = new HashMap<>();
                        check.put(checkType, input);
                        requirement.put(checkId, check);
                    }
                }
                CustomEventListener customEventListener = new CustomEventListenerBuilder()
                        .setCustomEvent(customEvent)
                        .setCommands(commands.toArray(new String[0]))
                        .setDenyCommands(denyCommands.toArray(new String[0]))
                        .setDescription(description).setRequirement(requirement)
                        .setKey(NamespacedKey.fromString(key.toLowerCase()))
                        .build();


                BAirDrop.customEventListeners.put(NamespacedKey.fromString(key.toLowerCase()), customEventListener);
            } catch (NullPointerException e) {
                Message.error(getMessage("listeners-error"));
                e.printStackTrace();
            } catch (IllegalArgumentException e) {
                Message.error(String.format(getMessage("unknown-event"), key));
                e.printStackTrace();
            }
        }
    }

    public void loadEnchant() {
        EnchantMaterial.materialHashMap.clear();
        if (!getInstance().getConfig().getBoolean("auto-enchant.enable")) return;
        for (String id : getInstance().getConfig().getConfigurationSection("auto-enchant").getKeys(false)) {
            if (id.equals("enable")) continue;

            if (getInstance().getConfig().getConfigurationSection("auto-enchant." + id) == null) continue;
            Material material1 = Material.DIRT;
            try {
                material1 = Material.valueOf(getInstance().getConfig().getString(String.format("auto-enchant.%s.material", id)));
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            }
            List<EnchantInfo> enchantInfos = new ArrayList<>();
            List<Enchantment> conflictEnchantments = new ArrayList<>();
            for (String enchant : getInstance().getConfig().getConfigurationSection("auto-enchant." + id).getKeys(false)) {
                if (enchant.equals("material")) continue;
                try {
                    if (enchant.equals("conflict-enchant")) {
                        for (String str : getInstance().getConfig().getStringList(String.format("auto-enchant.%s.%s", id, enchant))) {
                            Enchantment enchantment = Objects.requireNonNull(Enchantment.getByKey(NamespacedKey.fromString("minecraft:" + str)));
                            conflictEnchantments.add(enchantment);
                        }
                        continue;
                    }
                    Enchantment enchantment = Enchantment.getByKey(NamespacedKey.fromString(enchant));
                    if (enchantment == null) {
                        Message.error("Неизвестный чар: " + enchant);
                        continue;
                    }
                    int chance = getInstance().getConfig().getInt(String.format("auto-enchant.%s.%s.chance", id, enchant));
                    int minLevel = getInstance().getConfig().getInt(String.format("auto-enchant.%s.%s.min-level", id, enchant));
                    int maxLevel = getInstance().getConfig().getInt(String.format("auto-enchant.%s.%s.max-level", id, enchant));
                    //Message.debug(enchant + " = chance:" + chance + ", minLevel:" + minLevel + ", maxLevel:" + maxLevel);
                    enchantInfos.add(new EnchantInfo(chance, minLevel, maxLevel, enchantment));
                } catch (NullPointerException e) {
                    e.printStackTrace();
                }
            }
            EnchantMaterial.materialHashMap.put(id, new EnchantMaterial(material1, conflictEnchantments, enchantInfos));
        }
    }


    public String getMessage(String path) {
        if (message.getString(path) == null) {
            String MessageFromPlugin = getMessageFromPlugin(path);
            if (MessageFromPlugin == null) {
                Message.error(path + " <- this path does not exist!");
                return Message.messageBuilder("&cСообщения с таким пути нет!, There are no messages with this path!");
            } else {
                message.set(path, MessageFromPlugin);
                try {
                    message.save(fileMessage);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return Message.messageBuilder(MessageFromPlugin);
            }

        }
        return Message.messageBuilder(message.getString(path));
    }

    @Nullable
    public String getMessageFromPlugin(String path) {
        InputStream resourceStream = getInstance().getResource("lang/" + language + "/messages.yml");
        if (resourceStream == null) {
            resourceStream = getInstance().getResource("lang/en/messages.yml");
        }
        if (resourceStream == null) {
            return null;
        }
        File tempFile;
        try {
            tempFile = File.createTempFile("messages", ".yml");
        } catch (IOException e) {
            return null;
        }
        try (FileOutputStream fos = new FileOutputStream(tempFile)) {
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = resourceStream.read(buffer)) != -1) {
                fos.write(buffer, 0, bytesRead);
            }
        } catch (IOException e) {
            return null;
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(tempFile);
        tempFile.delete();
        return config.getString(path);
    }

    public List<String> getList(String path) {
        if (message.getStringList(path).isEmpty()) {
            List<String> list = getListFromPlugin(path);
            if (!list.isEmpty()) {
                message.set(path, list);
                try {
                    message.save(fileMessage);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return list;

            } else {
                Message.error(path + " <- this path does not exist!");
                return Collections.singletonList(Message.messageBuilder("&cСообщения с таким пути нет!, There are no messages with this path!"));
            }
        }
        return new ArrayList<>(message.getStringList(path));
    }

    @NotNull
    public List<String> getListFromPlugin(String path) {
        InputStream resourceStream = getInstance().getResource("lang/" + language + "/messages.yml");
        if (resourceStream == null) {
            resourceStream = getInstance().getResource("lang/en/messages.yml");
        }
        if (resourceStream == null) {
            return new ArrayList<>();
        }
        File tempFile;
        try {
            tempFile = File.createTempFile("messages", ".yml");
        } catch (IOException e) {
            return new ArrayList<>();
        }
        try (FileOutputStream fos = new FileOutputStream(tempFile)) {
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = resourceStream.read(buffer)) != -1) {
                fos.write(buffer, 0, bytesRead);
            }
        } catch (IOException e) {
            return new ArrayList<>();
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(tempFile);
        tempFile.delete();
        return config.getStringList(path);
    }

    public List<String> getListOrEmpty(String path, FileConfiguration file) {
        if (file.getStringList(path).isEmpty()) {
            return new ArrayList<String>();
        }
        return new ArrayList<>(file.getStringList(path));
    }

    public HashMap<File, FileConfiguration> getAirDrops() {
        return airDrops;
    }

    public FileConfiguration getSchemConf() {
        return schemConf;
    }

    public FileConfiguration getListeners() {
        return listeners;
    }

    public File getFileListeners() {
        return fileListeners;
    }

    public FileConfiguration getEffects() {
        return effects;
    }

    public File getFileEffects() {
        return fileEffects;
    }

    public FileConfiguration getMenu() {
        return menu;
    }

    public File getFileMenu() {
        return fileMenu;
    }

    public File getFileSchemConf() {
        return fileSchemConf;
    }

    public File getFileGeneratorSettings() {
        return fileGeneratorSettings;
    }

    public HashMap<String, File> getSchematics() {
        return Schematics;
    }

    public FileConfiguration getMessage() {
        return message;
    }

    public File getFileMessage() {
        return fileMessage;
    }

    public boolean isLoaded() {
        return loaded;
    }

    public HashMap<String, File> getScripts() {
        return scripts;
    }

    public FileConfiguration getGeneratorSettings() {
        return generatorSettings;
    }

    private void initLang() {
        File langDir = new File(getInstance().getDataFolder() + File.separator + "lang");
        if (!langDir.exists()) {
            langDir.mkdirs();
        }
        
        String[] languages = {"en", "ru"};
        String[] langFiles = {"messages.yml", "menu.yml", "listeners.yml"};
        
        for (String lang : languages) {
            File dir = new File(langDir, lang);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            for (String fileName : langFiles) {
                File file = new File(dir, fileName);
                if (!file.exists()) {
                    copyResource("lang/" + lang + "/" + fileName, file);
                }
            }
        }
        Message.logger("[BAirDropS] Language: " + language);
    }
    
    private void initGlobalConfigs() {
        File effectsFile = new File(getInstance().getDataFolder() + File.separator + "effects.yml");
        if (!effectsFile.exists()) {
            getInstance().saveResource("effects.yml", false);
        }
        
        File generatorsFile = new File(getInstance().getDataFolder() + File.separator + "generators.yml");
        if (!generatorsFile.exists()) {
            getInstance().saveResource("generators.yml", false);
        }
        
        File schematicsDir = new File(getInstance().getDataFolder() + File.separator + "schematics");
        if (!schematicsDir.exists()) {
            schematicsDir.mkdirs();
        }
        File schemConfigFile = new File(schematicsDir, "schemConfig.yml");
        if (!schemConfigFile.exists()) {
            copyResource("schematics/schemConfig.yml", schemConfigFile);
        }
    }
    
    private void copyResource(String resourcePath, File outFile) {
        InputStream in = getInstance().getResource(resourcePath);
        if (in != null) {
            try {
                if (outFile.getParentFile() != null && !outFile.getParentFile().exists()) {
                    outFile.getParentFile().mkdirs();
                }
                try (FileOutputStream out = new FileOutputStream(outFile)) {
                    byte[] buffer = new byte[1024];
                    int len;
                    while ((len = in.read(buffer)) != -1) {
                        out.write(buffer, 0, len);
                    }
                }
                in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public String getLanguage() {
        return language;
    }
}
