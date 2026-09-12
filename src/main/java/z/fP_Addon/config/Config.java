package z.fP_Addon.config;

import org.bukkit.configuration.file.FileConfiguration;
import z.fP_Addon.FP_Addon;

public class  Config {

    private final FP_Addon plugin;
    private FileConfiguration cfg;

    public Config(FP_Addon plugin) {
        this.plugin = plugin;
    }

    public void load() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        cfg = plugin.getConfig();
    }

    public void reload() {
        load();
    }

    public boolean isAlsoBanIpEnabled() {
        return cfg.getBoolean("also-banip.enabled", false);
    }

    public boolean isAlsoBanAllAccountEnabled() {
        return cfg.getBoolean("also-ban-all-account.enabled", false);
    }

    public boolean isIpAlertEnabled() {
        return cfg.getBoolean("ip-alert.enabled", false);
    }

    public String raw(String path) {
        return cfg.getString("messages." + path,
                "&aСообщение 'messages." + path + "' не найдено в config.yml");
    }

    public String message(String path, String... placeholdersAndValues) {
        String text = raw(path);
        for (int i = 0; i + 1 < placeholdersAndValues.length; i += 2) {
            text = text.replace(placeholdersAndValues[i], placeholdersAndValues[i + 1]);
        }
        return text;
    }

}
