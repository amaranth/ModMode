package com.probablycoding.bukkit.modmode;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class ModModeListener implements Listener {
    private final ModMode plugin;

    public class ModModeRunnable implements Runnable {
        private final Player player;

        public ModModeRunnable(Player foo) {
            player = foo;
        }

        public void run() {
            plugin.state.enableModMode(player, true);
        }
    }

    ModModeListener(ModMode instance) {
        plugin = instance;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        if (plugin.state.isModMode(player) && !player.getName().startsWith(ChatColor.GREEN.toString())) {
            event.setJoinMessage(null);
            Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new ModModeRunnable(player), 1);
        }

        if (plugin.state.isVanished(player)) {
            plugin.state.enableVanish(player, plugin.state.getVanishLevel(player));
        }

        int level = plugin.state.getVanishLevel(player);
        for (Player other : plugin.getServer().getOnlinePlayers()) {
            if (plugin.state.isVanished(other) && plugin.state.getVanishLevel(other) > level) {
                player.hidePlayer(other);
            }
        }

        // send our own join message only to people who can see the player
        if (plugin.state.isVanished(player) && event.getJoinMessage() != null) {
            String message = event.getJoinMessage();
            event.setJoinMessage(null);
            for (Player other : Bukkit.getOnlinePlayers()) {
                if (other.canSee(player)) {
                    other.sendMessage(message);
                }
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // send our own quit message only to people who can see the player
        Player player = event.getPlayer();
        if (plugin.state.isVanished(player) && event.getQuitMessage() != null) {
            String message = event.getQuitMessage();
            event.setQuitMessage(null);
            for (Player other : Bukkit.getOnlinePlayers()) {
                if (other.canSee(player)) {
                    other.sendMessage(message);
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerPickupItem(PlayerPickupItemEvent event) {
        Player player = event.getPlayer();
        if (plugin.state.isVanished(player)) {
            event.setCancelled(true);
        }
        if (plugin.state.isModMode(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        if (plugin.state.isVanished(player)) {
            event.setCancelled(true);
        }
        if (plugin.state.isModMode(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityTarget(EntityTargetEvent event) {
        if (!(event.getTarget() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getTarget();
        if (plugin.state.isVanished(player)) {
            event.setCancelled(true);
        }
        if (plugin.state.isModMode(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        Player damager = null;
        Player victim = null;

        // block PVP with a message
        if (event instanceof EntityDamageByEntityEvent) {
            EntityDamageByEntityEvent e = (EntityDamageByEntityEvent) event;
            if (e.getDamager() instanceof Player) {
                damager = (Player) e.getDamager();
            } else if (e.getDamager() instanceof Projectile) {
                Projectile projectile = (Projectile) e.getDamager();
                if (projectile.getShooter() != null && projectile.getShooter() instanceof Player) {
                    damager = (Player) projectile.getShooter();
                }
            }

            if (e.getEntity() instanceof Player) {
                victim = (Player) e.getEntity();
            }

            if (damager != null) {
                if (plugin.state.isVanished(damager)) {
                    event.setCancelled(true);
                }
                if (plugin.state.isModMode(damager)) {
                    event.setCancelled(true);
                }
            }

            if (damager != null && victim != null) {
                // only show message if they aren't invisible
                if (plugin.state.isModMode(victim) && !plugin.state.isVanished(victim)) {
                    damager.sendMessage("This moderator is in ModMode.");
                    damager.sendMessage("ModMode should only be used for official server business.");
                    damager.sendMessage("Please let an admin know if a moderator is abusing ModMode.");
                }
            }
        }

        // block all damage to invisible and modmode players
        if (event.getEntity() instanceof Player) {
            victim = (Player) event.getEntity();
            if (plugin.state.isVanished(victim)) {
                event.setCancelled(true);
            }
            if (plugin.state.isModMode(victim)) {
                event.setCancelled(true);
            }
        }
    }
}
