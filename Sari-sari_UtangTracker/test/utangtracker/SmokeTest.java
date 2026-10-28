package utangtracker;

import utangtracker.db.Database;
import utangtracker.model.Role;
import utangtracker.service.AppException;
import utangtracker.service.AuthService;
import utangtracker.service.UtangService;

/**
 * Headless check used while building the project. Not required to run the app.
 * Tip: run with -Dutang.home=/some/temp/folder to keep your real database untouched.
 */
public class SmokeTest {
    public static void main(String[] args) throws Exception {
        Database.get();
        AuthService auth = new AuthService();
        UtangService utang = new UtangService();

        // --- ledger basics ---
        Session.set(auth.login("maria", "maria123"));
        check(Session.get().getRole() == Role.CUSTOMER, "maria should be a customer");
        double before = utang.myBalance();
        check(before >= 45, "Expected Maria balance around 45, got " + before);

        // --- customers cannot do owner actions ---
        expectDenied("customer approve", () -> auth.approveCustomer(1));
        expectDenied("customer create staff", () -> auth.createStaff("Bad", "Actor", "bad.actor", "Passw0rd!", ""));
        expectDenied("customer pending list", () -> auth.pendingCustomers());

        // --- staff can record utang but not owner actions ---
        Session.set(auth.login("staff", "staff123"));
        int mariaId = utang.searchCustomers("maria").get(0).getCustomer().getId();
        utang.addUtang(mariaId, "smoke item", 1, 1.00, "smoke note");
        expectDenied("staff approve", () -> auth.approveCustomer(mariaId));
        expectDenied("staff deactivate staff", () -> auth.setStaffActive(mariaId, false));

        // --- owner ---
        Session.set(auth.login("owner", "owner123"));
        check(utang.kpis().getTotalUnpaid() > 0, "Owner KPI unpaid should be positive");

        // --- registration -> pending -> approval ---
        Session.clear();
        String user = "smoke" + (System.currentTimeMillis() % 100000);
        auth.registerCustomer("Smoke", "Tester", user, "Passw0rd!", "Passw0rd!", "09171234567", "4321");
        try {
            auth.login(user, "Passw0rd!");
            throw new IllegalStateException("pending customer should not log in");
        } catch (AppException ex) {
            check(ex.getMessage().contains("approval"), "pending message should mention approval: " + ex.getMessage());
        }

        // --- PIN reset: wrong PIN, lockout, staff/owner refused ---
        for (int i = 0; i < 4; i++) {
            expectDenied("wrong PIN " + i, () -> auth.resetPassword(user, "0000", "Newpass1!", "Newpass1!"));
        }
        try {
            auth.resetPassword(user, "0000", "Newpass1!", "Newpass1!");
            throw new IllegalStateException("5th wrong PIN should fail");
        } catch (AppException ex) {
            check(ex.getMessage().contains("15 minutes"), "5th wrong PIN should lock: " + ex.getMessage());
        }
        expectDenied("locked even with right PIN", () -> auth.resetPassword(user, "4321", "Newpass1!", "Newpass1!"));
        expectDenied("owner PIN reset", () -> auth.resetPassword("owner", "1234", "Newpass1!", "Newpass1!"));
        expectDenied("staff PIN reset", () -> auth.resetPassword("staff", "1234", "Newpass1!", "Newpass1!"));

        // --- PIN reset success path on a fresh customer ---
        String user2 = "smokeb" + (System.currentTimeMillis() % 100000);
        auth.registerCustomer("Smoke", "Second", user2, "Passw0rd!", "Passw0rd!", "09171234567", "4321");
        auth.resetPassword(user2, "4321", "Newpass1!", "Newpass1!");

        System.out.println("SMOKE OK balance=" + before);
    }

    private interface Action {
        void run() throws Exception;
    }

    private static void expectDenied(String label, Action action) throws Exception {
        try {
            action.run();
        } catch (AppException expected) {
            return;
        }
        throw new IllegalStateException("Expected refusal but it succeeded: " + label);
    }

    private static void check(boolean ok, String message) {
        if (!ok) {
            throw new IllegalStateException(message);
        }
    }
}
