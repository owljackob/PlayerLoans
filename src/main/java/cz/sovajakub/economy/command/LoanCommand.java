package cz.sovajakub.economy.command;

import cz.sovajakub.economy.PlayerLoans;
import cz.sovajakub.economy.event.AgreeEvent;
import cz.sovajakub.economy.event.DisagreeEvent;
import cz.sovajakub.economy.localization.LocalizationMessage;
import cz.sovajakub.economy.manager.ConfigManager;
import cz.sovajakub.economy.manager.DatabaseManager;
import cz.sovajakub.economy.manager.RequestManager;
import cz.sovajakub.economy.object.LoanObject;
import cz.sovajakub.economy.util.ConversionUtil;
import cz.sovajakub.economy.util.LoanUtil;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.*;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

public class LoanCommand {
    public static void register() {

        new CommandAPICommand("loan")
                .withPermission("playerloans.*")
                .executesPlayer((commandSender, commandArguments) -> {
                    commandSender.sendMessage("§f- §bPlayerLoans §f-");
                    commandSender.sendMessage("§b/loan - §fShows help");
                    commandSender.sendMessage("§b/loan ask §f<player> <amount> <interest> <length> §b- §fAsks for a loan");
                    commandSender.sendMessage("§b/loan list §b- §fLists your loans");
                    commandSender.sendMessage("§b/loan pay §f<id> §b- §fPays off a loan");
                })
                .register();

        new CommandAPICommand("loan")
                .withArguments(
                        new MultiLiteralArgument("list")
                                .setListed(false)
                )
                .withPermission("playerloans.*")
                .executesPlayer((commandSender, commandArguments) -> {
                    final UUID commandSenderUUID = commandSender.getUniqueId();
                    DatabaseManager.getLoanList(ConversionUtil.convertUniqueId(commandSenderUUID)).acceptSync(loanList -> {
                        if (Bukkit.getPlayer(commandSenderUUID) == null) {
                            return;
                        }
                        if (loanList == null) {
                            commandSender.sendMessage(ConfigManager.getMessage(LocalizationMessage.LOAN_ERROR_NOTIFY_NULL_LIST));
                            return;
                        }
                        if (loanList.isEmpty()) {
                            commandSender.sendMessage(ConfigManager.getMessage(LocalizationMessage.LOAN_LIST_NOTIFY_NONE));
                            return;
                        }
                        final TextComponent loanComponent = new TextComponent(ConfigManager.getMessage(LocalizationMessage.LOAN_LIST_COMPONENT_HEADER));
                        for (LoanObject loanObject : loanList) {
                            final int loanId = loanObject.getLoanId();
                            loanComponent.addExtra(MessageFormat.format(ConfigManager.getMessage(LocalizationMessage.LOAN_LIST_COMPONENT_ROW), loanId, loanObject.getTotalAmount(), loanObject.getDueDate()));
                            final TextComponent PAY_COMPONENT = new TextComponent(ConfigManager.getMessage(LocalizationMessage.LOAN_LIST_COMPONENT_PAY));
                            PAY_COMPONENT.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/loan pay" + " " + loanId));
                            loanComponent.addExtra(PAY_COMPONENT);
                        }
                        loanComponent.addExtra(new TextComponent(ConfigManager.getMessage(LocalizationMessage.LOAN_LIST_COMPONENT_FOOTER)));
                        commandSender.spigot().sendMessage(loanComponent);
                    });
                })
                .register();

        new CommandAPICommand("loan")
                .withArguments(
                        new MultiLiteralArgument("pay")
                                .setListed(false)
                )
                .withPermission("playerloans.*")
                .withArguments(new IntegerArgument("loanId", 1, 2147483647))
                .executesPlayer((debtorPlayer, commandArguments) -> {
                    final int LOAN_ID = (int) commandArguments[0];
                    final Economy serverEconomy = PlayerLoans.getEconomy();
                    DatabaseManager.payLoan(LOAN_ID).acceptSync(loanObject -> {
                        final UUID SENDER_UUID = debtorPlayer.getUniqueId();
                        Player loanDebtor = Bukkit.getPlayer(SENDER_UUID);
                        if (loanObject == null) {
                            if (loanDebtor != null) {
                                debtorPlayer.sendMessage(ConfigManager.getMessage(LocalizationMessage.LOAN_PAY_INVALID_LOAN));
                            }
                            return;
                        }
                        if (!SENDER_UUID.equals(ConversionUtil.convertBinaryStream(loanObject.getLoanDebtor()))) {
                            debtorPlayer.sendMessage(ConfigManager.getMessage(LocalizationMessage.LOAN_PAY_INVALID_DEBTOR));
                            return;
                        }
                        final double LOAN_TOTAL = loanObject.getTotalAmount();
                        EconomyResponse withdrawResponse = serverEconomy.withdrawPlayer(debtorPlayer, LOAN_TOTAL);
                        if (!withdrawResponse.transactionSuccess()) {
                            debtorPlayer.sendMessage(ConfigManager.getMessage(LocalizationMessage.LOAN_PAY_NOTIFY_FAILURE_DEBTOR));
                            return;
                        }
                        DatabaseManager.deleteLoan(LOAN_ID).acceptSync(isDeleted -> {
                            if (!isDeleted) {
                                if (loanDebtor != null) {
                                    debtorPlayer.sendMessage(ConfigManager.getMessage(LocalizationMessage.LOAN_ERROR_NOTIFY_DELETE_LOAN));
                                }
                            }
                        });
                        UUID loanCreditorOfflineUuid = ConversionUtil.convertBinaryStream(loanObject.getLoanCreditor());
                        if (loanCreditorOfflineUuid == null) {
                            return;
                        }
                        final OfflinePlayer loanCreditorOffline = Bukkit.getOfflinePlayer(loanCreditorOfflineUuid);
                        Player loanCreditor = Bukkit.getPlayer(loanCreditorOfflineUuid);
                        EconomyResponse depositResponse = serverEconomy.depositPlayer(loanCreditorOffline, LOAN_TOTAL);
                        if (!depositResponse.transactionSuccess()) {
                            if (loanCreditor != null) {
                                loanCreditor.sendMessage(ConfigManager.getMessage(LocalizationMessage.LOAN_PAY_NOTIFY_FAILURE_CREDITOR));
                            }
                        }
                        if (Bukkit.getPlayer(SENDER_UUID) != null) {
                            debtorPlayer.sendMessage(ConfigManager.getMessage(LocalizationMessage.LOAN_PAY_NOTIFY_SUCCESS_DEBTOR));
                        }
                        if (loanCreditor != null) {
                            loanCreditor.sendMessage(MessageFormat.format(ConfigManager.getMessage(LocalizationMessage.LOAN_PAY_NOTIFY_SUCCESS_CREDITOR), debtorPlayer.getName()));
                        }
                    });
                })
                .register();

        new CommandAPICommand("loan")
                .withArguments(
                        new MultiLiteralArgument("ask")
                                .setListed(false)
                )
                .withPermission("playerloans.*")
                .withArguments(new PlayerArgument("creditorPlayer").replaceSuggestions(ArgumentSuggestions.strings(info -> {
                    List<Player> onlinePlayers = new ArrayList<>(Bukkit.getOnlinePlayers());
                    onlinePlayers.remove((Player) info.sender());
                    return onlinePlayers.stream()
                            .map(Entity::getName)
                            .toArray(String[]::new);
                })))
                .withArguments(new DoubleArgument("loanAmount", 0.1D, 1000000.0D))
                .withArguments(new FloatArgument("loanInterestRate", 0.1F, 100.0F))
                .withArguments(new IntegerArgument("loanLength", 1, 365))
                .executesPlayer((debtorPlayer, commandArguments) -> {
                    Player creditorPlayer = (Player) commandArguments[0];
                    double loanAmount = (double) commandArguments[1];
                    float loanInterestRate = (float) commandArguments[2];
                    int loanLength = (int) commandArguments[3];
                    if (debtorPlayer.equals(creditorPlayer)) {
                        debtorPlayer.sendMessage(ConfigManager.getMessage(LocalizationMessage.LOAN_ASK_NOTIFY_WRONG_TARGET));
                        return;
                    }
                    Random randomRequestId = new Random();
                    int requestId = randomRequestId.nextInt(Integer.MIN_VALUE, Integer.MAX_VALUE);
                    while (RequestManager.hasRequest(requestId)) {
                        requestId = randomRequestId.nextInt(Integer.MIN_VALUE, Integer.MAX_VALUE);
                    }
                    double loanTotal = LoanUtil.calculateTotalAmount(loanAmount, loanInterestRate, loanLength);
                    RequestManager.addRequest(requestId, creditorPlayer, debtorPlayer, loanAmount, loanInterestRate, loanLength, loanTotal);
                    TextComponent textComponent = new TextComponent(MessageFormat.format(ConfigManager.getMessage(LocalizationMessage.LOAN_ASK_COMPONENT_OPINION), debtorPlayer.getName()));
                    textComponent.addExtra(new TextComponent(MessageFormat.format(ConfigManager.getMessage(LocalizationMessage.LOAN_ASK_COMPONENT_DEAL), loanTotal, loanAmount, loanLength, loanInterestRate)));
                    TextComponent agreeComponent = new TextComponent(ConfigManager.getMessage(LocalizationMessage.LOAN_ASK_COMPONENT_AGREE));
                    agreeComponent.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/loan agree" + " " + requestId));
                    textComponent.addExtra(agreeComponent);
                    TextComponent declineComponent = new TextComponent(ConfigManager.getMessage(LocalizationMessage.LOAN_ASK_COMPONENT_DISAGREE));
                    declineComponent.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/loan disagree" + " " + requestId));
                    textComponent.addExtra(declineComponent);
                    creditorPlayer.spigot().sendMessage(textComponent);
                    debtorPlayer.sendMessage(MessageFormat.format(ConfigManager.getMessage(LocalizationMessage.LOAN_REQUEST_NOTIFY_SENT), creditorPlayer.getName()));
                    RequestManager.removeRequestLater(requestId, creditorPlayer, debtorPlayer);
                })
                .register();

        new CommandAPICommand("loan")
                .withArguments(
                        new MultiLiteralArgument("agree")
                                .setListed(false)
                )
                .withPermission("playerloans.*")
                .withArguments(new IntegerArgument("requestId", -2147483648, 2147483647))
                .executesPlayer((commandSender, commandArguments) -> {
                    int requestId = (int) commandArguments[0];
                    if (!RequestManager.hasRequest(requestId)) {
                        commandSender.sendMessage(ConfigManager.getMessage(LocalizationMessage.LOAN_REQUEST_NOTIFY_INACTIVE));
                        return;
                    }
                    if (!PlayerLoans.getEconomy().has(RequestManager.getRequest(requestId).getLoanCreditor(), RequestManager.getRequest(requestId).getLoanAmount())) {
                        commandSender.sendMessage(ConfigManager.getMessage(LocalizationMessage.LOAN_REQUEST_NOTIFY_NO_MONEY));
                        return;
                    }
                    Bukkit.getPluginManager().callEvent(new AgreeEvent(requestId));
                })
                .register();

        new CommandAPICommand("loan")
                .withArguments(
                        new MultiLiteralArgument("disagree").replaceSuggestions(ArgumentSuggestions.strings("null"))
                                .setListed(false)
                )
                .withPermission("playerloans.*")
                .withArguments(new IntegerArgument("requestId", -2147483648, 2147483647))
                .executesPlayer((commandSender, commandArguments) -> {
                    int requestId = (int) commandArguments[0];
                    if (!RequestManager.hasRequest(requestId)) {
                        commandSender.sendMessage(ConfigManager.getMessage(LocalizationMessage.LOAN_REQUEST_NOTIFY_INACTIVE));
                        return;
                    }
                    Bukkit.getPluginManager().callEvent(new DisagreeEvent(requestId));
                })
                .register();
    }
}