// $Id$
/*
 * WorldGuard
 * Copyright (C) 2010 sk89q <http://www.sk89q.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.sk89q.worldguard.bukkit;

import static com.sk89q.worldguard.bukkit.BukkitUtil.toVector;

import com.nijiko.coelho.iConomy.system.Account;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.blacklist.events.BlockInteractBlacklistEvent;
import com.sk89q.worldguard.domains.DefaultDomain;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.flags.RegionFlagContainer;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.Event.Priority;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.PluginManager;
import com.nijiko.coelho.iConomy.iConomy;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldguard.blacklist.events.ItemAcquireBlacklistEvent;
import com.sk89q.worldguard.blacklist.events.ItemDropBlacklistEvent;
import com.sk89q.worldguard.blacklist.events.ItemUseBlacklistEvent;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.regionmanager.RegionManager;
import com.sk89q.worldguard.protection.regions.flags.Flags;
import com.sk89q.worldguard.protection.regions.flags.RegionFlag.RegionGroup;

import java.util.Iterator;
import java.util.List;

/**
 * Handles all events thrown in relation to a Player
 */
public class WorldGuardPlayerListener extends PlayerListener {

    /**
     * Plugin.
     */
    private WorldGuardPlugin plugin;
    private boolean checkediConomy = false;

    /**
     * Construct the object;
     * 
     * @param plugin
     */
    public WorldGuardPlayerListener(WorldGuardPlugin plugin) {
        this.plugin = plugin;
    }


    public void registerEvents() {
        PluginManager pm = plugin.getServer().getPluginManager();

        /* pm.registerEvent(Event.Type.PLAYER_ITEM, this, Priority.High, plugin); */
        pm.registerEvent(Event.Type.PLAYER_INTERACT, this, Priority.High, plugin);
        pm.registerEvent(Event.Type.PLAYER_DROP_ITEM, this, Priority.High, plugin);
        pm.registerEvent(Event.Type.PLAYER_PICKUP_ITEM, this, Priority.High, plugin);
        pm.registerEvent(Event.Type.PLAYER_JOIN, this, Priority.Normal, plugin);
        pm.registerEvent(Event.Type.PLAYER_LOGIN, this, Priority.Normal, plugin);
        pm.registerEvent(Event.Type.PLAYER_QUIT, this, Priority.Normal, plugin);
        pm.registerEvent(Event.Type.PLAYER_RESPAWN, this, Priority.High, plugin);
    }


    /**
     * Called when a player joins a server
     *
     * @param event Relevant event details
     */
    // @Override
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        GlobalConfiguration cfg = plugin.getGlobalConfiguration();
        WorldConfiguration wcfg = cfg.getWorldConfig(player.getWorld().getName());

        if (wcfg.fireSpreadDisableToggle) {
            player.sendMessage(ChatColor.YELLOW
                    + "Fire spread is currently globally disabled.");
        }

        // if (cfg.godmode || plugin.inGroup(player, "wg-invincible")) {
            cfg.addInvinciblePlayer(player.getName());
        // }

        if (plugin.inGroup(player, "wg-amphibious")) {
            cfg.addAmphibiousPlayer(player.getName());
        }
    }

    /**
     * Called when a player leaves a server
     *
     * @param event Relevant event details
     */
    @Override
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        GlobalConfiguration cfg = plugin.getGlobalConfiguration();

        cfg.removeInvinciblePlayer(player.getName());
        cfg.removeAmphibiousPlayer(player.getName());

        cfg.forgetPlayer(BukkitPlayer.wrapPlayer(plugin, player));
    }


    public void onPlayerInteract(PlayerInteractEvent event)
    {
        if (event.isCancelled()) {
            return;
        }
        Action action = event.getAction();
        if(action == Action.RIGHT_CLICK_BLOCK || action == Action.RIGHT_CLICK_AIR)
        {
            Block block = event.getClickedBlock();
            Material type = block.getType();
            Player player = event.getPlayer();

            GlobalConfiguration cfg = plugin.getGlobalConfiguration();
            WorldConfiguration wcfg = cfg.getWorldConfig(event.getClickedBlock().getWorld().getName());

            if (wcfg.useRegions && player.getItemInHand().getTypeId() == wcfg.regionWand) {
                Vector pt = toVector(block);

                RegionManager mgr = plugin.getGlobalRegionManager().getRegionManager(player.getWorld().getName());
                ApplicableRegionSet app = mgr.getApplicableRegions(pt);
                List<String> regions = mgr.getApplicableRegionsIDs(pt);

                if (regions.size() > 0) {
                    player.sendMessage(ChatColor.YELLOW + "Can you build? "
                            + (app.canBuild(BukkitPlayer.wrapPlayer(plugin, player)) ? "Yes" : "No"));

                    StringBuilder str = new StringBuilder();
                    for (Iterator<String> it = regions.iterator(); it.hasNext();) {
                        str.append(it.next());
                        if (it.hasNext()) {
                            str.append(", ");
                        }
                    }

                    player.sendMessage(ChatColor.YELLOW + "Applicable regions: " + str.toString());
                } else {
                    player.sendMessage(ChatColor.YELLOW + "WorldGuard: No defined regions here!");
                }
            }

            if (block.getType() == Material.CHEST
                    || block.getType() == Material.DISPENSER
                    || block.getType() == Material.FURNACE
                    || block.getType() == Material.BURNING_FURNACE
                    || block.getType() == Material.NOTE_BLOCK) {
                if (wcfg.useRegions) {
                    Vector pt = toVector(block);
                    LocalPlayer localPlayer = BukkitPlayer.wrapPlayer(plugin, player);
                    RegionManager mgr = plugin.getGlobalRegionManager().getRegionManager(player.getWorld().getName());

                    if (!plugin.hasPermission(player, "region.bypass")) {
                        ApplicableRegionSet set = mgr.getApplicableRegions(pt);
                        if (!set.isStateFlagAllowed(Flags.CHEST_ACCESS) && !set.canBuild(localPlayer)) {
                            player.sendMessage(ChatColor.DARK_RED + "You don't have permission for this area.");
                            event.setCancelled(true);
                            return;
                        }
                    }
                }
            }

            if (wcfg.useRegions && (type == Material.LEVER || type == Material.STONE_BUTTON)) {
                Vector pt = toVector(block);
                RegionManager mgr = cfg.getWorldGuardPlugin().getGlobalRegionManager().getRegionManager(player.getWorld().getName());
                ApplicableRegionSet applicableRegions = mgr.getApplicableRegions(pt);
                LocalPlayer localPlayer = BukkitPlayer.wrapPlayer(plugin, player);

                if (!applicableRegions.isStateFlagAllowed(Flags.LEVER_AND_BUTTON, localPlayer)) {
                    player.sendMessage(ChatColor.DARK_RED + "You don't have permission for this area.");
                    event.setCancelled(true);
                    return;
                }
            }

            if (wcfg.useRegions && type == Material.CAKE_BLOCK) {

                Vector pt = toVector(block);

                if (!cfg.canBuild(player, pt)) {
                    player.sendMessage(ChatColor.DARK_RED + "You don't have permission for this area.");

                    byte newData = (byte) (block.getData() - 1);
                    newData = newData < 0 ? 0 : newData;

                    block.setData(newData);
                    player.setHealth(player.getHealth() - 3);

                    return;
                }
            }

            if (wcfg.useRegions && wcfg.useiConomy && cfg.getiConomy() != null
                        && (type == Material.SIGN_POST || type == Material.SIGN || type == Material.WALL_SIGN)) {
                BlockState blockstate = block.getState();

                if (((Sign)blockstate).getLine(0).equalsIgnoreCase("[WorldGuard]")
                        && ((Sign)blockstate).getLine(1).equalsIgnoreCase("For sale")) {
                    String regionId = ((Sign)blockstate).getLine(2);
                    //String regionComment = ((Sign)block).getLine(3);

                    if (regionId != null && regionId != "") {
                        RegionManager mgr = cfg.getWorldGuardPlugin().getGlobalRegionManager().getRegionManager(player.getWorld().getName());
                        ProtectedRegion region = mgr.getRegion(regionId);

                        if (region != null) {
                            RegionFlagContainer flags = region.getFlags();

                            if (flags.getBooleanFlag(Flags.BUYABLE).getValue(false)) {
                                if (iConomy.getBank().hasAccount(player.getName())) {
                                    Account account = iConomy.getBank().getAccount(player.getName());
                                    double balance = account.getBalance();
                                    double regionPrice = flags.getDoubleFlag(Flags.PRICE).getValue();

                                    if (balance >= regionPrice) {
                                        account.subtract(regionPrice);
                                        player.sendMessage(ChatColor.YELLOW + "You have bought the region " + regionId + " for " +
                                                iConomy.getBank().format(regionPrice));
                                        DefaultDomain owners = region.getOwners();
                                        owners.addPlayer(player.getName());
                                        region.setOwners(owners);
                                        flags.getBooleanFlag(Flags.BUYABLE).setValue(false);
                                        account.save();
                                    } else {
                                        player.sendMessage(ChatColor.YELLOW + "You have not enough money.");
                                    }
                                } else {
                                    player.sendMessage(ChatColor.YELLOW + "You have not enough money.");
                                }
                            } else {
                                player.sendMessage(ChatColor.RED + "Region: " + regionId + " is not buyable");
                            }
                        } else {
                            player.sendMessage(ChatColor.DARK_RED + "The region " + regionId + " does not exist.");
                        }
                    } else {
                        player.sendMessage(ChatColor.DARK_RED + "No region specified.");
                    }
                }
            }

            if (wcfg.getBlacklist() != null) {

                if (!wcfg.getBlacklist().check(
                        new BlockInteractBlacklistEvent(BukkitPlayer.wrapPlayer(plugin, player), toVector(block),
                        block.getTypeId()), false, false)) {
                    event.setCancelled(true);
                    return;
                }
            }
        }
    }

    /**
     * Called when a player uses an item
     * 
     * @param event Relevant event details
     */
    /* @Override
    public void onPlayerItem(PlayerItemEvent event) {

        if (event.isCancelled()) {
            return;
        }

        Player player = event.getPlayer();
        Block block = event.getBlockClicked();
        ItemStack item = event.getItem();
        int itemId = item.getTypeId();

        GlobalConfiguration cfg = plugin.getGlobalConfiguration();
        WorldConfiguration wcfg = cfg.getWorldConfig(player.getWorld().getName());

        if (wcfg.useRegions
                && (itemId == 322 || itemId == 320 || itemId == 319 || itemId == 297 || itemId == 260
                        || itemId == 350 || itemId == 349 || itemId == 354) ) {
            return;
        }

        if (!wcfg.itemDurability) {
            // Hoes
            if (item.getTypeId() >= 290 && item.getTypeId() <= 294) {
                item.setDurability((byte) -1);
                player.setItemInHand(item);
            }
        }

        if (wcfg.useRegions && !event.isBlock() && block != null) {
            Vector pt = toVector(block.getRelative(event.getBlockFace()));
            if (block.getType() == Material.WALL_SIGN) {
                pt = pt.subtract(0, 1, 0);
            }

            if (!cfg.canBuild(player, pt)) {
                player.sendMessage(ChatColor.DARK_RED
                        + "You don't have permission for this area.");
                event.setCancelled(true);
                return;
            }
        }

        if (wcfg.getBlacklist() != null && item != null && block != null) {
            if (!wcfg.getBlacklist().check(
                    new ItemUseBlacklistEvent(BukkitPlayer.wrapPlayer(plugin, player),
                    toVector(block.getRelative(event.getBlockFace())),
                    item.getTypeId()), false, false)) {
                event.setCancelled(true);
                return;
            }
        }

        if (wcfg.useRegions && item != null && block != null && item.getTypeId() == 259) {
            Vector pt = toVector(block.getRelative(event.getBlockFace()));
            RegionManager mgr = plugin.getGlobalRegionManager().getRegionManager(player.getWorld().getName());

            if (!mgr.getApplicableRegions(pt).isStateFlagAllowed(Flags.LIGHTER)) {
                event.setCancelled(true);
                return;
            }
        }
    } */

    /**
     * Called when a player attempts to log in to the server
     *
     * @param event Relevant event details
     */
    @Override
    public void onPlayerLogin(PlayerLoginEvent event) {
        Player player = event.getPlayer();

        GlobalConfiguration cfg = plugin.getGlobalConfiguration();
        WorldConfiguration wcfg = cfg.getWorldConfig(player.getWorld().getName());

        if (wcfg.enforceOneSession) {
            String name = player.getName();

            for (Player pl : plugin.getServer().getOnlinePlayers()) {
                if (pl.getName().equalsIgnoreCase(name)) {
                    pl.kickPlayer("Logged in from another location.");
                }
            }
        }

        if (!checkediConomy) {
            iConomy iconomy = (iConomy) plugin.getServer().getPluginManager().getPlugin("iConomy");
            if (iconomy != null) {
                plugin.getGlobalConfiguration().setiConomy(iconomy);
            }

            checkediConomy = true;
        }
    }

    /**
     * Called when a player attempts to drop an item
     *
     * @param event Relevant event details
     */
    @Override
    public void onPlayerDropItem(PlayerDropItemEvent event) {

        if (event.isCancelled()) {
            return;
        }

        GlobalConfiguration cfg = plugin.getGlobalConfiguration();
        WorldConfiguration wcfg = cfg.getWorldConfig(event.getPlayer().getWorld().getName());

        if (wcfg.getBlacklist() != null) {
            Item ci = event.getItemDrop();

            if (!wcfg.getBlacklist().check(
                    new ItemDropBlacklistEvent(BukkitPlayer.wrapPlayer(plugin, event.getPlayer()), toVector(ci.getLocation()), ci.getItemStack().getTypeId()), false, false)) {
                event.setCancelled(true);
                return;
            }
        }
    }

    /**
     * Called when a player attempts to pickup an item
     * 
     * @param event
     *            Relevant event details
     */
    @Override
    public void onPlayerPickupItem(PlayerPickupItemEvent event) {

        if (event.isCancelled()) {
            return;
        }

        GlobalConfiguration cfg = plugin.getGlobalConfiguration();
        WorldConfiguration wcfg = cfg.getWorldConfig(event.getPlayer().getWorld().getName());

        if (wcfg.getBlacklist() != null) {
            Item ci = event.getItem();

            if (!wcfg.getBlacklist().check(
                    new ItemAcquireBlacklistEvent(BukkitPlayer.wrapPlayer(plugin, event.getPlayer()), toVector(ci.getLocation()), ci.getItemStack().getTypeId()), false, false)) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @Override
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        Location location = player.getLocation();

        ApplicableRegionSet regions = plugin.getGlobalRegionManager().getRegionManager(
                player.getWorld().getName()).getApplicableRegions(
                BukkitUtil.toVector(location));

        Location spawn = regions.getLocationFlag(Flags.SPAWN_LOC, true).getValue(player.getServer());

        if (spawn != null) {
            RegionGroup spawnconfig = regions.getRegionGroupFlag(Flags.SPAWN_PERM, true).getValue();
            if (spawnconfig != null) {
                BukkitPlayer localPlayer = BukkitPlayer.wrapPlayer(plugin, player);
                if (spawnconfig == RegionGroup.OWNER) {
                    if (regions.isOwner(localPlayer)) {
                        event.setRespawnLocation(spawn);
                    }
                } else if (spawnconfig == RegionGroup.MEMBER) {
                    if (regions.isMember(localPlayer)) {
                        event.setRespawnLocation(spawn);
                    }
                } else {
                    event.setRespawnLocation(spawn);
                }
            } else {
                event.setRespawnLocation(spawn);
            }
        }
    }
}
