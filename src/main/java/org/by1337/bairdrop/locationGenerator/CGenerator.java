package org.by1337.bairdrop.locationGenerator;

import org.bukkit.*;
import org.bukkit.block.Biome;
import org.by1337.bairdrop.AirDrop;
import org.by1337.bairdrop.util.Message;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import org.by1337.bairdrop.BAirDrop;

public class CGenerator implements Generator{
    private static final HashMap<String, Long> cd = new HashMap<>();
    private World world;

    @Override
    @Nullable
    public Location getLocation(@NotNull AirDrop airDrop) {
        world = airDrop.getWorld();

        double x = ThreadLocalRandom.current().nextInt(airDrop.getSpawnRadiusMin(), airDrop.getSpawnRadiusMax());
        double y = 100;//100
        double z = ThreadLocalRandom.current().nextInt(airDrop.getSpawnRadiusMin(), airDrop.getSpawnRadiusMax());

        Location loc1 = new Location(world, x, y, z);

        String worldType = String.valueOf(world.getEnvironment());
        if (worldType.equals("NORMAL"))
            return getLocation_NORMAL(loc1, airDrop);
        if (worldType.equals("THE_END"))
            return getLocation_THE_END(loc1, airDrop);
        if (worldType.equals("NETHER")) {
            if (cd.getOrDefault(airDrop.getSuperName() + "002", 0L) < System.currentTimeMillis()) {
                Message.warning(String.format(BAirDrop.getConfigMessage().getMessage("generation-nether"), airDrop.getSuperName()));
                cd.put(airDrop.getSuperName() + "002", System.currentTimeMillis() + 150000L);
            }
            return getLocation_NETHER(loc1, airDrop);
        }
        if (worldType.equals("CUSTOM")) {
            if (cd.getOrDefault(airDrop.getSuperName() + "003", 0L) < System.currentTimeMillis()) {
                Message.warning(String.format(BAirDrop.getConfigMessage().getMessage("unknown-world-type"), world.getName()));
                cd.put(airDrop.getSuperName() + "003", System.currentTimeMillis() + 150000L); //а это 003
            }
            return getLocation_NORMAL(loc1, airDrop);

        }
        return null;
    }


    @Nullable
    private Location getLocation_NORMAL(@NotNull Location location, AirDrop airDrop) {
        Chunk chunk = location.getChunk();
        Random random = new Random();

        int x = random.nextInt(16);
        int z = random.nextInt(16);
        int y = getHighestBlock(chunk, x, z, GeneratorUtils.getSettings(airDrop.getGeneratorSettings(), "world-NORMAL.max-y"));

        if(y == -1)
            return null;

        location = chunk.getBlock(x, y, z).getLocation();
        if (isBiomeInBlackList(location)) return null;

        location.add(GeneratorUtils.getOffsets(airDrop));

        if (GeneratorUtils.isRegionEmpty(airDrop, location))
            if (airDrop.isFlatnessCheck()) {
                if (checkForEvenness(location, airDrop))
                    return location;
                return null;
            } else return location;

        return null;
    }

    private int getHighestBlock(Chunk chunk, int x, int z, int maxY){ //world-NORMAL.max-y
        boolean upBlockIsAir = false;
        for(int y = maxY; y > 30; y--){
            if(!chunk.getBlock(x, y, z).getType().isAir()){
                if (BAirDrop.getiConfig().getGeneratorSettings().getStringList("black-List").contains(String.valueOf(chunk.getBlock(x, y, z).getType()))) {
                    return -1;
                }
                if(upBlockIsAir)
                    return y;
                else
                    return -1;
            }else {
                upBlockIsAir = true;
            }
        }
        return -1;
    }


    @Nullable
    private Location getLocation_NETHER(@NotNull Location location, AirDrop airDrop) {
        Location loc = location.clone();
        loc.setY(GeneratorUtils.getSettings(airDrop.getGeneratorSettings(), "world-NETHER.check-lava"));
        if (loc.getBlock().getType() == Material.LAVA)
            return null;
        loc.setY(GeneratorUtils.getSettings(airDrop.getGeneratorSettings(), "world-NETHER.start-y") + 1);
        boolean upBlockIsAir = loc.getBlock().isEmpty();
        if (isBiomeInBlackList(location)) {
            return null;
        }
        for (int y = GeneratorUtils.getSettings(airDrop.getGeneratorSettings(), "world-NETHER.start-y"); y > GeneratorUtils.getSettings(airDrop.getGeneratorSettings(), "world-NETHER.end-y"); y--) {
            loc.setY(y);
            if (!loc.getBlock().getType().isAir())
                if (! BAirDrop.getiConfig().getGeneratorSettings().getStringList("black-List").contains(String.valueOf(loc.getBlock().getType()))) {
                    if (upBlockIsAir) {
                        if (GeneratorUtils.isRegionEmpty(airDrop, location)) {
                            if (checkMaxY(location, airDrop, "world-NETHER.max-y")) {
                                if (airDrop.isFlatnessCheck()) {
                                    if (checkForEvenness(loc, airDrop)) {
                                        return loc.add(GeneratorUtils.getOffsets(airDrop));
                                    } else {
                                        return null;
                                    }

                                } else
                                    return loc.add(GeneratorUtils.getOffsets(airDrop));
                            }
                        }
                    }
                }
            upBlockIsAir = loc.getBlock().getType().isAir() ||  BAirDrop.getiConfig().getGeneratorSettings().getStringList("ignored-blocks").contains(String.valueOf(location.getBlock().getType()));
        }
        return null;
    }

    @Nullable
    private Location getLocation_THE_END(@NotNull Location location, AirDrop airDrop) {

        location.setY(GeneratorUtils.getSettings(airDrop.getGeneratorSettings(), "world-THE_END.start-y"));
        if (!location.getBlock().isEmpty()) {
            location.setY(location.getWorld().getHighestBlockYAt(location));
            if ( BAirDrop.getiConfig().getGeneratorSettings().getStringList("black-List").contains(String.valueOf(location.getBlock().getType()))) {
                return null;
            }
            if (!checkMaxY(location, airDrop, "world-THE_END.max-y"))
                return null;
            location.add(GeneratorUtils.getOffsets(airDrop));
            if (GeneratorUtils.isRegionEmpty(airDrop, location)) {
                if (!isBiomeInBlackList(location))

                    if (airDrop.isFlatnessCheck()) {
                        if (checkForEvenness(location, airDrop)) {

                            return location;
                        } else {

                            return null;
                        }
                    } else {

                        return location;
                    }
            }
        }

        return null;
    }

    private boolean checkMaxY(@NotNull Location location, AirDrop airDrop, String path) {
        return GeneratorUtils.getSettings(airDrop.getGeneratorSettings(), path) >= location.getY();
    }
    private boolean checkMaxY(int y, AirDrop airDrop, String path) {
        return GeneratorUtils.getSettings(airDrop.getGeneratorSettings(), path) >= y;
    }

    @Override
    public boolean checkForEvenness(@NotNull Location location, AirDrop airDrop) {
        for (int y = GeneratorUtils.getSettings(airDrop.getGeneratorSettings(), "check-for-evenness.poz.start-y"); y < GeneratorUtils.getSettings(airDrop.getGeneratorSettings(), "check-for-evenness.poz.end-y"); y++) {
            for (int x = GeneratorUtils.getSettings(airDrop.getGeneratorSettings(), "check-for-evenness.poz.start-x"); x < GeneratorUtils.getSettings(airDrop.getGeneratorSettings(), "check-for-evenness.poz.end-x"); x++) {
                for (int z = GeneratorUtils.getSettings(airDrop.getGeneratorSettings(), "check-for-evenness.poz.start-z"); z < GeneratorUtils.getSettings(airDrop.getGeneratorSettings(), "check-for-evenness.poz.end-z"); z++) {
                    if (x == 0 && y == 0 && z == 0) continue;
                    if (!location.clone().add(x, y, z).getBlock().getType().isAir() && ! BAirDrop.getiConfig().getGeneratorSettings().getStringList("ignored-blocks").contains(String.valueOf(location.clone().add(x, y, z).getBlock().getType())))
                        return false;
                }
            }
        }
        return true;
    }

    private boolean isBiomeInBlackList(@NotNull Location location) {
        Biome bom = location.getWorld().getBiome((int) location.getX(), (int) location.getY(), (int) location.getZ());
        return  BAirDrop.getiConfig().getGeneratorSettings().getStringList("black-List-biome").contains(String.valueOf(bom));
    }
}
