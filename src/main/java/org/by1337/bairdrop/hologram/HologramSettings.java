package org.by1337.bairdrop.hologram;

import org.bukkit.Color;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Display;
import org.bukkit.entity.TextDisplay;

public class HologramSettings {
    private boolean textShadow;
    private byte textOpacity;
    private Color backgroundColor;
    private int backgroundOpacity;
    private boolean seeThrough;
    private float viewRange;
    private float scale;
    private int brightness;
    private Display.Billboard billboard;
    private TextDisplay.TextAlignment textAlignment;
    private float yaw;
    private float pitch;

    public HologramSettings() {
        this.textShadow = true;
        this.textOpacity = (byte) -1;
        this.backgroundColor = Color.fromARGB(0, 0, 0, 0);
        this.backgroundOpacity = 0;
        this.seeThrough = false;
        this.viewRange = 32.0f;
        this.scale = 1.0f;
        this.brightness = 15;
        this.billboard = Display.Billboard.CENTER;
        this.textAlignment = TextDisplay.TextAlignment.CENTER;
        this.yaw = 0.0f;
        this.pitch = 0.0f;
    }

    public static HologramSettings fromConfig(FileConfiguration config) {
        HologramSettings settings = new HologramSettings();
        
        if (config.contains("holo-settings")) {
            settings.textShadow = config.getBoolean("holo-settings.text-shadow", true);
            int textOpacityPercent = config.getInt("holo-settings.text-opacity", 100);
            settings.textOpacity = (byte) (textOpacityPercent * 255 / 100);
            
            String bgColorHex = config.getString("holo-settings.background-color", "#000000");
            settings.backgroundColor = parseColor(bgColorHex);
            settings.backgroundOpacity = config.getInt("holo-settings.background-opacity", 0);
            
            int alpha = (int) (settings.backgroundOpacity * 255 / 100);
            settings.backgroundColor = Color.fromARGB(
                alpha,
                settings.backgroundColor.getRed(),
                settings.backgroundColor.getGreen(),
                settings.backgroundColor.getBlue()
            );
            
            settings.seeThrough = config.getBoolean("holo-settings.see-through", false);
            settings.viewRange = (float) config.getDouble("holo-settings.view-range", 32.0);
            settings.scale = (float) config.getDouble("holo-settings.scale", 1.0);
            settings.brightness = config.getInt("holo-settings.brightness", 15);
            
            String billboardStr = config.getString("holo-settings.billboard", "CENTER").toUpperCase();
            try {
                settings.billboard = Display.Billboard.valueOf(billboardStr);
            } catch (IllegalArgumentException e) {
                settings.billboard = Display.Billboard.CENTER;
            }
            
            String alignmentStr = config.getString("holo-settings.text-alignment", "CENTER").toUpperCase();
            try {
                settings.textAlignment = TextDisplay.TextAlignment.valueOf(alignmentStr);
            } catch (IllegalArgumentException e) {
                settings.textAlignment = TextDisplay.TextAlignment.CENTER;
            }
            
            settings.yaw = (float) config.getDouble("holo-settings.yaw", 0.0);
            settings.pitch = (float) config.getDouble("holo-settings.pitch", 0.0);
        }
        
        return settings;
    }

    private static Color parseColor(String hex) {
        if (hex == null || hex.isEmpty()) {
            return Color.fromRGB(0, 0, 0);
        }
        hex = hex.replace("#", "");
        try {
            int rgb = Integer.parseInt(hex, 16);
            return Color.fromRGB((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF);
        } catch (NumberFormatException e) {
            return Color.fromRGB(0, 0, 0);
        }
    }

    public boolean isTextShadow() { return textShadow; }
    public byte getTextOpacity() { return textOpacity; }
    public Color getBackgroundColor() { return backgroundColor; }
    public int getBackgroundOpacity() { return backgroundOpacity; }
    public boolean isSeeThrough() { return seeThrough; }
    public float getViewRange() { return viewRange; }
    public float getScale() { return scale; }
    public int getBrightness() { return brightness; }
    public Display.Billboard getBillboard() { return billboard; }
    public TextDisplay.TextAlignment getTextAlignment() { return textAlignment; }
    public float getYaw() { return yaw; }
    public float getPitch() { return pitch; }
}
