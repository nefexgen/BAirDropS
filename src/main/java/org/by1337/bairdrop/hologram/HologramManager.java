package org.by1337.bairdrop.hologram;

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.TextDisplay;
import org.bukkit.util.Transformation;
import org.by1337.bairdrop.util.Message;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HologramManager {
    private static final Map<String, HologramData> holograms = new HashMap<>();
    private static final double ARMORSTAND_LINE_HEIGHT = 0.3D;

    public static void createOrUpdateHologram(List<String> lines, Location location, String id, HologramType type, HologramSettings settings) {
        if (location == null || location.getWorld() == null || type == null) {
            return;
        }

        HologramData data = holograms.get(id);
        boolean chunkLoaded = location.getWorld().isChunkLoaded(location.getBlockX() >> 4, location.getBlockZ() >> 4);

        if (data != null) {
            if (data.type != type) {
                removeEntities(data);
                data.entities.clear();
                data.type = type;
            }

            data.location = location.clone();
            data.settings = settings;
            data.lines = new ArrayList<>(lines);

            if (!chunkLoaded) {
                return;
            }

            if (type == HologramType.TEXTDISPLAY) {
                updateTextDisplay(data, lines, location, settings);
            } else {
                updateArmorStands(data, lines, location);
            }
        } else {
            data = new HologramData(id, type, location.clone(), settings, lines);
            holograms.put(id, data);

            if (!chunkLoaded) {
                return;
            }

            if (type == HologramType.TEXTDISPLAY) {
                createTextDisplay(data, lines, location, settings);
            } else {
                createArmorStands(data, lines, location);
            }
        }
    }

    private static void createTextDisplay(HologramData data, List<String> lines, Location location, HologramSettings settings) {
        Location spawnLoc = location.clone();
        if (settings != null && settings.getBillboard() == Display.Billboard.FIXED) {
            spawnLoc.setYaw(settings.getYaw());
            spawnLoc.setPitch(settings.getPitch());
        }
        
        TextDisplay display = spawnLoc.getWorld().spawn(spawnLoc, TextDisplay.class, entity -> {
            entity.setText(buildText(lines));
            entity.setPersistent(false);

            if (settings != null) {
                entity.setBillboard(settings.getBillboard());
                entity.setAlignment(settings.getTextAlignment());
                entity.setSeeThrough(settings.isSeeThrough());
                entity.setShadowed(settings.isTextShadow());
                entity.setTextOpacity(settings.getTextOpacity());
                entity.setDefaultBackground(false);
                entity.setBackgroundColor(settings.getBackgroundColor());
                entity.setLineWidth(Integer.MAX_VALUE);
                entity.setViewRange(settings.getViewRange() / 64.0f);

                float s = settings.getScale();
                entity.setTransformation(new Transformation(
                    new Vector3f(0, 0, 0),
                    new AxisAngle4f(0, 0, 0, 1),
                    new Vector3f(s, s, s),
                    new AxisAngle4f(0, 0, 0, 1)
                ));

                int brightness = settings.getBrightness();
                if (brightness >= 0 && brightness <= 15) {
                    entity.setBrightness(new Display.Brightness(brightness, brightness));
                }
            } else {
                entity.setBillboard(Display.Billboard.CENTER);
                entity.setAlignment(TextDisplay.TextAlignment.CENTER);
            }
        });
        data.entities.add(display);
    }

    private static void updateTextDisplay(HologramData data, List<String> lines, Location location, HologramSettings settings) {
        if (data.entities.isEmpty() || !(data.entities.get(0) instanceof TextDisplay)) {
            removeEntities(data);
            data.entities.clear();
            createTextDisplay(data, lines, location, settings);
            return;
        }

        TextDisplay display = (TextDisplay) data.entities.get(0);
        if (!display.isValid()) {
            data.entities.clear();
            createTextDisplay(data, lines, location, settings);
            return;
        }

        display.setText(buildText(lines));
        
        Location teleportLoc = location.clone();
        if (settings != null && settings.getBillboard() == Display.Billboard.FIXED) {
            teleportLoc.setYaw(settings.getYaw());
            teleportLoc.setPitch(settings.getPitch());
        }
        display.teleport(teleportLoc);
    }

    private static void createArmorStands(HologramData data, List<String> lines, Location location) {
        for (int i = 0; i < lines.size(); i++) {
            Location lineLoc = location.clone().add(0, -i * ARMORSTAND_LINE_HEIGHT, 0);
            ArmorStand stand = location.getWorld().spawn(lineLoc, ArmorStand.class, entity -> {
                entity.setVisible(false);
                entity.setGravity(false);
                entity.setMarker(true);
                entity.setSmall(true);
                entity.setCustomNameVisible(true);
                entity.setPersistent(false);
                entity.setInvulnerable(true);
                entity.setCollidable(false);
            });
            stand.customName(LegacyComponentSerializer.legacySection().deserialize(Message.messageBuilder(lines.get(i))));
            data.entities.add(stand);
        }
    }

    private static void updateArmorStands(HologramData data, List<String> lines, Location location) {
        List<Entity> validStands = new ArrayList<>();
        for (Entity e : data.entities) {
            if (e instanceof ArmorStand && e.isValid()) {
                validStands.add(e);
            }
        }

        if (validStands.size() != lines.size()) {
            removeEntities(data);
            data.entities.clear();
            createArmorStands(data, lines, location);
            return;
        }

        for (int i = 0; i < lines.size(); i++) {
            ArmorStand stand = (ArmorStand) validStands.get(i);
            Location lineLoc = location.clone().add(0, -i * ARMORSTAND_LINE_HEIGHT, 0);
            stand.teleport(lineLoc);
            stand.customName(LegacyComponentSerializer.legacySection().deserialize(Message.messageBuilder(lines.get(i))));
        }
        data.entities = validStands;
    }

    private static String buildText(List<String> lines) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lines.size(); i++) {
            sb.append(Message.messageBuilder(lines.get(i)));
            if (i < lines.size() - 1) {
                sb.append("\n");
            }
        }
        return sb.toString();
    }

    private static void removeEntities(HologramData data) {
        for (Entity entity : data.entities) {
            if (entity != null && entity.isValid()) {
                entity.remove();
            }
        }
    }

    public static void remove(String id) {
        HologramData data = holograms.remove(id);
        if (data != null) {
            removeEntities(data);
        }
    }

    public static void removeAll() {
        for (HologramData data : holograms.values()) {
            removeEntities(data);
        }
        holograms.clear();
    }

    public static void onChunkLoad(org.bukkit.Chunk chunk) {
        for (HologramData data : holograms.values()) {
            if (data.location == null || data.location.getWorld() == null) continue;
            if (!data.location.getWorld().equals(chunk.getWorld())) continue;
            int chunkX = data.location.getBlockX() >> 4;
            int chunkZ = data.location.getBlockZ() >> 4;
            if (chunkX == chunk.getX() && chunkZ == chunk.getZ()) {
                if (data.entities.isEmpty() || data.entities.stream().noneMatch(Entity::isValid)) {
                    data.entities.clear();
                    if (data.type == HologramType.TEXTDISPLAY) {
                        createTextDisplay(data, data.lines, data.location, data.settings);
                    } else {
                        createArmorStands(data, data.lines, data.location);
                    }
                }
            }
        }
    }

    private static class HologramData {
        String id;
        HologramType type;
        Location location;
        HologramSettings settings;
        List<String> lines = new ArrayList<>();
        List<Entity> entities = new ArrayList<>();

        HologramData(String id, HologramType type, Location location, HologramSettings settings, List<String> lines) {
            this.id = id;
            this.type = type;
            this.location = location;
            this.settings = settings;
            this.lines = new ArrayList<>(lines);
        }
    }
}
