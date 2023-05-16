package me.kiccco.plugin.utils;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.properties.Property;
import me.kiccco.plugin.NickPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.function.Consumer;

public class SkinFetcher {

    public static String getFromURL(String name) {
        try {
            URL url = new URL(name);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            if(connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder content = new StringBuilder();
                String line;
                while (((line = reader.readLine()) != null)) {
                    content.append(line);
                }
                return content.toString();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void getSkin(String name, Consumer<Property> callback) {
        new BukkitRunnable() {
            @Override
            public void run() {
                String uuidResponse = getFromURL("https://api.mojang.com/users/profiles/minecraft/" + name);
                if(uuidResponse == null) {
                    callback.accept(null);
                    return;
                }
                JsonObject uuidObject = (JsonObject) new JsonParser().parse(uuidResponse);
                String uuid = uuidObject.get("id").getAsString();

                String skinResponse = getFromURL("https://sessionserver.mojang.com/session/minecraft/profile/" + uuid + "?unsigned=false");
                if(skinResponse == null) {
                    callback.accept(null);
                    return;
                }
                JsonObject skinObject = (JsonObject) new JsonParser().parse(skinResponse);
                String value = skinObject.getAsJsonArray("properties").get(0).getAsJsonObject().get("value").getAsString();
                String signature = skinObject.getAsJsonArray("properties").get(0).getAsJsonObject().get("signature").getAsString();
                callback.accept(new Property("textures", value, signature));
            }
        }.runTaskAsynchronously(NickPlugin.getInstance());
    }
}
