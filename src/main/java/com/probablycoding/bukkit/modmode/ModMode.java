package com.probablycoding.bukkit.modmode;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class ModMode extends JavaPlugin {
    private final ModModeListener listener = new ModModeListener(this);
    public final StateManager state = new StateManager(this);

    public void showVanishList(CommandSender player) {
        // TODO: write this
    }

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(listener, this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("vanish")) {
            int level = 0;
            Player other = null;

            if (args.length > 0) {
                if (args[0].equalsIgnoreCase("list")) {
                    showVanishList(sender);
                    return true;
                }

                try {
                    level = Integer.parseInt(args[0]);
                } catch (Exception e) {
                    other = getServer().getPlayer(args[0]);
                    if (other == null) {
                        sender.sendMessage(ChatColor.RED + args[0] + " is not online!");
                        return true;
                    }
                }
            }

            if (other != null) {
                if (!Permissions.hasPermission(sender, Permissions.VANISHOTHERS)) {
                    sender.sendMessage(ChatColor.RED + "Sorry, you do not have permission to vanish other players.");
                    return true;
                }

                if (state.isVanished(other)) {
                    sender.sendMessage(ChatColor.RED + other.getName() + " is already vanished!");
                } else {
                    state.enableVanish(other, level);
                    sender.sendMessage(ChatColor.RED + other.getName() + " was vanished!");
                }
            } else {
                if (!(sender instanceof Player)) {
                    sender.sendMessage(ChatColor.RED + "Console cannot vanish!");
                    return true;
                }

                Player player = (Player) sender;
                if (state.isVanished(player) && state.getVanishLevel(player) == level) {
                    player.sendMessage(ChatColor.RED + "You are already vanished!");
                } else {
                    state.enableVanish(player, level);
                }
            }
        }

        if (command.getName().equalsIgnoreCase("unvanish")) {
            Player other = null;
            if (args.length > 0) {
                other = getServer().getPlayer(args[0]);
                if (other == null) {
                    sender.sendMessage(ChatColor.RED + args[0] + " is not online!");
                    return true;
                }
            }

            if (other != null) {
                if (!Permissions.hasPermission(sender, Permissions.UNVANISHOTHERS)) {
                    sender.sendMessage(ChatColor.RED + "Sorry, you do not have permission to unvanish other players.");
                    return true;
                }

                if (state.isVanished(other)) {
                    state.disableVanish(other);
                    sender.sendMessage(ChatColor.RED + other.getName() + " is now visible!");
                } else {
                    sender.sendMessage(ChatColor.RED + other.getName() + " is not vanished!");
                }
            } else {
                if (!(sender instanceof Player)) {
                    sender.sendMessage(ChatColor.RED + "Console cannot unvanish!");
                    return true;
                }

                Player player = (Player) sender;
                if (state.isVanished(player)) {
                    state.disableVanish(player);
                } else {
                    player.sendMessage(ChatColor.RED + "You are not vanished!");
                }
            }
        }

        if (command.getName().equalsIgnoreCase("modmode")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "Console cannot enter ModMode!");
                return true;
            }

            Player player = (Player) sender;
            if (state.isModMode(player)) {
                state.disableModMode(player);

            } else {
                state.enableModMode(player);
            }
        }

        return true;
    }
}
