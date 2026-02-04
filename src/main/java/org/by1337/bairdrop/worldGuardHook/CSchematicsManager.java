package org.by1337.bairdrop.worldGuardHook;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockTypes;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.RespawnAnchor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.util.Vector;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.by1337.bairdrop.AirDrop;
import org.by1337.bairdrop.BAirDrop;
import org.by1337.bairdrop.util.LogLevel;
import org.by1337.bairdrop.util.Message;


public class CSchematicsManager implements SchematicsManager{

    public void PasteSchematics(String name, AirDrop airDrop) {
        try {
            if (airDrop.getSavedBlocksData() != null && !airDrop.getSavedBlocksData().isEmpty()) {
                Message.error(BAirDrop.getConfigMessage().getMessage("schem-limit"));
                return;
            }
            Vector offsets = new Vector(
                    BAirDrop.getiConfig().getSchemConf().getInt(String.format("schematics.%s.offsets-x", name)),
                    BAirDrop.getiConfig().getSchemConf().getInt(String.format("schematics.%s.offsets-y", name)),
                    BAirDrop.getiConfig().getSchemConf().getInt(String.format("schematics.%s.offsets-z", name))
            );
            boolean ignoreAirBlocks = BAirDrop.getiConfig().getSchemConf().getBoolean(String.format("schematics.%s.ignore-air-blocks", name));
            String file = BAirDrop.getiConfig().getSchemConf().getString(String.format("schematics.%s.file", name));

            Map<BlockState, BlockState> replaceBlocks = new HashMap<>();
            ConfigurationSection replaceSection = BAirDrop.getiConfig().getSchemConf().getConfigurationSection(String.format("schematics.%s.replace-blocks", name));
            if (replaceSection != null) {
                for (String fromBlock : replaceSection.getKeys(false)) {
                    String toBlock = replaceSection.getString(fromBlock);
                    if (toBlock != null) {
                        try {
                            BlockState from = BlockTypes.get(fromBlock.toLowerCase()).getDefaultState();
                            BlockState to = BlockTypes.get(toBlock.toLowerCase()).getDefaultState();
                            if (from != null && to != null) {
                                replaceBlocks.put(from, to);
                            }
                        } catch (Exception e) {
                            Message.warning("Invalid block in replace-blocks: " + fromBlock + " -> " + toBlock);
                        }
                    }
                }
            }

            if (file == null || !BAirDrop.getiConfig().getSchematics().containsKey(file)) {
                throw new IllegalArgumentException("unknown schematic: " + name);
            }

            Message.debug("paste " + file, LogLevel.LOW);

            File schem = BAirDrop.getiConfig().getSchematics().get(file);
            ClipboardFormat format = ClipboardFormats.findByFile(schem);
            ClipboardReader reader = format.getReader(new FileInputStream(schem));
            Clipboard clipboard = reader.read();

            Location loc = airDrop.getAirDropLocation();
            if (loc == null)
                loc = airDrop.getFutureLocation();
            if (loc == null)
                throw new NullPointerException();

            World world = loc.getWorld();
            com.sk89q.worldedit.world.World adaptedWorld = BukkitAdapter.adapt(world);

            BlockVector3 clipboardMin = clipboard.getMinimumPoint();
            BlockVector3 clipboardMax = clipboard.getMaximumPoint();
            BlockVector3 origin = clipboard.getOrigin();

            int pasteX = loc.getBlockX() + offsets.getBlockX();
            int pasteY = loc.getBlockY() + offsets.getBlockY();
            int pasteZ = loc.getBlockZ() + offsets.getBlockZ();

            int minX = pasteX + (clipboardMin.x() - origin.x());
            int minY = pasteY + (clipboardMin.y() - origin.y());
            int minZ = pasteZ + (clipboardMin.z() - origin.z());
            int maxX = pasteX + (clipboardMax.x() - origin.x());
            int maxY = pasteY + (clipboardMax.y() - origin.y());
            int maxZ = pasteZ + (clipboardMax.z() - origin.z());

            SavedBlocksData savedBlocksData = new SavedBlocksData(world);
            savedBlocksData.setBounds(minX, minY, minZ, maxX, maxY, maxZ);

            for (int x = minX; x <= maxX; x++) {
                for (int y = minY; y <= maxY; y++) {
                    for (int z = minZ; z <= maxZ; z++) {
                        int clipX = origin.x() + (x - pasteX);
                        int clipY = origin.y() + (y - pasteY);
                        int clipZ = origin.z() + (z - pasteZ);
                        BlockVector3 clipPos = BlockVector3.at(clipX, clipY, clipZ);
                        
                        try {
                            BlockState clipboardBlock = clipboard.getBlock(clipPos);
                            boolean isAir = clipboardBlock.getBlockType() == BlockTypes.AIR 
                                    || clipboardBlock.getBlockType() == BlockTypes.CAVE_AIR
                                    || clipboardBlock.getBlockType() == BlockTypes.VOID_AIR;
                            
                            if (ignoreAirBlocks && isAir) {
                                continue;
                            }
                            
                            Location blockLoc = new Location(world, x, y, z);
                            Block block = blockLoc.getBlock();
                            savedBlocksData.saveBlock(blockLoc, block.getBlockData());
                        } catch (Exception ignored) {
                        }
                    }
                }
            }

            airDrop.setSavedBlocksData(savedBlocksData);

            if (!replaceBlocks.isEmpty()) {
                for (int x = clipboardMin.x(); x <= clipboardMax.x(); x++) {
                    for (int y = clipboardMin.y(); y <= clipboardMax.y(); y++) {
                        for (int z = clipboardMin.z(); z <= clipboardMax.z(); z++) {
                            BlockVector3 pos = BlockVector3.at(x, y, z);
                            BlockState currentBlock = clipboard.getBlock(pos);
                            BlockState replacement = replaceBlocks.get(currentBlock);
                            if (replacement != null) {
                                clipboard.setBlock(pos, replacement);
                            }
                        }
                    }
                }
            }

            EditSession editSession = WorldEdit.getInstance().newEditSession(adaptedWorld);
            Operation operation = new ClipboardHolder(clipboard).createPaste(editSession)
                    .to(BlockVector3.at(pasteX, pasteY, pasteZ)).ignoreAirBlocks(ignoreAirBlocks).build();

            Operations.complete(operation);
            editSession.close();

            airDrop.setEditSession(editSession);
            
            if (!ignoreAirBlocks && airDrop.getAirDropLocation() != null) {
                Location airDropLoc = airDrop.getAirDropLocation();
                Material material = airDrop.isAirDropLocked() ? airDrop.getMaterialLocked() : airDrop.getMaterialUnlocked();
                if (material != null) {
                    Block block = airDropLoc.getBlock();
                    block.setType(material);
                    if (material == Material.RESPAWN_ANCHOR) {
                        RespawnAnchor anchorData = (RespawnAnchor) block.getBlockData();
                        anchorData.setCharges(anchorData.getMaximumCharges());
                        block.setBlockData(anchorData);
                    }
                }
            }
        } catch (IOException | WorldEditException | IllegalArgumentException | NullPointerException e) {
            e.printStackTrace();
        }
    }

}
