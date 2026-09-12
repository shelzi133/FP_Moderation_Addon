package z.fP_Addon.listener;

import net.flectone.pulse.FlectonePulse;
import net.flectone.pulse.annotation.Pulse;
import net.flectone.pulse.constant.ModuleName;
import net.flectone.pulse.model.entity.FPlayer;
import net.flectone.pulse.model.event.Event;
import net.flectone.pulse.model.event.message.MessagePrepareEvent;
import net.flectone.pulse.model.event.message.context.MessageContext;
import net.flectone.pulse.model.event.message.context.ModerationMessageContext;
import net.flectone.pulse.model.event.player.PlayerPreLoginEvent;
import net.flectone.pulse.model.value.Moderation;
import net.flectone.pulse.platform.adapter.PlatformPlayerAdapter;
import net.flectone.pulse.platform.sender.MessageSender;
import net.flectone.pulse.service.FPlayerService;
import net.flectone.pulse.service.ModerationService;
import net.flectone.pulse.listener.PulseListener;
import z.fP_Addon.FP_Addon;
import z.fP_Addon.config.Config;
import z.fP_Addon.storage.BannedIpRepository;
import z.fP_Addon.utils.PulseHEXColor;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class BanListener implements PulseListener {

    private final FP_Addon plugin;
    private final FlectonePulse flectonePulse;
    private final Config config;
    private final BannedIpRepository bannedIpRepository;

    private final Map<Integer, Boolean> processedModerationIds = Collections.synchronizedMap(
            new LinkedHashMap<>(256, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<Integer, Boolean> eldest) {
                    return size() > 200;
                }
            }
    );

    public BanListener(FP_Addon plugin, FlectonePulse flectonePulse,
                       Config config, BannedIpRepository bannedIpRepository) {
        this.plugin = plugin;
        this.flectonePulse = flectonePulse;
        this.config = config;
        this.bannedIpRepository = bannedIpRepository;
    }


    @Pulse
    public void onMessagePrepare(MessagePrepareEvent event) {
        if (event.moduleName() != ModuleName.COMMAND_BAN) return;

        MessageContext context = event.messageContext();
        if (!(context instanceof ModerationMessageContext moderationMessageContext)) return;

        Moderation moderation = moderationMessageContext.moderation();
        if (moderation.type() != Moderation.Type.BAN) return;
        if (!moderation.valid()) return;

        if (Boolean.TRUE.equals(processedModerationIds.putIfAbsent(moderation.id(), Boolean.TRUE))) {
            return;
        }

        FPlayerService fPlayerService = flectonePulse.get(FPlayerService.class);
        ModerationService moderationService = flectonePulse.get(ModerationService.class);
        PlatformPlayerAdapter platformPlayerAdapter = flectonePulse.get(PlatformPlayerAdapter.class);
        MessageSender messageSender = flectonePulse.get(MessageSender.class);

        FPlayer target = fPlayerService.getFPlayer(moderation.player());
        if (target.isUnknown()) {
            plugin.getLogger().warning("Не удалось найти FPlayer по id=" + moderation.player());
            return;
        }

        FPlayer moderator = fPlayerService.getFPlayer(moderation.moderator());
        String reason = moderation.reason() == null ? "Не указана" : moderation.reason();

        String targetIp = target.ip();
        if (targetIp == null) return;

        int extraBanned = 0;

        if (config.isAlsoBanAllAccountEnabled()) {
            extraBanned = banAllKnownAccountsWithIp(
                    fPlayerService, moderationService, platformPlayerAdapter,
                    target, targetIp, reason, moderation.moderator(), moderation.time()
            );
        }

        if (config.isAlsoBanIpEnabled()) {
            bannedIpRepository.ban(targetIp, moderation.time());
        }

        if (!moderator.isUnknown()) {
            if (extraBanned > 0) {
                messageSender.sendMessage(moderator, PulseHEXColor.colorize(
                        config.message("banip-also-banned", "%count%", String.valueOf(extraBanned))), false);
            }

            if (config.isAlsoBanIpEnabled()) {
                messageSender.sendMessage(moderator, PulseHEXColor.colorize(
                        config.message("banip-ip-blocked", "%ip%", targetIp)), false);
            }
        }
    }

    private int banAllKnownAccountsWithIp(FPlayerService fPlayerService, ModerationService moderationService,
                                           PlatformPlayerAdapter platformPlayerAdapter, FPlayer excluded,
                                           String ip, String reason, int moderatorId, long until) {
        int banned = 0;

        List<FPlayer> accounts = fPlayerService.getFPlayersByIp(ip);
        if (accounts == null || accounts.isEmpty()) return 0;

        for (FPlayer account : accounts) {
            if (account.uuid().equals(excluded.uuid())) continue;
            if (moderationService.hasValid(account, Moderation.Type.BAN)) continue;

            Moderation result = moderationService.ban(account, until, reason, moderatorId);
            if (result == null) continue;

            banned++;

            if (account.isOnline()) {
                String kickText = config.message("ban-kick-message", "%reason%", reason);
                platformPlayerAdapter.kick(account, PulseHEXColor.colorize(kickText));
            }
        }

        return banned;
    }

    @Pulse(priority = Event.Priority.HIGH)
    public Event onPreLogin(PlayerPreLoginEvent event) {
        if (!config.isAlsoBanIpEnabled()) return event;

        FPlayer fPlayer = event.player();
        String ip = fPlayer.ip();
        if (ip == null) return event;

        BannedIpRepository.Status status = bannedIpRepository.status(ip);

        if (status.justExpired()) {
            unbanAllKnownAccountsWithIp(ip);
        }

        if (!status.active()) return event;

        return event.withAllowed(false)
                .withKickReason(PulseHEXColor.colorize(config.raw("banip-ip-blocked-kick")));
    }

    private void unbanAllKnownAccountsWithIp(String ip) {
        if (!config.isAlsoBanAllAccountEnabled()) return;

        FPlayerService fPlayerService = flectonePulse.get(FPlayerService.class);
        ModerationService moderationService = flectonePulse.get(ModerationService.class);

        List<FPlayer> accounts = fPlayerService.getFPlayersByIp(ip);
        if (accounts == null || accounts.isEmpty()) return;

        for (FPlayer account : accounts) {
            if (!moderationService.hasValid(account, Moderation.Type.BAN)) continue;

            moderationService.invalidate(account, Moderation.Type.BAN, FPlayer.CONSOLE_ID);
        }
    }
}
