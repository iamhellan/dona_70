package org.example;

import java.io.IOException;
import java.util.Properties;

public class ConfigHelper {
    private static final Properties props = new Properties();

    static {
        try {
            props.load(ConfigHelper.class.getClassLoader().getResourceAsStream("config.properties"));
        } catch (IOException e) {
            throw new RuntimeException("Не удалось загрузить config.properties", e);
        }
    }

    public static String get(String key) {
        return props.getProperty(key);
    }
}
