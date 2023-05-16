package me.kiccco.plugin.managers;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import me.kiccco.plugin.NickPlayer;
import me.kiccco.plugin.NickPlugin;
import me.kiccco.plugin.commands.NickCommand;
import me.kiccco.plugin.commands.NickSkinCommand;
import me.kiccco.plugin.mysql.MySQLManager;
import me.kiccco.plugin.others.NickRank;
import me.kiccco.plugin.others.NickedSkin;
import me.kiccco.plugin.others.ServerType;
import me.kiccco.plugin.tasks.RepeatNickTask;
import me.kiccco.plugin.utils.LPRank;
import me.kiccco.plugin.utils.PlayerUtil;
import me.neznamy.tab.api.TabAPI;
import me.neznamy.tab.api.team.UnlimitedNametagManager;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.types.PrefixNode;
import net.minecraft.server.v1_8_R3.EnumDifficulty;
import net.minecraft.server.v1_8_R3.PacketPlayOutPlayerInfo;
import net.minecraft.server.v1_8_R3.PacketPlayOutRespawn;
import net.minecraft.server.v1_8_R3.WorldSettings;
import org.bukkit.*;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.function.Consumer;

public class NickManager implements Listener {

    private final NickPlugin plugin;
    private final String SKIN_TABLE = "CREATE TABLE IF NOT EXISTS nickSkins(id INT NOT NULL AUTO_INCREMENT, name VARCHAR(20), value TEXT, signature TEXT, PRIMARY KEY(id))";
    private final String NAME_TABLE = "CREATE TABLE IF NOT EXISTS nickNames(id INT NOT NULL AUTO_INCREMENT, name VARCHAR(20), PRIMARY KEY(id))";
    private final String NICK_TABLE = "CREATE TABLE IF NOT EXISTS activeNicks(uuid VARCHAR(200), name VARCHAR(20), SkinID INT, keepSkin BOOL DEFAULT '0', keepDefaultSkin BOOL DEFAULT '0', PRIMARY KEY(uuid), FOREIGN KEY(SkinID) REFERENCES nickSkins(id))";
    private final String SAVEDNICKS_TABLE = "CREATE TABLE IF NOT EXISTS savedNicks(uuid VARCHAR(200), name VARCHAR(20), SkinID INT, keepSkin BOOL DEFAULT '0', keepDefaultSkin BOOL DEFAULT '0', PRIMARY KEY(uuid), FOREIGN KEY(SkinID) REFERENCES nickSkins(id))";

    private static HashSet<NickPlayer> nickPlayers;

    private final Map<Integer, String> names;

    public NickManager(NickPlugin plugin) {
        this.plugin = plugin;
        this.nickPlayers = new HashSet<>();
        this.names = new HashMap<>();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        createTables();
        getRandomNames();
        registerCommands();
    }

    private void createTables() {
        MySQLManager.createTable(SKIN_TABLE);
        MySQLManager.createTable(NAME_TABLE);
        MySQLManager.createTable(NICK_TABLE);
        MySQLManager.createTable(SAVEDNICKS_TABLE);

    }

    private void registerCommands() {
        plugin.getCommand("nick").setExecutor(new NickCommand(this));
        plugin.getCommand("nickskin").setExecutor(new NickSkinCommand());
    }


     @EventHandler
     public void onPrelogin(AsyncPlayerPreLoginEvent event) {
        loadActiveNickPlayer(event.getUniqueId());
     }

     @EventHandler
     public void onLogin(PlayerLoginEvent event) {
        Player player = event.getPlayer();
        NickPlayer nickPlayer = getNickPlayer(player.getUniqueId());
        if(nickPlayer == null) return;
        nickPlayer.setOgName(player.getName());
        GameProfile gameProfile = ((CraftPlayer) player).getProfile();

        nickPlayer.setOgSkin(gameProfile.getProperties().get("textures").stream().filter(property -> property.getName().equals("textures")).findFirst().orElse(new Property("textures", "")));

        if(!nickPlayer.isKeepSkin() || nickPlayer.getNickedSkin() != null) {
            gameProfile.getProperties().clear();
            if(nickPlayer.getNickedSkin() != null) {
                gameProfile.getProperties().put("textures", nickPlayer.getNickedSkin().getProperty());
            }
        }
        PlayerUtil.changeName(player, nickPlayer.getName());


         new BukkitRunnable() {
             @Override
             public void run() {
                 if(NickPlugin.getServerType().equals(ServerType.LOBBY)) {
                     new RepeatNickTask(nickPlayer).runTaskTimerAsynchronously(plugin, 0, 20);
                 }
             }
         }.runTaskLater(plugin, 2);


     }
    @EventHandler
    public void onSignChange(SignChangeEvent event) {
        NickPlayer nickPlayer = getNickPlayer(event.getPlayer().getUniqueId());
        if(nickPlayer == null) return;
        if(nickPlayer.getSign() == null) return;
        nickPlayer.setName(event.getLine(0));
        insertNickPlayer(nickPlayer);
        nickPlayer(nickPlayer);
        nickPlayer.getSign().getBlock().setType(Material.AIR);
        nickPlayer.setSign(null);
    }


    @EventHandler
    public void onLeave(PlayerQuitEvent event) {
        NickPlayer nickPlayer = getNickPlayer(event.getPlayer().getUniqueId());
        if(nickPlayer != null) {
            nickPlayers.remove(nickPlayer);
        }
    }

// DB STUFF -----------------------------------------------------------------------------------------------------------------------------------------------------------------

    public void loadActiveNickPlayer(UUID uuid) {
        new BukkitRunnable() {
            @Override
            public void run() {
                String sql = "SELECT activeNicks.name, " +
                        "nickSkins.id, nickSkins.name, nickSkins.value, nickSkins.signature, " +
                        "activeNicks.keepSkin, activeNicks.keepDefaultSkin  FROM activeNicks " +
                        "INNER JOIN nickSkins ON nickSkins.id = activeNicks.SkinID WHERE uuid=?";
                Connection connection = null;
                PreparedStatement preparedStatement = null;
                ResultSet resultSet = null;
                try {
                    connection = MySQLManager.getHikari().getConnection();
                    preparedStatement = connection.prepareStatement(sql);
                    preparedStatement.setString(1, uuid.toString());
                    resultSet = preparedStatement.executeQuery();
                    if(resultSet.next()) {
                        NickPlayer nickPlayer = new NickPlayer(uuid);
                        nickPlayer.setName(resultSet.getString(1));

                        boolean keepSkin = resultSet.getBoolean(6);
                        boolean keepDefaultSkin = resultSet.getBoolean(7);
                        if(keepSkin) {
                            nickPlayer.setKeepSkin(true);
                        }

                        if(!keepSkin) {
                            int skinId = resultSet.getInt(2);
                            String skinName = resultSet.getString(3);
                            String skinValue = resultSet.getString(4);
                            String skinSignature = resultSet.getString(5);

                            NickedSkin nickedSkin = new NickedSkin(skinId, skinName, skinValue, skinSignature);
                            nickPlayer.setNickedSkin(nickedSkin);
                        }

                        if(keepDefaultSkin) {
                            nickPlayer.setNickedSkin(null);
                        }

                        nickPlayer.setNicked(true);

                        nickPlayers.add(nickPlayer);
                    }

                } catch (SQLException throwables) {
                    throwables.printStackTrace();
                } finally {
                    MySQLManager.closeConnections(connection, preparedStatement, resultSet);
                }
            }
        }.runTaskAsynchronously(plugin);
    }


    public void insertNickPlayer(NickPlayer nickPlayer) {
        new BukkitRunnable() {
            @Override
            public void run() {
                String sql = "INSERT INTO activeNicks (uuid, name,SkinID, keepSkin, keepDefaultSkin) VALUES (?,?,?,?,?)";
                Connection connection = null;
                PreparedStatement preparedStatement = null;
                try {
                    connection = MySQLManager.getHikari().getConnection();
                    preparedStatement = connection.prepareStatement(sql);
                    preparedStatement.setString(1, nickPlayer.getUuid().toString());
                    preparedStatement.setString(2, nickPlayer.getName());
                    if(nickPlayer.isKeepSkin() || nickPlayer.getNickedSkin() == null)
                        preparedStatement.setInt(3, 1);
                    else
                        preparedStatement.setInt(3, nickPlayer.getNickedSkin().getId());
                    preparedStatement.setBoolean(4, nickPlayer.isKeepSkin());

                    if(nickPlayer.getNickedSkin() == null && !nickPlayer.isKeepSkin()) preparedStatement.setBoolean(5, true);
                    else preparedStatement.setBoolean(5, false);

                    preparedStatement.executeUpdate();
                    hasSavedNickPlayer(nickPlayer);
                } catch (SQLException throwables) {
                    throwables.printStackTrace();
                } finally {
                    MySQLManager.closeConnections(connection, preparedStatement, null);
                }
            }
        }.runTaskAsynchronously(plugin);
    }

    public void removeActiveNickPlayer(NickPlayer nickPlayer) {
        new BukkitRunnable() {
            @Override
            public void run() {
                String sql = "DELETE FROM activeNicks WHERE uuid=?";
                Connection connection = null;
                PreparedStatement preparedStatement = null;
                try {
                    connection = MySQLManager.getHikari().getConnection();
                    preparedStatement = connection.prepareStatement(sql);
                    preparedStatement.setString(1, nickPlayer.getUuid().toString());
                    preparedStatement.executeUpdate();
                } catch (SQLException throwables) {
                    throwables.printStackTrace();
                } finally {
                    MySQLManager.closeConnections(connection, preparedStatement, null);
                }
            }
        }.runTaskAsynchronously(plugin);
    }

    public void insertSavedNickPlayer(NickPlayer nickPlayer) {
        new BukkitRunnable() {
            @Override
            public void run() {
                String sql = "INSERT INTO savedNicks (uuid, name,SkinID, keepSkin, keepDefaultSkin) VALUES (?,?,?,?,?)";
                Connection connection = null;
                PreparedStatement preparedStatement = null;

                try {
                    connection = MySQLManager.getHikari().getConnection();
                    preparedStatement = connection.prepareStatement(sql);
                    preparedStatement.setString(1, nickPlayer.getUuid().toString());
                    preparedStatement.setString(2, nickPlayer.getName());
                    if(nickPlayer.isKeepSkin()  || nickPlayer.getNickedSkin() == null)
                        preparedStatement.setInt(3, 1);
                    else
                        preparedStatement.setInt(3, nickPlayer.getNickedSkin().getId());
                    preparedStatement.setBoolean(4, nickPlayer.isKeepSkin());

                    if(nickPlayer.getNickedSkin() == null && !nickPlayer.isKeepSkin()) preparedStatement.setBoolean(5, true);
                    else preparedStatement.setBoolean(5, false);

                    preparedStatement.executeUpdate();
                } catch (SQLException throwables) {
                    throwables.printStackTrace();
                } finally {
                    MySQLManager.closeConnections(connection, preparedStatement, null);
                }
            }
        }.runTaskAsynchronously(plugin);
    }

    public void updateNickPlayer(NickPlayer nickPlayer) {
        new BukkitRunnable() {
            @Override
            public void run() {
                String sql = "UPDATE savedNicks SET name=?, SkinID=?, keepSkin=?, keepDefaultSkin=? WHERE uuid=?";
                Connection connection = null;
                PreparedStatement preparedStatement = null;
                try {
                    connection = MySQLManager.getHikari().getConnection();
                    preparedStatement = connection.prepareStatement(sql);
                    preparedStatement.setString(1, nickPlayer.getName());
                    if(nickPlayer.isKeepSkin() || nickPlayer.getNickedSkin() == null)
                        preparedStatement.setInt(2, 1);
                    else
                        preparedStatement.setInt(2, nickPlayer.getNickedSkin().getId());
                    preparedStatement.setBoolean(3, nickPlayer.isKeepSkin());
                    preparedStatement.setString(5, nickPlayer.getUuid().toString());

                    if(nickPlayer.getNickedSkin() == null && !nickPlayer.isKeepSkin()) preparedStatement.setBoolean(4, true);
                    else preparedStatement.setBoolean(4, false);
                    preparedStatement.executeUpdate();
                } catch (SQLException throwables) {
                    throwables.printStackTrace();
                } finally {
                    MySQLManager.closeConnections(connection, preparedStatement, null);
                }
            }
        }.runTaskAsynchronously(plugin);
    }

    public void hasSavedNickPlayer(NickPlayer nickPlayer) {
        new BukkitRunnable() {
            @Override
            public void run() {
                String sql = "SELECT * FROM savedNicks WHERE uuid=?";
                Connection connection = null;
                PreparedStatement preparedStatement = null;
                ResultSet resultSet = null;
                try {
                    connection = MySQLManager.getHikari().getConnection();
                    preparedStatement = connection.prepareStatement(sql);
                    preparedStatement.setString(1, nickPlayer.getUuid().toString());
                    resultSet = preparedStatement.executeQuery();
                    if(resultSet.next()) updateNickPlayer(nickPlayer);
                    else insertSavedNickPlayer(nickPlayer);
                } catch (SQLException throwables) {
                    throwables.printStackTrace();
                } finally {
                    MySQLManager.closeConnections(connection, preparedStatement, resultSet);
                }
            }
        }.runTaskAsynchronously(plugin);
    }

    public void loadSavedNickPlayer(NickPlayer nickPlayer) {
        new BukkitRunnable() {
            @Override
            public void run() {
                String sql = "SELECT savedNicks.name, nickSkins.id, nickSkins.name, nickSkins.value, nickSkins.signature, savedNicks.keepDefaultSkin FROM savedNicks " +
                        "INNER JOIN nickSkins ON nickSkins.id = savedNicks.SkinID WHERE uuid=?";
                Connection connection = null;
                PreparedStatement preparedStatement = null;
                ResultSet resultSet = null;

                try {
                    connection = MySQLManager.getHikari().getConnection();
                    preparedStatement = connection.prepareStatement(sql);
                    preparedStatement.setString(1, nickPlayer.getUuid().toString());
                    resultSet = preparedStatement.executeQuery();
                    if(resultSet.next()) {
                        if(!resultSet.getBoolean(6)) {
                            NickedSkin skin = new NickedSkin(resultSet.getInt(2), resultSet.getString(3), resultSet.getString(4), resultSet.getString(5));
                            nickPlayer.setPreviousSkin(skin);
                        }
                        nickPlayer.setPreviousName(resultSet.getString(1));

                    }

                } catch (SQLException throwables) {
                    throwables.printStackTrace();
                } finally {
                    MySQLManager.closeConnections(connection, preparedStatement, resultSet);
                }
            }
        }.runTaskAsynchronously(plugin);
    }



    private void getRandomNames() {
        String sql = "SELECT * FROM nickNames";
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        try {
            connection = MySQLManager.getHikari().getConnection();
            preparedStatement = connection.prepareStatement(sql);
            resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                names.put(resultSet.getInt(1), resultSet.getString(2));
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        } finally {
            MySQLManager.closeConnections(connection, preparedStatement, resultSet);
        }

    }

    public void getRandomSkin(Consumer<NickedSkin> callback) {
        new BukkitRunnable() {
            @Override
            public void run() {
                String sql = "SELECT * FROM nickSkins ORDER BY RAND() LIMIT 1";
                Connection connection = null;
                PreparedStatement preparedStatement = null;
                ResultSet resultSet = null;
                try {
                    connection = MySQLManager.getHikari().getConnection();
                    preparedStatement = connection.prepareStatement(sql);
                    resultSet = preparedStatement.executeQuery();
                    if(!resultSet.next()) {
                        callback.accept(null);
                        return;
                    }

                    callback.accept(new NickedSkin(resultSet.getInt(1), resultSet.getString(2), resultSet.getString(3), resultSet.getString(4)));

                } catch (SQLException throwables) {
                    throwables.printStackTrace();
                } finally {
                    MySQLManager.closeConnections(connection, preparedStatement, resultSet);
                }
            }
        }.runTaskAsynchronously(plugin);
    }

    public void getPreviousSkin(UUID uuid, Consumer<NickedSkin> callback) {
        new BukkitRunnable() {
            @Override
            public void run() {
                String sql = "SELECT nickSkins.id, nickSkins.name, nickSkins.value, nickSkins.signature FROM savedNicks INNER JOIN nickSkins ON nickSkins.id = savedNicks.SkinID WHERE uuid=?";
                Connection connection = null;
                PreparedStatement preparedStatement = null;
                ResultSet resultSet = null;
                try {
                    connection = MySQLManager.getHikari().getConnection();
                    preparedStatement = connection.prepareStatement(sql);
                    preparedStatement.setString(1, uuid.toString());
                    resultSet = preparedStatement.executeQuery();
                    if(!resultSet.next()) {
                        callback.accept(null);
                        return;
                    }

                    callback.accept(new NickedSkin(resultSet.getInt(1), resultSet.getString(2), resultSet.getString(3), resultSet.getString(4)));

                } catch (SQLException throwables) {
                    throwables.printStackTrace();
                } finally {
                    MySQLManager.closeConnections(connection, preparedStatement, resultSet);
                }
            }
        }.runTaskAsynchronously(plugin);
    }

//----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------


    public void updatePrefix(NickPlayer nickPlayer) {
        String prefix = getRankPrefix(nickPlayer.getRank());
        User user = NickPlugin.getLuckPerms().getUserManager().getUser(nickPlayer.getUuid());
        Player p = Bukkit.getPlayer(nickPlayer.getName());

        PrefixNode node = PrefixNode.builder(prefix, 101).value(true).build();

        user.data().add(node);

        NickPlugin.getLuckPerms().getUserManager().saveUser(user);
        if(NickPlugin.getTabAPI() != null) {
            TabAPI.getInstance().getTablistFormatManager().setPrefix(TabAPI.getInstance().getPlayer(nickPlayer.getUuid()), prefix);
            TabAPI.getInstance().getTablistFormatManager().setName(TabAPI.getInstance().getPlayer(nickPlayer.getUuid()), nickPlayer.getName());
        }
    }

    public void removePrefix(NickPlayer nickPlayer) {
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + nickPlayer.getOgName() + " meta removeprefix 101");
        if(NickPlugin.getTabAPI() != null){
            TabAPI.getInstance().getTablistFormatManager().setName(TabAPI.getInstance().getPlayer(nickPlayer.getUuid()), nickPlayer.getOgName());
        }
    }
    public void nickPlayer(NickPlayer nickPlayer) {
        nickPlayer.setNicked(true);
        Player player = Bukkit.getPlayer(nickPlayer.getUuid());

        CraftPlayer craftPlayer = (CraftPlayer) player;
        PlayerUtil.changeName(player, nickPlayer.getName());

        PlayerUtil.sendPacket(player, new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.REMOVE_PLAYER, craftPlayer.getHandle()));

        if(nickPlayer.getNickedSkin() == null) {
            GameProfile profile = craftPlayer.getProfile();
            profile.getProperties().clear();
        } else if(!nickPlayer.isKeepSkin()) {
            GameProfile profile = craftPlayer.getProfile();
            profile.getProperties().removeAll("textures");
            profile.getProperties().put("textures", nickPlayer.getNickedSkin().getProperty());
        }


        PacketPlayOutRespawn respawn = new PacketPlayOutRespawn(0, EnumDifficulty.valueOf(player.getWorld().getDifficulty().name()), craftPlayer.getHandle().getWorld().G(), WorldSettings.EnumGamemode.valueOf(player.getGameMode().name()));
        Location location = player.getLocation();
        double health = player.getHealth();
        int food = player.getFoodLevel();
        int heldItem = player.getInventory().getHeldItemSlot();
        boolean flying = player.isFlying();
        GameMode gameMode = player.getGameMode();

        PlayerUtil.sendPacket(player, respawn);
        player.teleport(location);
        player.setHealth(health);
        player.setFoodLevel(food);
        player.getInventory().setHeldItemSlot(heldItem);
        player.setFlying(flying);
        player.setGameMode(gameMode);
        player.updateInventory();

        PlayerUtil.sendPacket(player, new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.ADD_PLAYER, craftPlayer.getHandle()));
        player.setDisplayName(nickPlayer.getName());
        player.setPlayerListName(nickPlayer.getName());
        updatePrefix(nickPlayer);
        PlayerUtil.hidePlayer(player);
        PlayerUtil.showPlayer(player);
        player.sendMessage(ChatColor.GREEN + "You are now nicked as " + nickPlayer.getName());
        if(NickPlugin.getServerType().equals(ServerType.LOBBY)) {
            new RepeatNickTask(nickPlayer).runTaskTimerAsynchronously(plugin, 0, 20);
        }
    }

    public void unnick(NickPlayer nickPlayer) {
        removePrefix(nickPlayer);
        nickPlayer.setNicked(false);
        Player player = Bukkit.getPlayer(nickPlayer.getUuid());

        CraftPlayer craftPlayer = (CraftPlayer) player;

        PlayerUtil.sendPacket(player, new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.REMOVE_PLAYER, craftPlayer.getHandle()));
        PlayerUtil.changeName(player, nickPlayer.getOgName());
        GameProfile profile = craftPlayer.getProfile();
        profile.getProperties().removeAll("textures");
        profile.getProperties().put("textures", nickPlayer.getOgSkin());

        PacketPlayOutRespawn respawn = new PacketPlayOutRespawn(0, EnumDifficulty.valueOf(player.getWorld().getDifficulty().name()), craftPlayer.getHandle().getWorld().G(), WorldSettings.EnumGamemode.valueOf(player.getGameMode().name()));
        Location location = player.getLocation();
        double health = player.getHealth();
        int food = player.getFoodLevel();
        int heldItem = player.getInventory().getHeldItemSlot();
        boolean flying = player.isFlying();
        GameMode gameMode = player.getGameMode();

        PlayerUtil.sendPacket(player, respawn);
        player.teleport(location);
        player.setHealth(health);
        player.setFoodLevel(food);
        player.getInventory().setHeldItemSlot(heldItem);
        player.setFlying(flying);
        player.setGameMode(gameMode);
        player.updateInventory();

        PlayerUtil.sendPacket(player, new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.ADD_PLAYER, craftPlayer.getHandle()));
        player.setDisplayName(nickPlayer.getOgName());
        player.setPlayerListName(nickPlayer.getOgName());
        removePrefix(nickPlayer);
        PlayerUtil.hidePlayer(player);
        PlayerUtil.showPlayer(player);
        player.sendMessage(ChatColor.GREEN + "You unnicked!");
    }



    public static NickPlayer getNickPlayer(UUID uuid) {
        for(NickPlayer nickPlayer : nickPlayers) {
            if(nickPlayer.getUuid() == uuid) {
                return nickPlayer;
            }
        }
        return null;
    }

    public boolean hasNickPlayer(UUID uuid) {
        return nickPlayers.stream().anyMatch(nickPlayer -> nickPlayer.getUuid().equals(uuid));
    }

    public void addNickPlayer(UUID uuid) {
        if(!hasNickPlayer(uuid)) {
            Player player = Bukkit.getPlayer(uuid);
            NickPlayer nickPlayer = new NickPlayer(uuid);
            nickPlayer.setOgName(player.getName());
            Property property = ((CraftPlayer) player).getProfile().getProperties().get("textures").stream().filter(property1 -> property1.getName().equals("textures")).findFirst().orElse(new Property("textures", ""));
            nickPlayer.setOgSkin(property);
            nickPlayers.add(nickPlayer);
            loadSavedNickPlayer(nickPlayer);
        }
    }

    public void removeNickPlayer(NickPlayer nickPlayer) {
        nickPlayers.remove(nickPlayer);
    }

    public String getRandomName() {
        return names.get(new Random().nextInt(names.size()));
    }

    public String getRankPrefix(NickRank rank) {
        switch (rank){
            case DEFAULT:
                return "&7";
            case VIP:
                return "&a[VIP] ";
            case VIPP:
                return "&a[VIP&6+&a] ";
            case MVP:
                return "&b[MVP] ";
            case MVPP:
                return "&b[MVP&c+&b] ";
        }
        return "&7";
    }


}
