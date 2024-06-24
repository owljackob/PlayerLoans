package cz.sovajakub.economy.manager;

import com.zaxxer.hikari.HikariDataSource;
import cz.sovajakub.economy.object.LoanObject;
import org.jetbrains.annotations.NotNull;
import us.figt.mesh.Mesh;

import java.io.InputStream;
import java.sql.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class DatabaseManager {

    private static String HOST;
    private static String PORT;
    private static String NAME;
    private static String USER;
    private static String PASSWORD;
    private static HikariDataSource hikariCp;

    public static void initManager() {
        HOST = (String) ConfigManager.getObject("DATABASE_HOST");
        PORT = (String) ConfigManager.getObject("DATABASE_PORT");
        NAME = (String) ConfigManager.getObject("DATABASE_NAME");
        USER = (String) ConfigManager.getObject("DATABASE_USER");
        PASSWORD = (String) ConfigManager.getObject("DATABASE_PASSWORD");
    }

    public static void connectToDataSource() {
        hikariCp = new HikariDataSource();
        hikariCp.setDataSourceClassName("com.mysql.cj.jdbc.MysqlDataSource");
        hikariCp.addDataSourceProperty("serverName", HOST);
        hikariCp.addDataSourceProperty("port", PORT);
        hikariCp.addDataSourceProperty("databaseName", NAME);
        hikariCp.addDataSourceProperty("user", USER);
        hikariCp.addDataSourceProperty("password", PASSWORD);
    }

    private static boolean isConnected() {
        return hikariCp != null;
    }

    public static void disconnectFromDataSource() {
        if (hikariCp == null) {
            return;
        }
        if (isConnected()) {
            hikariCp.close();
        }
    }

    public static @NotNull Mesh<Boolean> createTable() {
        Mesh<Boolean> mesh = Mesh.createMesh();
        mesh.supplyAsync(() -> {
            try (Connection databaseConnection = hikariCp.getConnection(); Statement statement = databaseConnection.createStatement()) {
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS playerloans (loan_id INT AUTO_INCREMENT, loan_creditor BINARY(16), loan_debtor BINARY(16), loan_amount DOUBLE, loan_interest_rate FLOAT, loan_due_date DATE, loan_total_amount DOUBLE, PRIMARY KEY(loan_id))");
                return true;
            } catch (SQLException e) {
                return false;
            }
        });
        return mesh;
    }

    public static @NotNull Mesh<Boolean> insertLoan(InputStream creditorPlayer, InputStream debtorPlayer, double loanAmount, float interestRate, String loanDueDate, double totalAmount) {
        Mesh<Boolean> mesh = Mesh.createMesh();
        mesh.supplyAsync(() -> {
            try (Connection databaseConnection = hikariCp.getConnection(); PreparedStatement preparedStatement = databaseConnection.prepareStatement("INSERT INTO playerloans (loan_creditor, loan_debtor, loan_amount, loan_interest_rate, loan_due_date, loan_total_amount) VALUES (?, ?, ?, ?, ?, ?)")) {
                preparedStatement.setBinaryStream(1, creditorPlayer);
                preparedStatement.setBinaryStream(2, debtorPlayer);
                preparedStatement.setDouble(3, loanAmount);
                preparedStatement.setFloat(4, interestRate);
                preparedStatement.setString(5, loanDueDate);
                preparedStatement.setDouble(6, totalAmount);
                preparedStatement.executeUpdate();
                return true;
            } catch (SQLException sqlException) {
                return false;
            }
        });
        return mesh;
    }

    public static @NotNull Mesh<Boolean> deleteLoan(int loanId) {
        Mesh<Boolean> mesh = Mesh.createMesh();
        mesh.supplyAsync(() -> {
            try (Connection databaseConnection = hikariCp.getConnection();
                 PreparedStatement preparedStatement = databaseConnection.prepareStatement("DELETE FROM playerloans WHERE loan_id = ?")) {
                preparedStatement.setInt(1, loanId);
                preparedStatement.executeUpdate();
                return true;
            } catch (SQLException sqlException) {
                return false;
            }
        });
        return mesh;
    }

    public static @NotNull Mesh<List<LoanObject>> cleanExpiredLoans() {
        Mesh<List<LoanObject>> mesh = Mesh.createMesh();
        mesh.supplyAsync(() -> {
            try (Connection databaseConnection = hikariCp.getConnection();
                 PreparedStatement preparedStatement = databaseConnection.prepareStatement("SELECT loan_id, loan_creditor, loan_debtor, loan_amount, loan_total_amount FROM playerloans WHERE loan_due_date < CURDATE()")) {
                ResultSet expiredLoan = preparedStatement.executeQuery();
                List<LoanObject> loanList = new ArrayList<>();
                while (expiredLoan.next()) {
                    loanList.add(new LoanObject(expiredLoan.getInt("loan_id"), expiredLoan.getBinaryStream("loan_creditor"), expiredLoan.getBinaryStream("loan_debtor"), expiredLoan.getDouble("loan_amount"), null, expiredLoan.getDouble("loan_total_amount")));
                }
                return loanList;
            } catch (SQLException sqlException) {
                return null;
            }
        });
        return mesh;
    }

    public static @NotNull Mesh<List<LoanObject>> getLoanList(@NotNull InputStream commandSender) {
        Mesh<List<LoanObject>> loanListMesh = Mesh.createMesh();
        loanListMesh.supplyAsync(() -> {
            try (Connection databaseConnection = hikariCp.getConnection();
                 PreparedStatement preparedStatement = databaseConnection.prepareStatement("SELECT loan_id, loan_due_date, loan_total_amount FROM playerloans WHERE loan_debtor = ?")) {
                preparedStatement.setBinaryStream(1, commandSender);
                List<LoanObject> loanList = new ArrayList<>();
                ResultSet ownLoan = preparedStatement.executeQuery();
                while (ownLoan.next()) {
                    loanList.add(new LoanObject(ownLoan.getInt("loan_id"), null, null, 0, ownLoan.getString("loan_due_date"), ownLoan.getDouble("loan_total_amount")));
                }
                if (loanList.isEmpty()) {
                    return loanList;
                }
                loanList.sort(Comparator.comparing(LoanObject::getDueDate, Comparator.reverseOrder()));
                return loanList;
            } catch (SQLException sqlException) {
                return null;
            }
        });
        return loanListMesh;
    }

    public static @NotNull Mesh<LoanObject> payLoan(int loanId) {
        Mesh<LoanObject> mesh = Mesh.createMesh();
        mesh.supplyAsync(() -> {
            try (Connection databaseConnection = hikariCp.getConnection();
                 PreparedStatement selectStatement = databaseConnection.prepareStatement("SELECT loan_creditor, loan_debtor, loan_total_amount FROM playerloans WHERE loan_id = ?")) {
                selectStatement.setInt(1, loanId);
                LoanObject loanObject = null;
                ResultSet ownLoan = selectStatement.executeQuery();
                if (ownLoan.next()) {
                    loanObject = new LoanObject(0, ownLoan.getBinaryStream("loan_creditor"), ownLoan.getBinaryStream("loan_debtor"), 0, null, ownLoan.getDouble("loan_total_amount"));
                }
                return loanObject;
            } catch (SQLException sqlException) {
                return null;
            }
        });
        return mesh;
    }
}