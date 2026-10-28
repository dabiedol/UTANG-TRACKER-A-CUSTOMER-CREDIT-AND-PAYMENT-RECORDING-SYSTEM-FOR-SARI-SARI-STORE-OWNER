package utangtracker.service;

import utangtracker.Session;
import utangtracker.db.Database;
import utangtracker.model.CustomerBalance;
import utangtracker.model.KpiSnapshot;
import utangtracker.model.LedgerEntry;
import utangtracker.model.Role;
import utangtracker.model.User;

import java.sql.SQLException;
import java.util.List;

public class UtangService {
    private final Database database = Database.get();

    public double myBalance() throws AppException {
        requireCustomer();
        return balanceOf(Session.get().getId());
    }

    public List<LedgerEntry> myHistory() throws AppException {
        requireCustomer();
        try {
            return database.history(Integer.valueOf(Session.get().getId()), null, false);
        } catch (SQLException ex) {
            throw new AppException("Cannot load your history.");
        }
    }

    public double balanceOf(int customerId) throws AppException {
        try {
            return database.balanceOf(customerId);
        } catch (SQLException ex) {
            throw new AppException("Cannot compute the balance.");
        }
    }

    public List<CustomerBalance> searchCustomers(String search) throws AppException {
        requireStaffOrOwner();
        try {
            return database.customersWithBalances(search);
        } catch (SQLException ex) {
            throw new AppException("Cannot search customers.");
        }
    }

    public List<LedgerEntry> historyFor(int customerId) throws AppException {
        requireStaffOrOwner();
        try {
            return database.history(Integer.valueOf(customerId), null, false);
        } catch (SQLException ex) {
            throw new AppException("Cannot load that customer's history.");
        }
    }

    public List<LedgerEntry> allHistory(String search, boolean includeVoided) throws AppException {
        requireOwner();
        try {
            return database.history(null, search, includeVoided);
        } catch (SQLException ex) {
            throw new AppException("Cannot load the ledger.");
        }
    }

    public LedgerEntry addUtang(int customerId, String item, double quantity, double unitPrice, String note) throws AppException {
        requireStaffOrOwner();
        if (item == null || item.trim().length() < 2) {
            throw new AppException("Enter the item that was borrowed.");
        }
        if (quantity <= 0) {
            throw new AppException("Quantity must be greater than zero.");
        }
        if (unitPrice <= 0) {
            throw new AppException("Unit price must be greater than zero.");
        }
        double amount = Database.round(quantity * unitPrice);
        return write(customerId, item.trim(), quantity, unitPrice, amount, "UTANG", note);
    }

    public LedgerEntry acceptPayment(int customerId, double amount, String note) throws AppException {
        requireStaffOrOwner();
        if (amount <= 0) {
            throw new AppException("Payment amount must be greater than zero.");
        }
        double balance = balanceOf(customerId);
        if (amount - balance > 0.009) {
            throw new AppException("Payment is larger than the unpaid balance (" + utangtracker.util.AppTime.peso(balance) + ").");
        }
        return write(customerId, "Cash payment", 1, amount, amount, "PAYMENT", note);
    }

    public void voidRecord(int transactionId) throws AppException {
        requireOwner();
        try {
            database.voidTransaction(transactionId);
            database.audit(Session.get().getId(), Session.get().getFirstName() + " " + Session.get().getLastName(),
                    "DELETE", "TXN-" + transactionId, "Voided a ledger record");
        } catch (SQLException ex) {
            throw new AppException("Cannot void that record.");
        }
    }

    public KpiSnapshot kpis() throws AppException {
        requireOwner();
        try {
            return database.kpis();
        } catch (SQLException ex) {
            throw new AppException("Cannot load the dashboard.");
        }
    }

    public List<String[]> auditLog() throws AppException {
        requireOwner();
        try {
            return database.recentAudit();
        } catch (SQLException ex) {
            throw new AppException("Cannot load the audit log.");
        }
    }

    public double weekCollections() throws AppException {
        requireOwner();
        try {
            return database.collectionsSince(utangtracker.util.AppTime.daysAgo(7, "00:00:00"));
        } catch (SQLException ex) {
            throw new AppException("Cannot load weekly collections.");
        }
    }

    public List<CustomerBalance> highestBalances() throws AppException {
        requireOwner();
        List<CustomerBalance> rows = searchCustomers("");
        java.util.Collections.sort(rows, new java.util.Comparator<CustomerBalance>() {
            public int compare(CustomerBalance a, CustomerBalance b) {
                return Double.compare(b.getBalance(), a.getBalance());
            }
        });
        return rows.size() > 5 ? rows.subList(0, 5) : rows;
    }

    public List<User> staff() throws AppException {
        requireOwner();
        try {
            return database.listByRole(Role.STAFF);
        } catch (SQLException ex) {
            throw new AppException("Cannot load staff accounts.");
        }
    }

    public String reminderText(User customer, double balance) {
        return "Hi " + customer.getFirstName() + " " + customer.getLastName() + ", this is a reminder from the sari-sari store. "
                + "Your utang balance is " + utangtracker.util.AppTime.peso(balance) + ". "
                + "Please settle when you can. Thank you.";
    }

    private LedgerEntry write(int customerId, String item, double quantity, double unitPrice, double amount, String type, String note) throws AppException {
        try {
            User customer = database.findUser(customerId);
            if (customer == null || customer.getRole() != Role.CUSTOMER || !customer.isActive()) {
                throw new AppException("Choose an active customer account.");
            }
            int id = database.addTransaction(customerId, Session.get().getId(), item, quantity, unitPrice, amount, type, note);
            database.audit(Session.get().getId(), Session.get().getFirstName() + " " + Session.get().getLastName(),
                    "CREATE", "CUST-" + customerId, type + " " + utangtracker.util.AppTime.peso(amount) + " " + item);
            List<LedgerEntry> rows = database.history(Integer.valueOf(customerId), null, true);
            for (LedgerEntry row : rows) {
                if (row.getId() == id) {
                    return row;
                }
            }
            throw new AppException("Saved, but the new record could not be reloaded.");
        } catch (SQLException ex) {
            throw new AppException("Cannot save that record.");
        }
    }

    private void requireCustomer() throws AppException {
        if (Session.get() == null || Session.get().getRole() != Role.CUSTOMER) {
            throw new AppException("Only the customer can view this balance.");
        }
    }

    private void requireStaffOrOwner() throws AppException {
        Role role = Session.get() == null ? null : Session.get().getRole();
        if (role != Role.STAFF && role != Role.OWNER) {
            throw new AppException("Only staff or the owner can record utang and payments.");
        }
    }

    private void requireOwner() throws AppException {
        if (Session.get() == null || Session.get().getRole() != Role.OWNER) {
            throw new AppException("Only the owner can do that.");
        }
    }
}
