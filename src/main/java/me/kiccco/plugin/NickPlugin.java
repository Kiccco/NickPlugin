package me.kiccco.plugin;

import me.kiccco.plugin.managers.NickManager;
import me.kiccco.plugin.mysql.MySQLManager;
import me.kiccco.plugin.others.ServerType;
import me.neznamy.tab.api.*;
import net.luckperms.api.LuckPerms;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class NickPlugin extends JavaPlugin {

    private static NickPlugin instance;

    private static TabAPI tabAPI;

    private static LuckPerms api;

    private static ServerType serverType;

    @Override
    public void onEnable() {
        super.onEnable();
        instance = this;

        saveDefaultConfig();


        if(getConfig().getString("serverType").equals("LOBBY")) serverType = ServerType.LOBBY;
        if(getConfig().getString("serverType").equals("GAME")) serverType = ServerType.GAME;


        RegisteredServiceProvider<LuckPerms> provider = Bukkit.getServicesManager().getRegistration(LuckPerms.class);
        if (provider != null) {
            api = provider.getProvider();
        }

        if(getServer().getPluginManager().isPluginEnabled("TAB")) {
            tabAPI = TabAPI.getInstance();
        }

        new MySQLManager(this);
        new NickManager(this);
        getLogger().warning("NickPlugin Enabled");

    }

    @Override
    public void onDisable() {
        super.onDisable();
        getLogger().warning("NickPlugin Disabled!");
    }

    public static LuckPerms getLuckPerms() {
        return api;
    }

    public static NickPlugin getInstance() {
        return instance;
    }

    public static ServerType getServerType() {
        return serverType;
    }


    public static TabAPI getTabAPI() {
        return tabAPI;
    }

}
