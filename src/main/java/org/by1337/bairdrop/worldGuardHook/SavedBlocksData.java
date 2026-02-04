package org.by1337.bairdrop.worldGuardHook;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class SavedBlocksData {
    private final Map<Location, BlockData> savedBlocks = new HashMap<>();
    private final World world;

    public SavedBlocksData(World world) {
        this.world = world;
    }

    public void saveBlock(Location loc, BlockData data) {
        savedBlocks.put(loc.clone(), data.clone());
    }

    public void setBounds(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
    }

    public void restore() {
        Set<Chunk> affectedChunks = new HashSet<>();
        
        for (Map.Entry<Location, BlockData> entry : savedBlocks.entrySet()) {
            Location loc = entry.getKey();
            BlockData data = entry.getValue();
            Block block = loc.getBlock();
            block.setBlockData(data, true);
            affectedChunks.add(block.getChunk());
        }
        
        for (Chunk chunk : affectedChunks) {
            if (chunk.isLoaded()) {
                for (Player player : world.getPlayers()) {
                    if (player.getLocation().distanceSquared(chunk.getBlock(8, 64, 8).getLocation()) < 256 * 256) {
                        player.getWorld().refreshChunk(chunk.getX(), chunk.getZ());
                    }
                }
            }
        }
        
        savedBlocks.clear();
    }

    public boolean isEmpty() {
        return savedBlocks.isEmpty();
    }

    public World getWorld() {
        return world;
    }
}
