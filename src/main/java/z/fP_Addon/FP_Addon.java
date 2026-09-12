package z.fP_Addon;

import net.flectone.pulse.FlectonePulse;
import net.flectone.pulse.FlectonePulseAPI;
import net.flectone.pulse.platform.registry.ListenerRegistry;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import z.fP_Addon.command.FpaCommand;
import z.fP_Addon.config.Config;
import z.fP_Addon.listener.BanListener;
import z.fP_Addon.listener.IpAlertListener;
import z.fP_Addon.listener.UnbanListener;
import z.fP_Addon.storage.BannedIpRepository;

public final class FP_Addon extends JavaPlugin {

    private Config fpConfig;
    private BannedIpRepository bannedIpRepository;

    private FlectonePulse flectonePulse;
    private IpAlertListener ipAlertListener;
    private BukkitTask readyCheckTask;

    @Override
    public void onEnable() {
        fpConfig = new Config(this);
        fpConfig.load();

        bannedIpRepository = new BannedIpRepository(this);
        bannedIpRepository.init();

        if (getCommand("fpa") != null) {
            getCommand("fpa").setExecutor(new FpaCommand(this));
        }

        if (!Bukkit.getPluginManager().isPluginEnabled("FlectonePulse")) {
            getLogger().warning("FlectonePulse не найден на сервере. "
                    + "Аддон будет выключен");
            return;
        }

        hookFlectonePulse();
    }

    private void hookFlectonePulse() {
        FlectonePulse instance = FlectonePulseAPI.getInstance();

        if (instance != null && instance.isReady()) {
            this.flectonePulse = instance;
            registerPulseListeners();
            return;
        }

        readyCheckTask = Bukkit.getScheduler().runTaskTimer(this, () -> {
            FlectonePulse candidate = FlectonePulseAPI.getInstance();
            if (candidate == null || !candidate.isReady()) return;

            this.flectonePulse = candidate;
            registerPulseListeners();

            if (readyCheckTask != null) {
                readyCheckTask.cancel();
            }
        }, 20L, 20L);
    }

    private void registerPulseListeners() {
        ListenerRegistry listenerRegistry = flectonePulse.get(ListenerRegistry.class);

        BanListener banListener = new BanListener(this, flectonePulse, fpConfig, bannedIpRepository);
        UnbanListener unbanListener = new UnbanListener(this, flectonePulse, fpConfig, bannedIpRepository);
        ipAlertListener = new IpAlertListener(this, flectonePulse, fpConfig);

        listenerRegistry.registerPermanent(banListener);
        listenerRegistry.registerPermanent(unbanListener);
        listenerRegistry.registerPermanent(ipAlertListener);

        getLogger().info("FP_Addon успешно подключился к FlectonePulse API");

        ipAlertListener.registerCommandsOnce();
    }

    @Override
    public void onDisable() {
        if (readyCheckTask != null) {
            readyCheckTask.cancel();
        }

        if (bannedIpRepository != null) {
            bannedIpRepository.close();
        }
    }

    public Config getFpConfig() {
        return fpConfig;
    }

    public BannedIpRepository getBannedIpRepository() {
        return bannedIpRepository;
    }
}
