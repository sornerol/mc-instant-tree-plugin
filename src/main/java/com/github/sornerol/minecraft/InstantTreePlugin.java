package com.github.sornerol.minecraft;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public class InstantTreePlugin extends JavaPlugin implements Listener {

    public static final List<Material> TREE_MATERIALS = setTreeMaterials();
    public static final List<Material> LEAF_MATERIALS = setLeafMaterials();

    public boolean shouldInstantBreakLeaves = false;
    public boolean shouldAllowSilkTouch = false;
    public boolean shouldSkipBreakingWhileSneaking = true;

    @Override
    public void onEnable() {
        super.onEnable();
        Bukkit.getPluginManager().registerEvents(this, this);
        getConfig().options().copyDefaults(true);
        saveConfig();
        shouldInstantBreakLeaves = getConfig().getBoolean("instantBreakLeaves");
        shouldAllowSilkTouch = getConfig().getBoolean("allowSilkTouch");
        shouldSkipBreakingWhileSneaking = getConfig().getBoolean("skipBreakingWhileSneaking");
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void blockMine(BlockBreakEvent event) {
        //TODO: Modify to work with big mushrooms
        if (!shouldBreakTree(event)) {
            return;
        }

        breakTree(event.getBlock(), event.getPlayer());
    }

    private boolean shouldBreakTree(BlockBreakEvent event) {
        if (event.isCancelled()) {
            return false;
        }
        if (shouldSkipBreakingWhileSneaking && event.getPlayer().isSneaking()) {
            return false;
        }
        if (event.getPlayer().getGameMode() != GameMode.SURVIVAL) {
            return false;
        }
        if (!blockIsTreeMaterial(event.getBlock())) {
            return false;
        }
        return blockIsPartOfTree(event.getBlock());
    }

    private void breakTree(Block baseBlock, Player player) {
        List<Block> blocksToBreak = getBlocksToBreak(baseBlock);
        ItemStack itemInUse = player.getInventory().getItemInMainHand();

        for (Block block : blocksToBreak) {
            if (shouldAllowSilkTouch && itemInUse.containsEnchantment(Enchantment.SILK_TOUCH)) {
                block.breakNaturally(itemInUse);
            } else {
                block.breakNaturally();
            }
        }
    }

    private List<Block> getBlocksToBreak(Block startingBlock) {
        List<Block> blocksToBreak = new ArrayList<>();
        List<Block> blocksAlreadyExamined = new ArrayList<>();
        Queue<Block> blocksToExamine = new LinkedList<>();

        blocksToExamine.add(startingBlock);
        while (blocksToExamine.size() > 0) {
            Block currentBlock = blocksToExamine.remove();
            if (blockIsTreeMaterial(currentBlock) ||
                    (blockIsLeafMaterial(currentBlock) && shouldInstantBreakLeaves)) {
                blocksToBreak.add(currentBlock);
            }
            blocksAlreadyExamined.add(currentBlock);
            List<Block> neighboringBlocks = getNeighboringBlocks(currentBlock);
            neighboringBlocks.removeAll(blocksAlreadyExamined);
            neighboringBlocks.removeAll(blocksToExamine);
            blocksToExamine.addAll(neighboringBlocks);
        }

        return blocksToBreak;
    }

    private List<Block> getNeighboringBlocks(Block block) {
        List<Block> neighboringBlocks = new ArrayList<>();
        Location blockLocation = block.getLocation();

        for (int y = 0; y <= 1; y++) {
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    Block blockToAdd = blockLocation.clone().add(x, y, z).getBlock();
                    if (blockIsTreeMaterial(blockToAdd) || blockIsLeafMaterial(blockToAdd)) {
                        neighboringBlocks.add(blockToAdd);
                    }
                }
            }
        }

        return neighboringBlocks;
    }

    private boolean blockIsTreeMaterial(Block block) {
        return TREE_MATERIALS.contains(block.getType());
    }

    private boolean blockIsLeafMaterial(Block block) {
        return LEAF_MATERIALS.contains(block.getType());
    }

    private boolean blockIsPartOfTree(Block block) {
        Location location = block.getLocation();
        Material material = block.getType();

        while (location.add(0, 1, 0).getBlock().getType() == material) {
            if (areLeavesAdjacent(location)) {
                return true;
            }
        }
        return false;
    }

    private boolean areLeavesAdjacent(Location location) {
        for (int y = 0; y <= 2; y++) {
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    Block block = location.clone().add(x, y, z).getBlock();
                    if (blockIsLeafMaterial(block)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static List<Material> setTreeMaterials() {
        return Arrays.asList(
                Material.ACACIA_LOG,
                Material.BIRCH_LOG,
                Material.OAK_LOG,
                Material.SPRUCE_LOG,
                Material.DARK_OAK_LOG,
                Material.JUNGLE_LOG);
    }

    private static List<Material> setLeafMaterials() {
        return Arrays.asList(
                Material.ACACIA_LEAVES,
                Material.BIRCH_LEAVES,
                Material.OAK_LEAVES,
                Material.SPRUCE_LEAVES,
                Material.DARK_OAK_LEAVES,
                Material.JUNGLE_LEAVES);
    }
}
