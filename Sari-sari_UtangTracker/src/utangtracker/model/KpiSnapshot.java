package utangtracker.model;

public class KpiSnapshot {
    private double totalUnpaid;
    private int customersWithUtang;
    private int transactionsToday;
    private double collectedToday;

    public KpiSnapshot(double totalUnpaid, int customersWithUtang, int transactionsToday, double collectedToday) {
        this.totalUnpaid = totalUnpaid;
        this.customersWithUtang = customersWithUtang;
        this.transactionsToday = transactionsToday;
        this.collectedToday = collectedToday;
    }

    public double getTotalUnpaid() {
        return totalUnpaid;
    }

    public int getCustomersWithUtang() {
        return customersWithUtang;
    }

    public int getTransactionsToday() {
        return transactionsToday;
    }

    public double getCollectedToday() {
        return collectedToday;
    }
}
