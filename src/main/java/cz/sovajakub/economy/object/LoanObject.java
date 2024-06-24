package cz.sovajakub.economy.object;

import java.io.InputStream;

public class LoanObject {
    private final int loanId;
    private final InputStream loanCreditor;
    private final InputStream loanDebtor;
    private final double loanAmount;
    private final String dueDate;
    private final double totalAmount;

    public LoanObject(int loanId, InputStream loanCreditor, InputStream loanDebtor, double loanAmount, String dueDate, double loanTotal) {
        this.loanId = loanId;
        this.loanCreditor = loanCreditor;
        this.loanDebtor = loanDebtor;
        this.loanAmount = loanAmount;
        this.dueDate = dueDate;
        this.totalAmount = loanTotal;
    }

    public int getLoanId() {
        return loanId;
    }

    public InputStream getLoanCreditor() {
        return loanCreditor;
    }

    public InputStream getLoanDebtor() {
        return loanDebtor;
    }

    public double getLoanAmount() {
        return loanAmount;
    }

    public String getDueDate() {
        return dueDate;
    }

    public double getTotalAmount() {
        return totalAmount;
    }
}