package ru.zont.uinondsb;

import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

public class Globals {
    public static int gamePort = 2302;

    static {
        final Properties properties = new Properties();
        try {
            properties.load(Globals.class.getClassLoader().getResourceAsStream("version.properties"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        final String v = properties.getProperty("version");
        version = v != null ? v : "SNAPSHOT";
    }

    public static final String version;

    public static String dbConnection;
}
