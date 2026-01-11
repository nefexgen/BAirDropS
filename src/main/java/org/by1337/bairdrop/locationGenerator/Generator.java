package org.by1337.bairdrop.locationGenerator;

import org.bukkit.Location;
import org.by1337.bairdrop.AirDrop;
import org.jetbrains.annotations.NotNull;

public interface Generator {
    /**
     * Main method for generating locations
     * @param airDrop The AirDrop for which to generate the location
     * @return Generated location
     */
    Location getLocation(@NotNull AirDrop airDrop);

    /**
     * Checks the flatness of a location
     * @param location The location to check for flatness
     * @param airDrop The AirDrop whose settings will be used
     * @return Returns true if the location is flat, otherwise false
     */
    boolean checkForEvenness(@NotNull Location location, AirDrop airDrop);
}
