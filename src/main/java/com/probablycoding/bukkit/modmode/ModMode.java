package com.probablycoding.bukkit.modmode;

import net.minecraft.server.*;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class ModMode extends JavaPlugin {
    private final ModModeListener listener = new ModModeListener(this);
    public final StateManager state = new StateManager(this);

    public void enableVanish(Player player) {
        if (state.isFullVanished(player)) {
            for (Player other : getServer().getOnlinePlayers()) {
                if (Permissions.hasPermission(other, Permissions.SHOWMODS)) {
                    other.showPlayer(player);
                    other.sendMessage(ChatColor.YELLOW + player.getName() + " joined the game.");
                }
            }
            state.addVanished(player);
            state.removeFullVanished(player);
        } else {
            for (Player other : getServer().getOnlinePlayers()) {
                if (Permissions.hasPermission(other, Permissions.SHOWMODS)) {
                    continue;
                }
                if (Permissions.hasPermission(other, Permissions.SHOWVANISHED)) {
                    continue;
                }
                other.hidePlayer(player);
                other.sendMessage(ChatColor.YELLOW + player.getName() + " left the game.");
            }
            state.addVanished(player);
        }

        player.sendMessage(ChatColor.RED + "Poof!");
    }

    public void enableFullVanish(Player player) {
        if (state.isVanished(player)) {
            state.removeVanished(player);
        }

        for (Player other : getServer().getOnlinePlayers()) {
            if (!other.canSee(player)) {
                continue;
            }
            if (Permissions.hasPermission(other, Permissions.SHOWVANISHED)) {
                continue;
            }
            other.hidePlayer(player);
            other.sendMessage(ChatColor.YELLOW + player.getName() + " left the game.");
        }

        state.addFullVanished(player);
        player.sendMessage(ChatColor.RED + "Poof! Fully vanished!");
    }

    public void disableVanish(Player player) {
        for (Player other : getServer().getOnlinePlayers()) {
            if (other.canSee(player)) {
                continue;
            }
            other.showPlayer(player);
            other.sendMessage(ChatColor.YELLOW + player.getName() + " joined the game.");
        }

        state.removeVanished(player);
        state.removeFullVanished(player);
        player.sendMessage(ChatColor.RED + "You have reappeared!");
    }

    public void toggleModMode(Player player, boolean enable, boolean onJoin) {
        String name;
        Location loc = player.getLocation();
        ServerConfigurationManager scm = ((CraftServer) getServer()).getHandle();

        if (enable) {
            name = ChatColor.GREEN + player.getName() + ChatColor.WHITE;
            state.addModMode(player);
            player.sendMessage(ChatColor.RED + "You are now in ModMode!");
        } else {
            name = ChatColor.stripColor(player.getName());
            state.removeModMode(player);
            player.sendMessage(ChatColor.RED + "You are no longer in ModMode!");
        }

        if (!onJoin) {
            PlayerQuitEvent quitEvent = new PlayerQuitEvent(player, ChatColor.YELLOW + player.getName() + " left the game.");
            getServer().getPluginManager().callEvent(quitEvent);
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
        for (Player other : getServer().getOnlinePlayers()) {
            player.showPlayer(other);
            other.showPlayer(player);
        }

        PlayerJoinEvent joinEvent = new PlayerJoinEvent(player, ChatColor.YELLOW + player.getName() + " joined the game.");
        getServer().getPluginManager().callEvent(joinEvent);
        if ((joinEvent.getJoinMessage() != null) && (joinEvent.getJoinMessage().length() > 0)) {
            scm.sendAll(new Packet3Chat(joinEvent.getJoinMessage()));
        }

        ((WorldServer) entity.world).tracker.untrackEntity(entity);

        if (!enable || onJoin) {
            loc = new Location(entity.world.getWorld(), entity.locX, entity.locY, entity.locZ, entity.yaw, entity.pitch);
        }
        entity = scm.moveToWorld(entity, entity.dimension, true, loc);

        ((WorldServer) entity.world).tracker.track(entity);

        if (enable) {
            player.setAllowFlight(true);
        }
    }

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(listener, this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Sorry, this command cannot be used from console.");
            return true;
        }

        Player player = (Player) sender;
        if (command.getName().equalsIgnoreCase("vanish")) {
            if (state.isVanished(player)) {
                player.sendMessage(ChatColor.RED + "You are already vanished!");
            } else {
                enableVanish(player);
            }
        }

        if (command.getName().equalsIgnoreCase("fullvanish")) {
            if (state.isFullVanished(player)) {
                player.sendMessage(ChatColor.RED + "You are already fully vanished!");
            } else {
                enableFullVanish(player);
            }
        }

        if (command.getName().equalsIgnoreCase("unvanish")) {
            if (state.isFullVanished(player) || state.isVanished(player)) {
                disableVanish(player);
            } else {
                player.sendMessage(ChatColor.RED + "You are not vanished!");
            }
        }

        if (command.getName().equalsIgnoreCase("modmode")) {
            if (state.isModMode(player)) {
                toggleModMode(player, false, false);
                
            } else {
                toggleModMode(player, true, false);
            }
        }

        return true;
    }
}
