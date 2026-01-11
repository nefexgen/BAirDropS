package org.by1337.bairdrop.hologram.utils;

import org.bukkit.Location;
import org.by1337.bairdrop.hologram.utils.impl.HoloLineV1_21;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class HoloManager {
    private final List<HoloLine> lines = new ArrayList<>();
    private final String name;
    private final Location location;

    public HoloManager(Location location, List<String> lines, String name) {
        this.location = location.clone();
        this.location.add(0, -2, 0);
        this.name = name;
        double offset = 0D;
        for (String str : lines) {
            create(str, this.location.clone().add(0, -offset, 0));
            offset += 0.3D;
        }
    }

    public void remove() {
        lines.forEach(HoloLine::remove);
        lines.clear();
    }

    public void setLines(List<String> newLines) { //line 33
        Iterator<HoloLine> holos = lines.listIterator();

        double offset = 0D;
        boolean hasNew = false;
        for (String str : newLines) {
            if (holos.hasNext()) {
                HoloLine holoLine = holos.next();
                holoLine.updateName(str);
            } else {
                create(str, location.clone().add(0, -offset, 0));
                hasNew = true;
            }
            offset += 0.3D;
        }
        while (holos.hasNext() && !hasNew) {
            HoloLine holoLine = holos.next();
            holoLine.remove();
            holos.remove();
        }
    }

    private void create(String line, Location location) {
        HoloLine holoLine = new HoloLineV1_21(line, location);
        holoLine.spawn();
        lines.add(holoLine);
    }
    public String getName() {
        return name;
    }

    public Location getLocation() {
        return location;
    }

}
