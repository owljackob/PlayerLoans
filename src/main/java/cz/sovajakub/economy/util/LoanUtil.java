package cz.sovajakub.economy.util;

public class LoanUtil {
    public static double calculateTotalAmount(double loanAmount, float interestRate, int loanLength) {
        return loanAmount + ((loanAmount * interestRate * loanLength) / 100);
    }
}