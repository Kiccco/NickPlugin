package me.kiccco.plugin.commands;

import me.kiccco.plugin.NickPlugin;
import me.kiccco.plugin.mysql.MySQLManager;
import me.kiccco.plugin.utils.SkinFetcher;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class NickSkinCommand implements CommandExecutor {


    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String arg, String[] args) {

        Player player = (Player) sender;
        if(!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this comamnd!");
            return false;
        }

        if(!player.hasPermission("system.admin")) {
            player.sendMessage(ChatColor.RED + "You can't use that command!");
            return false;
        }

        if(args.length == 0) {
            player.sendMessage(ChatColor.GREEN + "Usage: /nickskin <name> - it will add skin to database");
            return false;
        }

        String name = args[0];
        SkinFetcher.getSkin(name, callback -> {
            if(callback == null) {
                player.sendMessage(ChatColor.RED + "INVALID SKIN!");
                return;
            }

            addSkin(name, callback.getValue(), callback.getSignature());
            player.sendMessage(ChatColor.GREEN + "Skin added to database!");
        });

        return false;
    }

    private void addSkin(String name, String value, String signsture) {
        new BukkitRunnable() {
            @Override
            public void run() {
                String sql = "INSERT INTO nickSkins (name, value, signature) VALUES (?,?,?)";
                Connection connection = null;
                PreparedStatement preparedStatement = null;
                try {
                    connection = MySQLManager.getHikari().getConnection();
                    preparedStatement = connection.prepareStatement(sql);
                    preparedStatement.setString(1, name);
                    preparedStatement.setString(2, value);
                    preparedStatement.setString(3, signsture);
                    preparedStatement.executeUpdate();
                } catch (SQLException throwables) {
                    Bukkit.getLogger().warning("ERROR OCCURED WHILE ADDING SKIN!");
                    throwables.printStackTrace();
                } finally {
                    MySQLManager.closeConnections(connection, preparedStatement, null);
                }
            }
        }.runTaskAsynchronously(NickPlugin.getInstance());
    }
}
