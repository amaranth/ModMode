package com.probablycoding.bukkit.modmode;

import java.util.Iterator;
import java.util.List;
import net.minecraft.server.*;
import org.bukkit.*;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class StateManager {
    private final ModMode plugin;

    public StateManager(ModMode instance) {
        plugin = instance;
    }

    public void reload() {
        plugin.reloadConfig();
    }

    public void enableVanish(Player player, int level) {
        int currentLevel = getVanishLevel(player);
        if (level == 0) {
            for (int i = 10; i > 0; i++) {
                if (Permissions.hasPermission(player, Permissions.VANISH + ".level." + i)) {
                    level = i;
                    break;
                }
            }
        }

        if (level < currentLevel) {
            for (Player other : Bukkit.getOnlinePlayers()) {
                if (Permissions.hasPermission(other, Permissions.VANISH + ".level." + level)) {
                    if (!other.equals(player)) {
                        other.showPlayer(player);
                        other.sendMessage(ChatColor.YELLOW + player.getName() + " joined the game.");
                    }
                }
            }
        } else {
            for (Player other : Bukkit.getOnlinePlayers()) {
                if (Permissions.hasPermission(other, Permissions.VANISH + ".level." + level)) {
                    continue;
                }
                other.hidePlayer(player);
                other.sendMessage(ChatColor.YELLOW + player.getName() + " left the game.");
            }
        }

        addVanished(player, level);
        player.sendMessage(ChatColor.RED + "Poof! Vanished at level " + level + "!");
    }

    public void disableVanish(Player player) {
        for (Player other : Bukkit.getOnlinePlayers()) {
            if (other.canSee(player)) {
                continue;
            }
            other.showPlayer(player);
            other.sendMessage(ChatColor.YELLOW + player.getName() + " joined the game.");
        }

        removeVanished(player);
        player.sendMessage(ChatColor.RED + "You have reappeared!");
    }

    public void enableModMode(Player player) {
        enableModMode(player, false);
    }

    public void enableModMode(Player player, boolean onJoin) {
        toggleModMode(player, true, onJoin);
    }

    public void disableModMode(Player player) {
        toggleModMode(player, false, false);
    }

    public boolean isVanished(Player player) {
        return getVanishLevel(player) != 0;
    }

    public boolean isModMode(Player player) {
        for (String name : plugin.getConfig().getStringList("modmode")) {
            if (name.equalsIgnoreCase(ChatColor.stripColor(player.getName()))) {
                return true;
            }
        }
        return false;
    }

    public int getVanishLevel(Player player) {
        for (int i = 1; i < 11; i++) {
            for (String name : plugin.getConfig().getStringList("vanished." + i)) {
                if (name.equalsIgnoreCase(player.getName())) {
                    return i;
                }
            }
        }
        return 0;
    }

    private void toggleModMode(Player player, boolean enable, boolean onJoin) {
        String name;
        Location loc = player.getLocation();

        ServerConfigurationManager scm = ((CraftServer) Bukkit.getServer()).getHandle();

        if (enable) {
            name = ChatColor.GREEN + player.getName() + ChatColor.WHITE;
            addModMode(player);
            player.sendMessage(ChatColor.RED + "You are now in ModMode!");
        } else {
            name = ChatColor.stripColor(player.getName());
            removeModMode(player);
            player.sendMessage(ChatColor.RED + "You are no longer in ModMode!");
        }

        if (!onJoin) {
            PlayerQuitEvent quitEvent = new PlayerQuitEvent(player, ChatColor.YELLOW + player.getName() + " left the game.");
            Bukkit.getPluginManager().callEvent(quitEvent);
            if ((quitEvent.getQuitMessage() != null) && (quitEvent.getQuitMessage().length() > 0)) {
                scm.sendAll(new Packet3Chat(quitEvent.getQuitMessage()));
            }
        }

        // eject from any vehicles, eject any passengers
        if (player.getVehicle() != null) {
            player.leaveVehicle();
        }
        if (player.getPassenger() != null) {
            player.getPassenger().leaveVehicle();
        }

        // change name, update inventory, and update gamemode
        GameMode mode = player.getGameMode();
        EntityPlayer entity = ((CraftPlayer) player).getHandle();
        scm.playerFileData.save(entity);
        entity.name = name;
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);
        scm.playerFileData.load(entity);
        if (player.getGameMode() != mode) {
            entity.netServerHandler.sendPacket(new Packet70Bed(3, mode.getValue()));
        }

        // reset vanish state so the join event can set it up correctly
        for (Player other : Bukkit.getOnlinePlayers()) {
            player.showPlayer(other);
            other.showPlayer(player);
        }

        PlayerJoinEvent joinEvent = new PlayerJoinEvent(player, ChatColor.YELLOW + player.getName() + " joined the game.");
        Bukkit.getPluginManager().callEvent(joinEvent);
        if ((joinEvent.getJoinMessage() != null) && (joinEvent.getJoinMessage().length() > 0)) {
            scm.sendAll(new Packet3Chat(joinEvent.getJoinMessage()));
        }

        ((WorldServer) entity.world).tracker.untrackEntity(entity);
        ((WorldServer) entity.world).tracker.track(entity);

        if (!enable || onJoin) {
            loc = new Location(entity.world.getWorld(), entity.locX, entity.locY, entity.locZ, entity.yaw, entity.pitch);
        }

        player.teleport(loc);
        //entity = scm.moveToWorld(entity, entity.dimension, true, loc);

        if (enable) {
            player.setAllowFlight(true);
        }
    }

    private void addVanished(Player player, int level) {
        if (getVanishLevel(player) != 0) {
            removeVanished(player);
        }

        List<String> vanished = plugin.getConfig().getStringList("vanished." + level);
        vanished.add(player.getName());
        plugin.getConfig().set("vanished." + level, vanished);
        plugin.saveConfig();
    }

    private void removeVanished(Player player) {
        for (int i = 1; i < 11; i++) {
            List<String> players = plugin.getConfig().getStringList("vanished." + i);
            Iterator it = players.iterator();
            while(it.hasNext()) {
                String name = (String) it.next();
                if (name.equalsIgnoreCase(player.getName())) {
                    it.remove();
                    break;
                }
            }
            plugin.getConfig().set("vanished." + i, players);
        }

        plugin.saveConfig();
    }

    private void addModMode(Player player) {
        List<String> modmode = plugin.getConfig().getStringList("modmode");
        if (!modmode.contains(player.getName())) {
            modmode.add(player.getName());
            plugin.getConfig().set("modmode", modmode);
            plugin.saveConfig();
        }
    }

    private void removeModMode(Player player) {
        List<String> modmode = plugin.getConfig().getStringList("modmode");
        modmode.remove(ChatColor.stripColor(player.getName()));
        plugin.getConfig().set("modmode", modmode);
        plugin.saveConfig();
    }
}
