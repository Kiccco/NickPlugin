package me.kiccco.plugin.utils;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.server.v1_8_R3.*;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;

public class PlayerUtil {

    public static void sendPacket(Player player, Packet packet){
        ((CraftPlayer) player).getHandle().playerConnection.sendPacket(packet);
    }

    public static void hidePlayer(Player player) {
        Bukkit.getOnlinePlayers().stream().filter(p -> p != player).forEach(p -> p.hidePlayer(player));
    }

    public static void showPlayer(Player player) {
        Bukkit.getOnlinePlayers().stream().filter(p -> p != player).forEach(p -> p.showPlayer(player));
    }

    public static void changeName(Player player, String name){
        CraftPlayer craftPlayer = (CraftPlayer) player;
        try {
            Object profile = craftPlayer.getClass().getMethod("getProfile").invoke(craftPlayer);
            Field nameField = profile.getClass().getDeclaredField("name");
            nameField.setAccessible(true);
            nameField.set(profile, name);

            Field playersByName = PlayerList.class.getDeclaredField("playersByName");
            playersByName.setAccessible(true);
            Map map = (Map) playersByName.get(MinecraftServer.getServer().getPlayerList());
            map.remove(player.getName());
            map.put(player.getName(), ((CraftPlayer) player).getHandle());

        } catch (InvocationTargetException | IllegalAccessException | NoSuchMethodException | NoSuchFieldException e) {
            e.printStackTrace();
        }

    }

    public static void openBook(Player player, ItemStack book) {
        int slot = player.getInventory().getHeldItemSlot();
        ItemStack old = player.getInventory().getItem(slot);
        player.getInventory().setItem(slot, book);
        ByteBuf buf = Unpooled.buffer(256);
        buf.setByte(0, (byte) 0);
        buf.writerIndex(1);
        PacketPlayOutCustomPayload packet = new PacketPlayOutCustomPayload("MC|BOpen", new PacketDataSerializer(buf));
        sendPacket(player, packet);
        player.getInventory().setItem(slot, old);
    }

    public static void sendActionBar(Player player, String value) {
        IChatBaseComponent chatBaseComponent = IChatBaseComponent.ChatSerializer.a("{\"text\":\"" + value + "\"}");

        PacketPlayOutChat packet = new PacketPlayOutChat(chatBaseComponent, (byte) 2);
        sendPacket(player, packet);
    }
}
