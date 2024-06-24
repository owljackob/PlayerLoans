package cz.sovajakub.economy.manager;

import cz.sovajakub.economy.PlayerLoans;
import cz.sovajakub.economy.localization.LocalizationMessage;
import cz.sovajakub.economy.object.RequestObject;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitScheduler;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;

public class RequestManager {
    private static final HashMap<Integer, RequestObject> REQUEST_LIST = new HashMap<>();
    private static PlayerLoans playerLoans;
    private static BukkitScheduler bukkitScheduler;

    public static void initManager(PlayerLoans playerLoans, BukkitScheduler bukkitScheduler) {
        RequestManager.playerLoans = playerLoans;
        RequestManager.bukkitScheduler = bukkitScheduler;
    }

    public static void addRequest(int requestId,
                                  @NotNull OfflinePlayer creditorPlayer,
                                  @NotNull OfflinePlayer debtorPlayer,
                                  double loanAmount,
                                  float loanInterestRate,
                                  int loanLength,
                                  double loanTotal) {
        REQUEST_LIST.put(requestId, new RequestObject(creditorPlayer, debtorPlayer, loanAmount, loanInterestRate, loanLength, loanTotal));
    }

    public static RequestObject getRequest(int requestId) {
        return REQUEST_LIST.get(requestId);
    }

    public static void removeRequest(int requestId) {
        REQUEST_LIST.remove(requestId);
    }

    public static boolean hasRequest(int requestId) {
        return REQUEST_LIST.containsKey(requestId);
    }

    public static void removeRequestLater(int requestId, Player creditorPlayer, Player debtorPlayer) {
        bukkitScheduler.runTaskLater(playerLoans, () -> {
            if (!hasRequest(requestId)) {
                return;
            }
            removeRequest(requestId);
            String timedOutMessage = ConfigManager.getMessage(LocalizationMessage.LOAN_REQUEST_NOTIFY_TIMED_OUT);
            if (Bukkit.getPlayer(creditorPlayer.getUniqueId()) != null) {
                creditorPlayer.sendMessage(timedOutMessage);
            }
            if (Bukkit.getPlayer(debtorPlayer.getUniqueId()) != null) {
                debtorPlayer.sendMessage(timedOutMessage);
            }
        }, 200L);
    }
}