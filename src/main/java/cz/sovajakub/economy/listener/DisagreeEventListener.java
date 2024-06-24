package cz.sovajakub.economy.listener;

import cz.sovajakub.economy.event.DisagreeEvent;
import cz.sovajakub.economy.localization.LocalizationMessage;
import cz.sovajakub.economy.manager.ConfigManager;
import cz.sovajakub.economy.manager.RequestManager;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

import java.text.MessageFormat;

public class DisagreeEventListener implements Listener {
    @EventHandler
    public void onClick(@NotNull DisagreeEvent disagreeEvent) {
        int requestId = disagreeEvent.getRequestId();
        OfflinePlayer creditorOfflinePlayer = RequestManager.getRequest(requestId).getLoanCreditor();
        Player creditorPlayer = Bukkit.getPlayer(creditorOfflinePlayer.getUniqueId());
        OfflinePlayer debtorOfflinePlayer = RequestManager.getRequest(requestId).getLoanDebtor();
        if (creditorPlayer != null)
            creditorPlayer.sendMessage(MessageFormat.format(ConfigManager.getMessage(LocalizationMessage.LOAN_REQUEST_NOTIFY_DISAGREED_CREDITOR), debtorOfflinePlayer.getName()));
        Player debtorPlayer = Bukkit.getPlayer(debtorOfflinePlayer.getUniqueId());
        if (debtorPlayer != null)
            debtorPlayer.sendMessage(MessageFormat.format(ConfigManager.getMessage(LocalizationMessage.LOAN_REQUEST_NOTIFY_DISAGREED_DEBTOR), creditorOfflinePlayer.getName()));
        RequestManager.removeRequest(requestId);
    }
}