package com.probablycoding.bukkit.modmode;

import org.bukkit.command.CommandSender;

public class Permissions {
    private static final String MODMODE = "modmode";

    public static final String VANISH         = MODMODE + ".vanish";
    public static final String VANISHOTHERS   = VANISH + ".others";
    public static final String UNVANISH       = MODMODE + ".unvanish";
    public static final String UNVANISHOTHERS = UNVANISH + ".others";
    public static final String TOGGLE         = MODMODE + ".toggle";

    public static boolean hasPermission(CommandSender sender, String permission) {
        return sender.hasPermission(permission);
    }
}
