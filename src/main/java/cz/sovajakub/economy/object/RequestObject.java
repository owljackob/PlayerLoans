package cz.sovajakub.economy.object;

import org.bukkit.OfflinePlayer;

public class RequestObject {
    private final OfflinePlayer loanCreditor;
    private final OfflinePlayer loanDebtor;
    private final double loanAmount;
    private final float loanInterestRate;
    private final int loanLength;
    private final double totalAmount;

    public RequestObject(OfflinePlayer loanCreditor, OfflinePlayer loanDebtor, double loanAmount, float loanInterestRate, int loanLength, double loanTotal) {
        this.loanCreditor = loanCreditor;
        this.loanDebtor = loanDebtor;
        this.loanAmount = loanAmount;
        this.loanInterestRate = loanInterestRate;
        this.loanLength = loanLength;
        this.totalAmount = loanTotal;
    }

    public OfflinePlayer getLoanCreditor() {
        return loanCreditor;
    }

    public OfflinePlayer getLoanDebtor() {
        return loanDebtor;
    }

    public double getLoanAmount() {
        return loanAmount;
    }

    public float getLoanInterestRate() {
        return loanInterestRate;
    }

    public int getLoanLength() {
        return loanLength;
    }

    public double getTotalAmount() {
        return totalAmount;
    }
}
