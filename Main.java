import java.io.*;
import java.util.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

class User {
    private String userID;
    private String hashedPin;

    public User(String userID, String pin) {
        this.userID = userID;
        this.hashedPin = hashPin(pin);
    }

    public String getUserID() {
        return userID;
    }

    public String getHashedPin() {
        return hashedPin;
    }

    private String hashPin(String pin) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(pin.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                hexString.append(String.format("%02x", b));
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error hashing PIN", e);
        }
    }
}

class Transaction {
    private String description;
    private double amount;
    private Date timestamp;

    public Transaction(String description, double amount) {
        this.description = description;
        this.amount = amount;
        this.timestamp = new Date();
    }

    public String getDescription() {
        return description;
    }

    public double getAmount() {
        return amount;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return description + " | Amount: $" + amount + " | Timestamp: " + timestamp;
    }
}

class Account {
    private String accountNumber;
    private double balance;
    private List<Transaction> transactions;

    public Account(String accountNumber) {
        this.accountNumber = accountNumber;
        this.balance = 0.0;
        this.transactions = new ArrayList<>();
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public double getBalance() {
        return balance;
    }

    public List<Transaction> getTransactions() {
        return transactions;
    }

    public void deposit(double amount) {
        balance += amount;
        transactions.add(new Transaction("Deposit", amount));
        saveTransactionToFile("Deposit", amount);
    }

    public void withdraw(double amount) {
        if (balance >= amount) {
            balance -= amount;
            transactions.add(new Transaction("Withdrawal", -amount));
            saveTransactionToFile("Withdrawal", -amount);
        } else {
            System.out.println("Insufficient balance.");
        }
    }

    public void transfer(Account recipient, double amount) {
        if (balance >= amount) {
            balance -= amount;
            transactions.add(new Transaction("Transfer to " + recipient.getAccountNumber(), -amount));
            recipient.deposit(amount);
            saveTransactionToFile("Transfer to " + recipient.getAccountNumber(), -amount);
        } else {
            System.out.println("Insufficient balance.");
        }
    }

    private void saveTransactionToFile(String type, double amount) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(accountNumber + "_transactions.txt", true))) {
            writer.write(type + " | Amount: $" + amount + " | Timestamp: " + new Date());
            writer.newLine();
        } catch (IOException e) {
            System.out.println("Error saving transaction to file.");
        }
    }
}

class ATMOperations {
    private Map<String, Account> accounts;
    private Map<String, User> users;
    private User currentUser;
    private Scanner scanner;

    public ATMOperations() {
        accounts = new HashMap<>();
        users = new HashMap<>();
        scanner = new Scanner(System.in);
        loadAccountsFromFile();
    }

    private void loadAccountsFromFile() {
        try (BufferedReader reader = new BufferedReader(new FileReader("accounts.txt"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length == 2) {
                    String userID = parts[0];
                    String hashedPin = parts[1];
                    User user = new User(userID, "");
                    users.put(userID, user);
                    accounts.put(userID, new Account(userID));
                }
            }
            System.out.println("Accounts loaded successfully.");
        } catch (IOException e) {
            System.out.println("No existing accounts file found. Starting with empty accounts.");
        }
    }

    private void saveAccountToFile(String userID, String pin) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("accounts.txt", true))) {
            writer.write(userID + "," + hashPin(pin));
            writer.newLine();
        } catch (IOException e) {
            System.out.println("Error saving account to file.");
        }
    }

    private String hashPin(String pin) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(pin.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                hexString.append(String.format("%02x", b));
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error hashing PIN", e);
        }
    }

    public void addUser(String userID, String pin) {
        if (users.containsKey(userID)) {
            System.out.println("User already exists.");
            return;
        }
        
        User user = new User(userID, pin);
        users.put(userID, user);
        
        Account account = new Account(userID);
        accounts.put(userID, account);

        saveAccountToFile(userID, pin);
        
        System.out.println("User added successfully.");
    }

    public void authenticateUser() {
        System.out.print("Enter user ID: ");
        String userID = scanner.nextLine();
        
        System.out.print("Enter PIN: ");
        String pin = scanner.nextLine();

        User user = users.get(userID);

        if (user != null && user.getHashedPin().equals(hashPin(pin))) {
            currentUser = user;
            mainMenu();
        } else {
            System.out.println("Authentication failed. Invalid user ID or PIN.");
        }
    }

    public void mainMenu() {
        while (true) {
            clearScreen();
            
            System.out.println("===================================");
            System.out.println("          ATM INTERFACE");
            System.out.println("===================================");
            
            System.out.println("Welcome, " + currentUser.getUserID() + "!");
            
            System.out.println("1. Transactions History");
            System.out.println("2. Withdraw");
            System.out.println("3. Deposit");
            System.out.println("4. Transfer");
            System.out.println("5. Quit");
            
            System.out.print("Enter your choice: ");
            int choice = scanner.nextInt();
            scanner.nextLine();

            switch (choice) {
                case 1:
                    displayTransactionsHistory();
                    pressEnterToContinue();
                    break;
                case 2:
                    performWithdrawal();
                    pressEnterToContinue();
                    break;
                case 3:
                    performDeposit();
                    pressEnterToContinue();
                    break;
                case 4:
                    performTransfer();
                    pressEnterToContinue();
                    break;
                case 5:
                    System.out.println("Thank you for using ATM Interface. Goodbye!");
                    return;
                default:
                    System.out.println("Invalid choice. Please select a valid option.");
            }
        }
    }

    public void displayTransactionsHistory() {
        Account account = accounts.get(currentUser.getUserID());
        List<Transaction> transactions = account.getTransactions();

        clearScreen();
        System.out.println("===================================");
        System.out.println("      Transactions History");
        System.out.println("===================================");
        System.out.println("Account Number: " + account.getAccountNumber());
        System.out.println("Balance: $" + account.getBalance());
        System.out.println("Transactions:");

        for (Transaction transaction : transactions) {
            System.out.println(transaction.toString());
        }
    }

    public void performWithdrawal() {
        Account account = accounts.get(currentUser.getUserID());
        System.out.print("Enter amount to withdraw: $");
        double amount = scanner.nextDouble();
        scanner.nextLine(); // consume newline
        account.withdraw(amount);
        System.out.println("Withdrawal completed.");
    }

    public void performDeposit() {
        Account account = accounts.get(currentUser.getUserID());
        System.out.print("Enter amount to deposit: $");
        double amount = scanner.nextDouble();
        scanner.nextLine(); // consume newline
        account.deposit(amount);
        System.out.println("Deposit completed.");
    }

    public void performTransfer() {
        Account account = accounts.get(currentUser.getUserID());
        System.out.print("Enter recipient's account number: ");
        String recipientAccountNumber = scanner.nextLine();
        Account recipient = accounts.get(recipientAccountNumber);

        if (recipient != null) {
            System.out.print("Enter amount to transfer: $");
            double amount = scanner.nextDouble();
            scanner.nextLine(); // consume newline
            account.transfer(recipient, amount);
            System.out.println("Transfer completed.");
        } else {
            System.out.println("Recipient account not found.");
        }
    }

    public boolean isDuplicateUser(String userID) {
        return users.containsKey(userID);
    }

    public void clearScreen() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }

    public void pressEnterToContinue() {
        System.out.print("\nPress Enter to continue...");
        scanner.nextLine();
    }
}

public class Main {
    public static void main(String[] args) {
        ATMOperations atm = new ATMOperations();
        Scanner scanner = new Scanner(System.in);

        while (true) {
            atm.clearScreen();
            System.out.println("===================================");
            System.out.println("          ATM INTERFACE");
            System.out.println("===================================");
            System.out.println("1. New User");
            System.out.println("2. Existing User");
            System.out.println("3. Quit");
            System.out.print("Enter your choice: ");
            int choice = scanner.nextInt();
            scanner.nextLine();

            switch (choice) {
                case 1:
                    System.out.print("Enter new user ID: ");
                    String newUserID = scanner.nextLine();

                    if (atm.isDuplicateUser(newUserID)) {
                        System.out.println("User already exists. Please choose a different user ID.");
                        atm.pressEnterToContinue();
                        break;
                    }

                    System.out.print("Enter new PIN: ");
                    String newPIN = scanner.nextLine();
                    atm.addUser(newUserID, newPIN);
                    atm.pressEnterToContinue();
                    break;
                case 2:
                    atm.authenticateUser();
                    break;
                case 3:
                    System.out.println("Thank you for using ATM Interface. Goodbye!");
                    return;
                default:
                    System.out.println("Invalid choice. Please select a valid option.");
            }
        }
    }
}
