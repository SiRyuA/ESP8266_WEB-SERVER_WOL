package com.example.esp8266_webserver_wol;


public class WoLItem {
    private String name;
    private String url;
    private String key;
    private String mac;


    public void setName(String set) {
        name = set;
    }

    public void setUrl(String set) {
        url = set;
    }

    public void setKey(String set) {
        key = set;
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

    public String getUrl() {
        return this.url;
    }

    public String getKey() {
        return this.key;
    }
}
