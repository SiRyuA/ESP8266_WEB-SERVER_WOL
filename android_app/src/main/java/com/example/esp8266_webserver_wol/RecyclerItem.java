package com.example.esp8266_webserver_wol;

public class RecyclerItem {
    private String name;
    private String mac;

    public void setName(String set) {
        name = set;
    }

    public void setMac(String set) {
        mac = set;
    }

    public String getName() {
        return this.name;
    }

    public String getMac() {
        return this.mac;
    }
}
