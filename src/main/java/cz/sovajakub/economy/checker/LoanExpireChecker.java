package cz.sovajakub.economy.checker;

import cz.sovajakub.economy.PlayerLoans;
import cz.sovajakub.economy.localization.LocalizationMessage;
import cz.sovajakub.economy.manager.ConfigManager;
import cz.sovajakub.economy.manager.DatabaseManager;
import cz.sovajakub.economy.object.LoanObject;
import cz.sovajakub.economy.util.ConversionUtil;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitScheduler;
import org.jetbrains.annotations.NotNull;

import java.text.MessageFormat;
import java.util.UUID;
import java.util.logging.Logger;

public class LoanExpireChecker {
    public PlayerLoans playerLoans;
    private final BukkitScheduler bukkitScheduler;
    private final Logger pluginLogger;
    private final Economy serverEconomy;

    public LoanExpireChecker(@NotNull PlayerLoans playerLoans, @NotNull BukkitScheduler bukkitScheduler, Logger pluginLogger, Economy serverEconomy) {
        this.playerLoans = playerLoans;
        this.bukkitScheduler = bukkitScheduler;
        this.pluginLogger = pluginLogger;
        this.serverEconomy = serverEconomy;
    }

    public void runChecker() {
        bukkitScheduler.runTaskTimer(playerLoans, () -> DatabaseManager.cleanExpiredLoans().acceptSync(loanList -> {
            if (loanList == null || loanList.isEmpty()) return;
            for (LoanObject loanObject : loanList) {
                UUID debtorUuid = ConversionUtil.convertBinaryStream(loanObject.getLoanDebtor());
                UUID creditorUuid = ConversionUtil.convertBinaryStream(loanObject.getLoanCreditor());
                if (debtorUuid == null || creditorUuid == null) return;
                EconomyResponse depositResponse = serverEconomy.depositPlayer(Bukkit.getOfflinePlayer(creditorUuid), loanObject.getLoanAmount()); //TODO over maximum
                OfflinePlayer debtorOfflinePlayer = Bukkit.getOfflinePlayer(debtorUuid);
                if (depositResponse.transactionSuccess()) {
                    Player creditorPlayer = Bukkit.getPlayer(creditorUuid);
                    if (creditorPlayer != null) {
                        creditorPlayer.sendMessage(MessageFormat.format(ConfigManager.getMessage(LocalizationMessage.LOAN_EXPIRED_NOTIFY_CREDITOR), debtorOfflinePlayer.getName()));
                    }
                }
                EconomyResponse withdrawResponse = serverEconomy.withdrawPlayer(debtorOfflinePlayer, loanObject.getTotalAmount());
                Player debtorPlayer = Bukkit.getPlayer(debtorUuid);
                if (!withdrawResponse.transactionSuccess()) {
                    serverEconomy.withdrawPlayer(debtorOfflinePlayer, serverEconomy.getBalance(debtorOfflinePlayer));
                }
                if (debtorPlayer != null) {
                    debtorPlayer.sendMessage(ConfigManager.getMessage(LocalizationMessage.LOAN_EXPIRED_NOTIFY_DEBTOR));
                }
                int loanId = loanObject.getLoanId();
                DatabaseManager.deleteLoan(loanId).acceptSync(isDeleted -> {
                    if (!isDeleted) pluginLogger.severe("We couldn't delete the loan with ID " + loanId);
                });
            }
        }), 600L, 600L);
    }
}