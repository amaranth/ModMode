package com.probablycoding.bukkit.modmode;

import java.util.List;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class StateManager {
    private final ModMode plugin;

    public StateManager(ModMode instance) {
        plugin = instance;
    }

    public void reload() {
        plugin.reloadConfig();
    }

    public boolean isVanished(Player player) {
        for (String name : plugin.getConfig().getStringList("vanished")) {
            if (name.equalsIgnoreCase(player.getName())) {
                return true;
            }
        }
        return false;
    }

    public boolean isFullVanished(Player player) {
        for (String name : plugin.getConfig().getStringList("fullvanished")) {
            if (name.equalsIgnoreCase(player.getName())) {
                return true;
            }
        }
        return false;
    }

    public boolean isModMode(Player player) {
        for (String name : plugin.getConfig().getStringList("modmode")) {
            if (name.equalsIgnoreCase(ChatColor.stripColor(player.getName()))) {
                return true;
            }
        }
        return false;
    }

    public void addVanished(Player player) {
        List<String> vanished = plugin.getConfig().getStringList("vanished");
        if (!vanished.contains(player.getName())) {
            vanished.add(player.getName());
            plugin.getConfig().set("vanished", vanished);
            plugin.saveConfig();
        }
    }

    public void removeVanished(Player player) {
        List<String> vanished = plugin.getConfig().getStringList("vanished");
        vanished.remove(player.getName());
        plugin.getConfig().set("vanished", vanished);
        plugin.saveConfig();
    }

    public void addFullVanished(Player player) {
        List<String> fullvanished = plugin.getConfig().getStringList("fullvanished");
        if (!fullvanished.contains(player.getName())) {
            fullvanished.add(player.getName());
            plugin.getConfig().set("fullvanished", fullvanished);
            plugin.saveConfig();
        }
    }

    public void removeFullVanished(Player player) {
        List<String> fullvanished = plugin.getConfig().getStringList("fullvanished");
        fullvanished.remove(player.getName());
        plugin.getConfig().set("fullvanished", fullvanished);
        plugin.saveConfig();
    }

    public void addModMode(Player player) {
        List<String> modmode = plugin.getConfig().getStringList("modmode");
        if (!modmode.contains(player.getName())) {
            modmode.add(player.getName());
            plugin.getConfig().set("modmode", modmode);
            plugin.saveConfig();
        }
    }

    public void removeModMode(Player player) {
        List<String> modmode = plugin.getConfig().getStringList("modmode");
        modmode.remove(ChatColor.stripColor(player.getName()));
        plugin.getConfig().set("modmode", modmode);
        plugin.saveConfig();
    }
}
