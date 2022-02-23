package com.example.downloadspeedtester;

public class Speed {

    String speed;
    String location;
    String network;

    public Speed(String speed, String location, String network) {
        this.speed = speed;
        this.location = location;
        this.network = network;
    }

    public String getSpeed() {
        return speed;
    }

    public String getLocation() {
        return location;
    }

    public String getNetwork() {
        return network;
    }
}
