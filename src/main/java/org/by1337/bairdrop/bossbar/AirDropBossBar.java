package org.by1337.bairdrop.bossbar;

import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.by1337.bairdrop.AirDrop;
import org.by1337.bairdrop.util.Message;

public class AirDropBossBar {
    private final AirDrop airDrop;
    private BossBar bossBar;
    private boolean enabled;
    private String visibility;
    private int radius;
    private BarColor color;
    private BarStyle style;
    private String titleClosed;
    private String titleOpen;
    private String titleActivate;
    private String titleNotActivated;

    public AirDropBossBar(AirDrop airDrop) {
        this.airDrop = airDrop;
        this.enabled = false;
        this.visibility = "radius";
        this.radius = 50;
        this.color = BarColor.YELLOW;
        this.style = BarStyle.SOLID;
        this.titleClosed = "&e{id} &7| &f{time-to-open}";
        this.titleOpen = "&a{id} &7| &f{time-stop}";
        this.titleActivate = "&6{id} &7| &f{auto-activate-timer}";
        this.titleNotActivated = "&c{id} &7| &fНе активирован";
    }

    public void create() {
        if (!enabled || bossBar != null) return;
        bossBar = Bukkit.createBossBar("", color, style);
    }

    public void update() {
        if (!enabled || bossBar == null) return;
        if (!airDrop.isAirDropStarted()) {
            remove();
            return;
        }

        bossBar.removeAll();

        if ("global".equalsIgnoreCase(visibility)) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                bossBar.addPlayer(player);
            }
        } else if ("radius".equalsIgnoreCase(visibility) && airDrop.getAirDropLocation() != null) {
            for (Entity entity : airDrop.getAirDropLocation().getWorld().getNearbyEntities(
                    airDrop.getAirDropLocation(), radius, radius, radius)) {
                if (entity instanceof Player player) {
                    if (player.getLocation().distance(airDrop.getAirDropLocation()) <= radius) {
                        bossBar.addPlayer(player);
                    }
                }
            }
        }

        String title;
        double progress;

        if (airDrop.isAirDropLocked() && airDrop.isStartCountdownAfterClick() && !airDrop.isActivated()) {
            if (airDrop.isAutoActivateEnabled()) {
                title = airDrop.replaceInternalPlaceholder(titleActivate);
                int autoActivateTimer = airDrop.getAutoActivateTimer();
                int totalTime = airDrop.getTimeToOpen();
                progress = totalTime > 0 ? (double) autoActivateTimer / totalTime : 0;
            } else {
                title = airDrop.replaceInternalPlaceholder(titleNotActivated);
                progress = 1.0;
            }
        } else if (airDrop.isAirDropLocked()) {
            title = airDrop.replaceInternalPlaceholder(titleClosed);
            int timeToOpen = airDrop.getTimeToOpen();
            int totalTime = (int) (airDrop.getTimeToUnlockCons() * 60);
            progress = totalTime > 0 ? (double) timeToOpen / totalTime : 0;
        } else {
            title = airDrop.replaceInternalPlaceholder(titleOpen);
            int timeStop = airDrop.getTimeStop();
            int totalTime = (int) (airDrop.getTimeToStopCons() * 60);
            progress = totalTime > 0 ? (double) timeStop / totalTime : 0;
        }

        progress = Math.max(0, Math.min(1, progress));

        bossBar.setTitle(Message.messageBuilder(title));
        bossBar.setProgress(progress);
        bossBar.setColor(color);
        bossBar.setStyle(style);
    }

    public void remove() {
        if (bossBar != null) {
            bossBar.removeAll();
            bossBar = null;
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getVisibility() {
        return visibility;
    }

    public void setVisibility(String visibility) {
        this.visibility = visibility;
    }

    public int getRadius() {
        return radius;
    }

    public void setRadius(int radius) {
        this.radius = radius;
    }

    public BarColor getColor() {
        return color;
    }

    public void setColor(BarColor color) {
        this.color = color;
    }

    public BarStyle getStyle() {
        return style;
    }

    public void setStyle(BarStyle style) {
        this.style = style;
    }

    public String getTitleClosed() {
        return titleClosed;
    }

    public void setTitleClosed(String titleClosed) {
        this.titleClosed = titleClosed;
    }

    public String getTitleOpen() {
        return titleOpen;
    }

    public void setTitleOpen(String titleOpen) {
        this.titleOpen = titleOpen;
    }

    public String getTitleActivate() {
        return titleActivate;
    }

    public void setTitleActivate(String titleActivate) {
        this.titleActivate = titleActivate;
    }

    public String getTitleNotActivated() {
        return titleNotActivated;
    }

    public void setTitleNotActivated(String titleNotActivated) {
        this.titleNotActivated = titleNotActivated;
    }
}
