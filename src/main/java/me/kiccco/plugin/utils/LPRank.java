package me.kiccco.plugin.utils;

import org.bukkit.entity.Player;

public enum LPRank {
    DEFAULT, VIP, VIPP, MVP, MVPP, MVPPP, YOUTUBE, HELPER, MOD, GAMEMASTER, ADMIN, SPONSOR, JERRY, COOWNER, OWNER;
    public static LPRank getRank(Player p) {
        if (p.hasPermission("system.owner"))
            return OWNER;
        if (p.hasPermission("system.coowner"))
            return COOWNER;
        if (p.hasPermission("system.jerry"))
            return JERRY;
        if (p.hasPermission("system.sponsor"))
            return SPONSOR;
        if (p.hasPermission("system.admin"))
            return ADMIN;
        if(p.hasPermission("system.gamemaster"))
            return GAMEMASTER;
        if (p.hasPermission("system.mod"))
            return MOD;
        if (p.hasPermission("system.helper"))
            return HELPER;
        if (p.hasPermission("system.youtube"))
            return YOUTUBE;
        if (p.hasPermission("system.mvppp"))
            return MVPPP;
        if (p.hasPermission("system.mvpp"))
            return MVPP;
        if (p.hasPermission("system.mvp"))
            return MVP;
        if (p.hasPermission("system.vipp"))
            return VIPP;
        if (p.hasPermission("system.vip"))
            return VIP;
        return DEFAULT;
    }

    public int getRankValue() {
        return this.ordinal();
    }
}