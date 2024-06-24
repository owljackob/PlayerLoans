package cz.sovajakub.economy.manager;

import cz.sovajakub.economy.localization.LocalizationMessage;
import cz.sovajakub.economy.util.TranslateUtil;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.HashMap;

public class ConfigManager {
    private static YamlConfiguration messageYamlConfiguration;
    private static final HashMap<String, Object> CONFIG_VALUES = new HashMap<>();
    private static final HashMap<LocalizationMessage, String> MESSAGE_CACHE = new HashMap<>();

    public static void initManager(File dataFolder, @NotNull Configuration pluginConfiguration) {
        //Init values
        CONFIG_VALUES.put("DATABASE_HOST", pluginConfiguration.getString("Host", "localhost"));
        CONFIG_VALUES.put("DATABASE_PORT", pluginConfiguration.getString("Port", "3306"));
        CONFIG_VALUES.put("DATABASE_NAME", pluginConfiguration.getString("Name", ""));
        CONFIG_VALUES.put("DATABASE_USER", pluginConfiguration.getString("User", "root"));
        CONFIG_VALUES.put("DATABASE_PASSWORD", pluginConfiguration.getString("Password", ""));
        //Init messages
        final File MESSAGE_FILE = new File(dataFolder + File.separator + "localization" + File.separator, "messages_" + pluginConfiguration.getString("Language") + ".yml");
        messageYamlConfiguration = YamlConfiguration.loadConfiguration(MESSAGE_FILE);
    }

    public static String getMessage(@NotNull LocalizationMessage localizationMessage) {
        if (!MESSAGE_CACHE.containsKey(localizationMessage)) {
            MESSAGE_CACHE.put(localizationMessage, TranslateUtil.translateMessage(messageYamlConfiguration.getString(localizationMessage.name().toLowerCase())));
        }
        return MESSAGE_CACHE.get(localizationMessage);
    }

    public static Object getObject(String objectKey) {
        return CONFIG_VALUES.get(objectKey);
    }
}