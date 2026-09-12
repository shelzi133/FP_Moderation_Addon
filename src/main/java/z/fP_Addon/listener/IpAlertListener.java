package z.fP_Addon.listener;

import net.flectone.pulse.FlectonePulse;
import net.flectone.pulse.annotation.Pulse;
import net.flectone.pulse.checker.PermissionChecker;
import net.flectone.pulse.model.entity.FPlayer;
import net.flectone.pulse.model.event.Event;
import net.flectone.pulse.model.event.lifecycle.EnableEvent;
import net.flectone.pulse.model.event.player.PlayerJoinEvent;
import net.flectone.pulse.platform.adapter.PlatformPlayerAdapter;
import net.flectone.pulse.platform.registry.CommandRegistry;
import net.flectone.pulse.platform.sender.MessageSender;
import net.flectone.pulse.service.FPlayerService;
import net.flectone.pulse.service.SocialService;
import net.flectone.pulse.listener.PulseListener;
import z.fP_Addon.FP_Addon;
import z.fP_Addon.config.Config;
import z.fP_Addon.utils.PulseHEXColor;

import java.util.List;
import java.util.UUID;

public class IpAlertListener implements PulseListener {

    private static final String IP_ALERT_SETTING_KEY = "fp_addon_ip_alert";

    private final FP_Addon plugin;
    private final FlectonePulse flectonePulse;
    private final Config config;

    private boolean commandsRegistered = false;

    public IpAlertListener(FP_Addon plugin, FlectonePulse flectonePulse, Config config) {
        this.plugin = plugin;
        this.flectonePulse = flectonePulse;
        this.config = config;
    }


    @Pulse
    public void onEnable(EnableEvent event) {
        if (event.type() != EnableEvent.Type.READY) return;
        registerCommandsOnce();
    }

    public synchronized void registerCommandsOnce() {
        if (commandsRegistered) return;
        commandsRegistered = true;

        try {
            CommandRegistry commandRegistry = flectonePulse.get(CommandRegistry.class);
            MessageSender messageSender = flectonePulse.get(MessageSender.class);

            commandRegistry.registerCommand(manager -> manager
                    .commandBuilder("ip-alert")
                    .permission("fp_addon.moderation")
                    .handler(context -> {
                        FPlayer fPlayer = context.sender();
                        SocialService socialService = flectonePulse.get(SocialService.class);

                        boolean currentlyEnabled = socialService.isSetting(fPlayer, IP_ALERT_SETTING_KEY);
                        boolean newValue = !currentlyEnabled;
                        socialService.saveSetting(fPlayer, IP_ALERT_SETTING_KEY, newValue);

                        String path = newValue ? "ip-alert-enabled" : "ip-alert-disabled";
                        messageSender.sendMessage(fPlayer, PulseHEXColor.colorize(config.raw(path)), false);
                    })
            );
        } catch (Exception e) {
            plugin.getLogger().severe("Не удалось зарегистрировать /ip-alert: " + e);
        }
    }

    @Pulse(priority = Event.Priority.MONITOR, ignoreCancelled = true)
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!config.isIpAlertEnabled()) return;

        FPlayer fPlayer = event.player();
        if (fPlayer.isUnknown() || fPlayer.isConsole()) return;

        String ip = fPlayer.ip();
        if (ip == null) return;

        PlatformPlayerAdapter platformPlayerAdapter = flectonePulse.get(PlatformPlayerAdapter.class);

        boolean firstTimeAccount = !platformPlayerAdapter.hasPlayedBefore(fPlayer.uuid());
        if (!firstTimeAccount) return;

        FPlayerService fPlayerService = flectonePulse.get(FPlayerService.class);

        List<FPlayer> accountsWithIp = fPlayerService.getFPlayersByIp(ip);
        int totalWithIp = accountsWithIp == null ? 0 : accountsWithIp.size();
        if (totalWithIp <= 1) return;

        notifyModerators(fPlayer, totalWithIp);
    }

    private void notifyModerators(FPlayer newAccount, int totalAccountsWithIp) {
        SocialService socialService = flectonePulse.get(SocialService.class);
        PermissionChecker permissionChecker = flectonePulse.get(PermissionChecker.class);
        MessageSender messageSender = flectonePulse.get(MessageSender.class);
        FPlayerService fPlayerService = flectonePulse.get(FPlayerService.class);

        String text = config.message("ip-alert-notify",
                "%player%", newAccount.name(),
                "%accounts%", String.valueOf(totalAccountsWithIp));

        int notified = 0;
        for (FPlayer online : fPlayerService.getOnlineFPlayers()) {
            UUID onlineUuid = online.uuid();
            if (onlineUuid != null && onlineUuid.equals(newAccount.uuid())) continue;
            if (!permissionChecker.check(online, "fp_addon.moderation")) continue;
            if (!socialService.isSetting(online, IP_ALERT_SETTING_KEY)) continue;

            messageSender.sendMessage(online, PulseHEXColor.colorize(text), false);
            notified++;
        }
    }
}
