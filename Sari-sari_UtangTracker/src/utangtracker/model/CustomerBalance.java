package utangtracker.model;

public class CustomerBalance {
    private User customer;
    private double balance;
    private String lastActivity;

    public CustomerBalance(User customer, double balance, String lastActivity) {
        this.customer = customer;
        this.balance = balance;
        this.lastActivity = lastActivity;
    }

    public User getCustomer() {
        return customer;
    }

    public double getBalance() {
        return balance;
    }

    public String getLastActivity() {
        return lastActivity;
    }
}
