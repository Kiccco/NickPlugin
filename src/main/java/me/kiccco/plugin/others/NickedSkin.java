package me.kiccco.plugin.others;

import com.mojang.authlib.properties.Property;

public class NickedSkin {

    private int id;
    private String name;
    private Property property;

    public NickedSkin(int id, String name, String value, String signature) {
        this.id = id;
        this.name = name;
        this.property = new Property("textures", value, signature);
    }


    public Property getProperty() {
        return property;
    }

    public String getName() {
        return name;
    }

    public int getId() {
        return id;
    }

    public void setProperty(Property property) {
        this.property = property;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setId(int id) {
        this.id = id;
    }
}
