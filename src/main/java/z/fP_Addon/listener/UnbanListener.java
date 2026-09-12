package z.fP_Addon.listener;

import net.flectone.pulse.FlectonePulse;
import net.flectone.pulse.annotation.Pulse;
import net.flectone.pulse.constant.ModuleName;
import net.flectone.pulse.model.entity.FPlayer;
import net.flectone.pulse.model.event.message.MessagePrepareEvent;
import net.flectone.pulse.model.event.message.context.MessageContext;
import net.flectone.pulse.model.event.message.context.ModerationMessageContext;
import net.flectone.pulse.model.value.Moderation;
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

public class UnbanListener implements PulseListener {

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

    public UnbanListener(FP_Addon plugin, FlectonePulse flectonePulse,
                         Config config, BannedIpRepository bannedIpRepository) {
        this.plugin = plugin;
        this.flectonePulse = flectonePulse;
        this.config = config;
        this.bannedIpRepository = bannedIpRepository;
    }


    @Pulse
    public void onMessagePrepare(MessagePrepareEvent event) {
        if (event.moduleName() != ModuleName.COMMAND_UNBAN) return;

        MessageContext context = event.messageContext();
        if (!(context instanceof ModerationMessageContext moderationMessageContext)) return;

        Moderation moderation = moderationMessageContext.moderation();
        if (moderation.type() != Moderation.Type.UNBAN) return;

        if (Boolean.TRUE.equals(processedModerationIds.putIfAbsent(moderation.id(), Boolean.TRUE))) {
            return;
        }

        FPlayerService fPlayerService = flectonePulse.get(FPlayerService.class);
        ModerationService moderationService = flectonePulse.get(ModerationService.class);
        MessageSender messageSender = flectonePulse.get(MessageSender.class);

        FPlayer target = fPlayerService.getFPlayer(moderation.player());
        if (target.isUnknown()) {
            plugin.getLogger().warning("Не удалось найти FPlayer по id=" + moderation.player());
            return;
        }

        FPlayer moderator = fPlayerService.getFPlayer(moderation.moderator());

        String targetIp = target.ip();
        boolean wasIpBlocked = targetIp != null && bannedIpRepository.isBanned(targetIp);

        if (targetIp != null) {
            bannedIpRepository.unban(targetIp);
        }

        int extraUnbanned = 0;
        if (targetIp != null && config.isAlsoBanAllAccountEnabled()) {
            extraUnbanned = unbanAllKnownAccountsWithIp(
                    fPlayerService, moderationService, target, targetIp, moderation.moderator()
            );
        }

        if (!moderator.isUnknown()) {
            if (extraUnbanned > 0) {
                messageSender.sendMessage(moderator, PulseHEXColor.colorize(
                        config.message("unbanip-also-unbanned", "%count%", String.valueOf(extraUnbanned))), false);
            }

            if (wasIpBlocked && targetIp != null) {
                messageSender.sendMessage(moderator, PulseHEXColor.colorize(
                        config.message("unbanip-ip-unblocked", "%ip%", targetIp)), false);
            }
        }
    }

    private int unbanAllKnownAccountsWithIp(FPlayerService fPlayerService, ModerationService moderationService,
                                             FPlayer excluded, String ip, int moderatorId) {
        int unbanned = 0;

        List<FPlayer> accounts = fPlayerService.getFPlayersByIp(ip);
        if (accounts == null || accounts.isEmpty()) return 0;

        for (FPlayer account : accounts) {
            if (account.uuid().equals(excluded.uuid())) continue;
            if (!moderationService.hasValid(account, Moderation.Type.BAN)) continue;

            moderationService.invalidate(account, Moderation.Type.BAN, moderatorId);
            boolean stillValid = moderationService.hasValid(account, Moderation.Type.BAN);

            if (!stillValid) unbanned++;
        }

        return unbanned;
    }
}
