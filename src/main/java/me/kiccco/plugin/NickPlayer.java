package me.kiccco.plugin;

import com.mojang.authlib.properties.Property;
import me.kiccco.plugin.others.NickedSkin;
import me.kiccco.plugin.others.NickRank;
import org.bukkit.Location;

import java.util.UUID;

public class NickPlayer {


    private UUID uuid;
    private String ogName;
    private Location sign;
    private Property ogSkin;

    private boolean keepSkin = false;
    private boolean nicked = false;

    //Skin from db
    private NickedSkin nickedSkin;

    //Name data
    private String name;

    private NickRank rank;

    //Previous data
    private NickedSkin previousSkin;
    private String previousName;

    public NickPlayer(UUID uuid) {
        this.uuid = uuid;
    }

    public String getName() {
        return name;
    }

    public Location getSign() {
        return sign;
    }

    public UUID getUuid() {
        return uuid;
    }

    public Property getOgSkin() {
        return ogSkin;
    }

    public NickedSkin getNickedSkin() {
        return nickedSkin;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public void setSign(Location sign) {
        this.sign = sign;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setOgSkin(Property ogSkin) {
        this.ogSkin = ogSkin;
    }

    public void setNickedSkin(NickedSkin nickedSkin) {
        this.nickedSkin = nickedSkin;
    }


    public boolean isKeepSkin() {
        return keepSkin;
    }

    public void setKeepSkin(boolean keepSkin) {
        this.keepSkin = keepSkin;
    }

    public NickRank getRank() {
        return rank;
    }

    public void setRank(NickRank rank) {
        this.rank = rank;
    }


    public NickedSkin getPreviousSkin() {
        return previousSkin;
    }

    public String getPreviousName() {
        return previousName;
    }

    public void setPreviousName(String previousName) {
        this.previousName = previousName;
    }

    public void setPreviousSkin(NickedSkin previousSkin) {
        this.previousSkin = previousSkin;
    }

    public String getOgName() {
        return ogName;
    }

    public void setOgName(String ogName) {
        this.ogName = ogName;
    }

    public boolean isNicked() {
        return nicked;
    }

    public void setNicked(boolean nicked) {
        this.nicked = nicked;
    }
}
