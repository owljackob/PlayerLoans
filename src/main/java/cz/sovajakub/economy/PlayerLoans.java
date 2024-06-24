package cz.sovajakub.economy;

import cz.sovajakub.economy.checker.LoanExpireChecker;
import cz.sovajakub.economy.command.LoanCommand;
import cz.sovajakub.economy.listener.AgreeEventListener;
import cz.sovajakub.economy.listener.DisagreeEventListener;
import cz.sovajakub.economy.manager.ConfigManager;
import cz.sovajakub.economy.manager.DatabaseManager;
import cz.sovajakub.economy.manager.RequestManager;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPIConfig;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import us.figt.mesh.utils.PluginUtil;

import java.io.File;
import java.util.logging.Logger;

public class PlayerLoans extends JavaPlugin {

    private static final BukkitScheduler BUKKIT_SCHEDULER = Bukkit.getScheduler();
    private static final Logger PLUGIN_LOGGER = Bukkit.getLogger();
    private static Economy serverEconomy = null;

    @Override
    public void onEnable() {
        //Init mesh
        PluginUtil.setPlugin(this);
        //Init config
        saveDefaultConfig();
        final File MESSAGES_EN = new File(getDataFolder() + File.separator + "localization" + File.separator + "messages_en.yml");
        if (!MESSAGES_EN.exists()) {
            saveResource("localization" + File.separator + "messages_en.yml", false);
        }
        //Init managers
        ConfigManager.initManager(getDataFolder(), getConfig());
        RequestManager.initManager(this, BUKKIT_SCHEDULER);
        //Init database
        DatabaseManager.initManager();
        DatabaseManager.connectToDataSource();
        DatabaseManager.createTable().acceptSync(isCreated -> {
            if (!isCreated) {
                PLUGIN_LOGGER.severe("We couldn't create the table");
            }
        });
        //Init economy
        RegisteredServiceProvider<Economy> registeredServiceProvider = getServer().getServicesManager().getRegistration(Economy.class);
        if (registeredServiceProvider == null) {
            return;
        }
        serverEconomy = registeredServiceProvider.getProvider();
        //Init commands
        CommandAPI.onEnable(this);
        LoanCommand.register();
        //Init listeners
        Bukkit.getPluginManager().registerEvents(new AgreeEventListener(serverEconomy), this);
        Bukkit.getPluginManager().registerEvents(new DisagreeEventListener(), this);
        //Init checkers
        new LoanExpireChecker(this, BUKKIT_SCHEDULER, PLUGIN_LOGGER, serverEconomy).runChecker();
    }

    @Override
    public void onLoad() {
        CommandAPI.onLoad(new CommandAPIConfig());
        super.onLoad();
    }

    @Override
    public void onDisable() {
        DatabaseManager.disconnectFromDataSource();
        CommandAPI.onDisable();
    }

    public static Economy getEconomy() {
        return serverEconomy;
    }
}