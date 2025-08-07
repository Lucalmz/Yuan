package com.bear27570.yuan.BotFactory;

public class ConfigDirectionPair {
    private String Config;

    private boolean isReverse;
    public ConfigDirectionPair(String Config,boolean isReverse){
        this.Config = Config;
        this.isReverse = isReverse;
    }
    public String getConfig() {
        return Config;
    }

    public void setConfig(String config) {
        Config = config;
    }
    public boolean isReverse() {
        return isReverse;
    }

    public void setReverse(boolean reverse) {
        isReverse = reverse;
    }

}