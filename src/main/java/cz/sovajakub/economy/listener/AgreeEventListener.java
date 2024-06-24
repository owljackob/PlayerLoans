package cz.sovajakub.economy.listener;

import cz.sovajakub.economy.event.AgreeEvent;
import cz.sovajakub.economy.localization.LocalizationMessage;
import cz.sovajakub.economy.manager.ConfigManager;
import cz.sovajakub.economy.manager.DatabaseManager;
import cz.sovajakub.economy.manager.RequestManager;
import cz.sovajakub.economy.util.ConversionUtil;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.UUID;

public class AgreeEventListener implements Listener {
    private final Economy serverEconomy;

    public AgreeEventListener(Economy serverEconomy) {
        this.serverEconomy = serverEconomy;
    }

    @EventHandler
    public void onAgreeEvent(@NotNull AgreeEvent agreeEvent) {
        int requestId = agreeEvent.getRequestId();
        UUID creditorPlayerUuid = RequestManager.getRequest(requestId).getLoanCreditor().getUniqueId();
        UUID debtorPlayerUuid = RequestManager.getRequest(requestId).getLoanDebtor().getUniqueId();
        Calendar calendar = Calendar.getInstance(Locale.getDefault());
        calendar.add(Calendar.DATE, RequestManager.getRequest(requestId).getLoanLength());
        double loanAmount = RequestManager.getRequest(requestId).getLoanAmount();
        OfflinePlayer debtorPlayerOffline = Bukkit.getOfflinePlayer(debtorPlayerUuid);
        serverEconomy.depositPlayer(debtorPlayerOffline, loanAmount); //TODO over maximum
        OfflinePlayer creditorPlayerOffline = Bukkit.getOfflinePlayer(creditorPlayerUuid);
        serverEconomy.withdrawPlayer(creditorPlayerOffline, loanAmount);
        DatabaseManager.insertLoan(ConversionUtil.convertUniqueId(creditorPlayerUuid), ConversionUtil.convertUniqueId(debtorPlayerUuid), loanAmount, RequestManager.getRequest(requestId).getLoanInterestRate(), new SimpleDateFormat("yyyy-MM-dd").format(calendar.getTime()), RequestManager.getRequest(requestId).getTotalAmount());
        Player creditorPlayer = Bukkit.getPlayer(creditorPlayerUuid);
        if (creditorPlayer != null)
            creditorPlayer.sendMessage(MessageFormat.format(ConfigManager.getMessage(LocalizationMessage.LOAN_REQUEST_NOTIFY_AGREED_CREDITOR), debtorPlayerOffline.getName()));
        Player debtorPlayer = Bukkit.getPlayer(debtorPlayerUuid);
        if (debtorPlayer != null)
            debtorPlayer.sendMessage(MessageFormat.format(ConfigManager.getMessage(LocalizationMessage.LOAN_REQUEST_NOTIFY_AGREED_DEBTOR), creditorPlayerOffline.getName()));
        RequestManager.removeRequest(requestId);
    }
}