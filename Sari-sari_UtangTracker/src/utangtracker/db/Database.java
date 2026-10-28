package utangtracker.db;

import utangtracker.model.CustomerBalance;
import utangtracker.model.KpiSnapshot;
import utangtracker.model.LedgerEntry;
import utangtracker.model.Role;
import utangtracker.model.User;
import utangtracker.util.AppTime;
import utangtracker.util.PasswordUtil;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public final class Database {
    private static Database instance;

    private final Connection connection;

    private Database() {
        try {
            Properties props = loadProperties();
            String host = props.getProperty("db.host", "localhost");
            String port = props.getProperty("db.port", "3306");
            String name = props.getProperty("db.name", "igpitopol_utang_v2");
            String user = props.getProperty("db.user", "root");
            String password = props.getProperty("db.password", "");

            Class.forName("com.mysql.cj.jdbc.Driver");
            String url = "jdbc:mysql://" + host + ":" + port + "/" + name
                    + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Manila&characterEncoding=UTF-8&useUnicode=true";
            connection = DriverManager.getConnection(url, user, password);

            createSchema();
            createExtraTables();
            seedIfEmpty();
            backfillExtraTables();
            ensureTriggers();
            migratePins();
            ensureDemoPins();
        } catch (Exception ex) {
            throw new IllegalStateException("Cannot open Utang Tracker database. "
                    + "Make sure XAMPP MySQL is running and utang_tracker.sql has been imported. "
                    + "Check db.properties in the project root.", ex);
        }
    }

    private Properties loadProperties() {
        Properties props = new Properties();
        try (FileInputStream in = new FileInputStream("db.properties")) {
            props.load(in);
        } catch (IOException ignored) {
            // Use XAMPP defaults if file is missing
        }
        return props;
    }

    public static synchronized Database get() {
        if (instance == null) {
            instance = new Database();
        }
        return instance;
    }

    public Connection connection() {
        return connection;
    }

    private void createSchema() throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE IF NOT EXISTS users ("
                    + "id INT NOT NULL AUTO_INCREMENT,"
                    + "first_name VARCHAR(100) NOT NULL,"
                    + "last_name VARCHAR(100) NOT NULL,"
                    + "username VARCHAR(50) NOT NULL,"
                    + "password_hash VARCHAR(255) NOT NULL,"
                    + "salt VARCHAR(255) NOT NULL,"
                    + "contact VARCHAR(20) DEFAULT NULL,"
                    + "role VARCHAR(20) NOT NULL,"
                    + "active TINYINT NOT NULL DEFAULT 1,"
                    + "recovery_pin VARCHAR(255) DEFAULT NULL,"
                    + "pin_attempts INT NOT NULL DEFAULT 0,"
                    + "pin_locked_until DATETIME DEFAULT NULL,"
                    + "created_at DATETIME NOT NULL,"
                    + "PRIMARY KEY (id),"
                    + "UNIQUE KEY username (username)"
                    + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
            statement.execute("CREATE TABLE IF NOT EXISTS audit_log ("
                    + "id INT NOT NULL AUTO_INCREMENT,"
                    + "actor_id INT DEFAULT NULL,"
                    + "actor_name VARCHAR(100) DEFAULT NULL,"
                    + "action VARCHAR(50) NOT NULL,"
                    + "target VARCHAR(50) DEFAULT NULL,"
                    + "detail VARCHAR(255) DEFAULT NULL,"
                    + "created_at DATETIME NOT NULL,"
                    + "PRIMARY KEY (id),"
                    + "KEY idx_actor (actor_id),"
                    + "CONSTRAINT fk_audit_actor FOREIGN KEY (actor_id) REFERENCES users (id) ON DELETE SET NULL"
                    + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
            statement.execute("CREATE TABLE IF NOT EXISTS transactions ("
                    + "id INT NOT NULL AUTO_INCREMENT,"
                    + "customer_id INT NOT NULL,"
                    + "staff_id INT DEFAULT NULL,"
                    + "item_description VARCHAR(255) NOT NULL,"
                    + "quantity DECIMAL(10,2) NOT NULL DEFAULT 1.00,"
                    + "unit_price DECIMAL(10,2) NOT NULL DEFAULT 0.00,"
                    + "amount DECIMAL(10,2) NOT NULL,"
                    + "txn_type VARCHAR(20) NOT NULL,"
                    + "note VARCHAR(255) DEFAULT NULL,"
                    + "created_at DATETIME NOT NULL,"
                    + "voided TINYINT NOT NULL DEFAULT 0,"
                    + "PRIMARY KEY (id),"
                    + "KEY customer_id (customer_id),"
                    + "KEY staff_id (staff_id)"
                    + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
        }
    }

    /** Activity Logs, Staff Management, Pending Approvals and Reports tables (no-op if utang_tracker.sql already made them). */
    private void createExtraTables() throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE IF NOT EXISTS activity_logs ("
                    + "id INT NOT NULL AUTO_INCREMENT,"
                    + "transaction_id INT DEFAULT NULL,"
                    + "customer_id INT NOT NULL,"
                    + "staff_id INT DEFAULT NULL,"
                    + "activity ENUM('UTANG','PAYMENT','VOID') NOT NULL,"
                    + "description VARCHAR(255) DEFAULT NULL,"
                    + "amount DECIMAL(10,2) NOT NULL DEFAULT 0.00,"
                    + "created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,"
                    + "PRIMARY KEY (id),"
                    + "KEY idx_act_customer (customer_id),"
                    + "KEY idx_act_staff (staff_id),"
                    + "KEY idx_act_txn (transaction_id),"
                    + "KEY idx_act_date (created_at),"
                    + "CONSTRAINT fk_act_txn FOREIGN KEY (transaction_id) REFERENCES transactions (id) ON DELETE CASCADE,"
                    + "CONSTRAINT fk_act_customer FOREIGN KEY (customer_id) REFERENCES users (id) ON DELETE CASCADE,"
                    + "CONSTRAINT fk_act_staff FOREIGN KEY (staff_id) REFERENCES users (id) ON DELETE SET NULL"
                    + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
            statement.execute("CREATE TABLE IF NOT EXISTS staff_management ("
                    + "id INT NOT NULL AUTO_INCREMENT,"
                    + "user_id INT NOT NULL,"
                    + "position VARCHAR(50) NOT NULL DEFAULT 'Cashier',"
                    + "hire_date DATE DEFAULT NULL,"
                    + "status ENUM('ACTIVE','INACTIVE') NOT NULL DEFAULT 'ACTIVE',"
                    + "notes VARCHAR(255) DEFAULT NULL,"
                    + "updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,"
                    + "PRIMARY KEY (id),"
                    + "UNIQUE KEY uq_staff_user (user_id),"
                    + "CONSTRAINT fk_staff_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE"
                    + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
            statement.execute("CREATE TABLE IF NOT EXISTS pending_approvals ("
                    + "id INT NOT NULL AUTO_INCREMENT,"
                    + "user_id INT NOT NULL,"
                    + "requested_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,"
                    + "status ENUM('PENDING','APPROVED') NOT NULL DEFAULT 'PENDING',"
                    + "reviewed_at DATETIME DEFAULT NULL,"
                    + "remarks VARCHAR(255) DEFAULT NULL,"
                    + "PRIMARY KEY (id),"
                    + "UNIQUE KEY uq_pending_user (user_id),"
                    + "KEY idx_pending_status (status),"
                    + "CONSTRAINT fk_pending_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE"
                    + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
            statement.execute("CREATE TABLE IF NOT EXISTS reports ("
                    + "id INT NOT NULL AUTO_INCREMENT,"
                    + "report_type ENUM('DAILY','WEEKLY','MONTHLY','CUSTOM') NOT NULL DEFAULT 'CUSTOM',"
                    + "period_start DATE NOT NULL,"
                    + "period_end DATE NOT NULL,"
                    + "total_utang DECIMAL(12,2) NOT NULL DEFAULT 0.00,"
                    + "total_payments DECIMAL(12,2) NOT NULL DEFAULT 0.00,"
                    + "outstanding_balance DECIMAL(12,2) NOT NULL DEFAULT 0.00,"
                    + "txn_count INT NOT NULL DEFAULT 0,"
                    + "generated_by INT DEFAULT NULL,"
                    + "generated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,"
                    + "PRIMARY KEY (id),"
                    + "UNIQUE KEY uq_report_period (report_type, period_start, period_end),"
                    + "KEY idx_report_user (generated_by),"
                    + "CONSTRAINT fk_report_user FOREIGN KEY (generated_by) REFERENCES users (id) ON DELETE SET NULL"
                    + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
        }
    }

    /** Copies rows that existed before the new tables did. Safe to run on every start. */
    private void backfillExtraTables() throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("INSERT INTO staff_management (user_id, position, hire_date, status) "
                    + "SELECT u.id, 'Cashier', DATE(u.created_at), IF(u.active = 1, 'ACTIVE', 'INACTIVE') "
                    + "FROM users u WHERE u.role = 'STAFF' "
                    + "AND NOT EXISTS (SELECT 1 FROM staff_management s WHERE s.user_id = u.id)");
            statement.executeUpdate("INSERT INTO pending_approvals (user_id, requested_at, status) "
                    + "SELECT u.id, u.created_at, 'PENDING' FROM users u "
                    + "WHERE u.role = 'CUSTOMER' AND u.active = 0 "
                    + "AND NOT EXISTS (SELECT 1 FROM pending_approvals p WHERE p.user_id = u.id)");
            statement.executeUpdate("INSERT INTO activity_logs (transaction_id, customer_id, staff_id, activity, description, amount, created_at) "
                    + "SELECT t.id, t.customer_id, t.staff_id, t.txn_type, t.item_description, t.amount, t.created_at "
                    + "FROM transactions t "
                    + "WHERE NOT EXISTS (SELECT 1 FROM activity_logs a WHERE a.transaction_id = t.id AND a.activity <> 'VOID')");
        }
    }

    /** Creates the sync triggers if utang_tracker.sql has not already done so. */
    private void ensureTriggers() throws SQLException {
        String[][] triggers = {
            {"trg_users_after_insert",
                "CREATE TRIGGER trg_users_after_insert AFTER INSERT ON users FOR EACH ROW BEGIN "
                + "IF NEW.role = 'STAFF' THEN "
                + "INSERT INTO staff_management (user_id, position, hire_date, status) "
                + "VALUES (NEW.id, 'Cashier', DATE(NEW.created_at), IF(NEW.active = 1, 'ACTIVE', 'INACTIVE')); END IF; "
                + "IF NEW.role = 'CUSTOMER' AND NEW.active = 0 THEN "
                + "INSERT INTO pending_approvals (user_id, requested_at, status) "
                + "VALUES (NEW.id, NEW.created_at, 'PENDING'); END IF; END"},
            {"trg_users_after_update",
                "CREATE TRIGGER trg_users_after_update AFTER UPDATE ON users FOR EACH ROW BEGIN "
                + "IF NEW.role = 'STAFF' AND NEW.active <> OLD.active THEN "
                + "UPDATE staff_management SET status = IF(NEW.active = 1, 'ACTIVE', 'INACTIVE') WHERE user_id = NEW.id; END IF; "
                + "IF NEW.role = 'CUSTOMER' AND OLD.active = 0 AND NEW.active = 1 THEN "
                + "UPDATE pending_approvals SET status = 'APPROVED', reviewed_at = NOW() "
                + "WHERE user_id = NEW.id AND status = 'PENDING'; END IF; END"},
            {"trg_txn_after_insert",
                "CREATE TRIGGER trg_txn_after_insert AFTER INSERT ON transactions FOR EACH ROW BEGIN "
                + "INSERT INTO activity_logs (transaction_id, customer_id, staff_id, activity, description, amount, created_at) "
                + "VALUES (NEW.id, NEW.customer_id, NEW.staff_id, NEW.txn_type, NEW.item_description, NEW.amount, NEW.created_at); END"},
            {"trg_txn_after_update",
                "CREATE TRIGGER trg_txn_after_update AFTER UPDATE ON transactions FOR EACH ROW BEGIN "
                + "IF OLD.voided = 0 AND NEW.voided = 1 THEN "
                + "INSERT INTO activity_logs (transaction_id, customer_id, staff_id, activity, description, amount, created_at) "
                + "VALUES (NEW.id, NEW.customer_id, NEW.staff_id, 'VOID', CONCAT('Voided: ', NEW.item_description), NEW.amount, NOW()); END IF; END"}
        };
        for (String[] trigger : triggers) {
            if (!triggerExists(trigger[0])) {
                try (Statement statement = connection.createStatement()) {
                    statement.execute(trigger[1]);
                } catch (SQLException ex) {
                    // Not fatal (e.g. no TRIGGER privilege): the app still works, the log tables just won't auto-fill.
                    System.err.println("Could not create trigger " + trigger[0] + ": " + ex.getMessage());
                }
            }
        }
    }

    private boolean triggerExists(String name) throws SQLException {
        String sql = "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TRIGGERS WHERE TRIGGER_SCHEMA = DATABASE() AND TRIGGER_NAME = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, name);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    private void repairOldSchema() throws SQLException {
        // No-op for MySQL. Schema comes from utang_tracker.sql
    }

    private boolean columnExists(String table, String column) throws SQLException {
        String sql = "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS "
                + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, table);
            statement.setString(2, column);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    private void seedIfEmpty() throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet count = statement.executeQuery("SELECT COUNT(*) FROM users")) {
            count.next();
            if (count.getInt(1) > 0) {
                return;
            }
        }
        int ownerId = insertUser("Rosa", "Bautista", "owner", "owner123", "09170000001", Role.OWNER);
        int staffId = insertUser("Ana", "Reyes", "staff", "staff123", "09170000002", Role.STAFF);
        int mariaId = insertUser("Maria", "Santos", "maria", "maria123", "09171234567", Role.CUSTOMER);
        int juanId = insertUser("Juan", "Dela Cruz", "juan", "juan123", "09181234567", Role.CUSTOMER);
        int lizaId = insertUser("Liza", "Garcia", "liza", "liza123", "09201234567", Role.CUSTOMER);
        int pedroId = insertUser("Pedro", "Ramos", "pedro", "pedro123", "09190001111", Role.CUSTOMER);

        insertTxn(mariaId, staffId, "1 pack noodles", 15.00, "UTANG", "", AppTime.daysAgo(2, "14:30:00"));
        insertTxn(mariaId, staffId, "1 canned sardines", 30.00, "UTANG", "", AppTime.daysAgo(1, "09:15:00"));
        insertTxn(juanId, staffId, "1 kg rice", 50.00, "UTANG", "", AppTime.daysAgo(5, "16:05:00"));
        insertTxn(juanId, staffId, "Cash payment", 20.00, "PAYMENT", "Partial payment", AppTime.daysAgo(1, "18:40:00"));
        insertTxn(lizaId, staffId, "3-in-1 coffee", 25.00, "UTANG", "Customer asked to double-check spelling of name", AppTime.daysAgo(0, "08:10:00"));
        insertTxn(pedroId, staffId, "Cooking oil 350ml", 42.00, "UTANG", "", AppTime.daysAgo(6, "11:20:00"));
        insertTxn(pedroId, ownerId, "Cash payment", 42.00, "PAYMENT", "Paid in full", AppTime.daysAgo(3, "17:05:00"));
        // Owner and staff get no recovery PIN: PIN reset is for customer accounts only.
        saveRecoveryPin(mariaId, "1234");
        saveRecoveryPin(juanId, "1234");
        saveRecoveryPin(lizaId, "1234");
        saveRecoveryPin(pedroId, "1234");
        // Demo customers start approved so sample data works
        setActive(mariaId, true);
        setActive(juanId, true);
        setActive(lizaId, true);
        setActive(pedroId, true);
    }

    /** Older databases stored recovery PINs in plain text. Hash any that are left. */
    private void migratePins() throws SQLException {
        List<Integer> ids = new ArrayList<Integer>();
        List<String> pins = new ArrayList<String>();
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("SELECT id, recovery_pin FROM users WHERE recovery_pin IS NOT NULL")) {
            while (rs.next()) {
                if (!PasswordUtil.isHashedPin(rs.getString("recovery_pin"))) {
                    ids.add(rs.getInt("id"));
                    pins.add(rs.getString("recovery_pin"));
                }
            }
        }
        for (int i = 0; i < ids.size(); i++) {
            saveRecoveryPin(ids.get(i), pins.get(i));
        }
    }

    private void ensureDemoPins() throws SQLException {
        // Demo customers only. Owner and staff accounts never get a default PIN.
        String sql = "SELECT id FROM users WHERE recovery_pin IS NULL AND username IN ('maria', 'juan', 'liza', 'pedro')";
        List<Integer> ids = new ArrayList<Integer>();
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(sql)) {
            while (rs.next()) {
                ids.add(rs.getInt(1));
            }
        }
        for (int id : ids) {
            saveRecoveryPin(id, "1234");
        }
    }

    public int insertUser(String firstName, String lastName, String username, String password, String contact, Role role) throws SQLException {
        String salt = PasswordUtil.newSalt();
        String hash = PasswordUtil.hash(password, salt);
        String sql = "INSERT INTO users (first_name, last_name, username, password_hash, salt, contact, role, active, created_at) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, firstName.trim());
            statement.setString(2, lastName.trim());
            statement.setString(3, username.trim().toLowerCase());
            statement.setString(4, hash);
            statement.setString(5, salt);
            statement.setString(6, contact == null ? "" : contact.trim());
            statement.setString(7, role.name());
            int active = (role == Role.CUSTOMER) ? 0 : 1;
            statement.setInt(8, active);
            statement.setString(9, AppTime.now());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                keys.next();
                return keys.getInt(1);
            }
        }
    }

    private void insertTxn(int customerId, int staffId, String item, double amount, String type, String note, String when) throws SQLException {
        String sql = "INSERT INTO transactions (customer_id, staff_id, item_description, quantity, unit_price, amount, txn_type, note, created_at, voided) "
                + "VALUES (?, ?, ?, 1, ?, ?, ?, ?, ?, 0)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, customerId);
            statement.setInt(2, staffId);
            statement.setString(3, item);
            statement.setDouble(4, amount); // unit_price = amount for seed
            statement.setDouble(5, amount);
            statement.setString(6, type);
            statement.setString(7, note);
            statement.setString(8, when);
            statement.executeUpdate();
        }
    }

    public User findByUsername(String username) throws SQLException {
        String sql = "SELECT id, first_name, last_name, username, contact, role, active, password_hash, salt FROM users WHERE username = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, username.trim().toLowerCase());
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                User user = mapUser(rs);
                user.setActive(rs.getInt("active") == 1);
                return user;
            }
        }
    }

    public String[] credentials(String username) throws SQLException {
        String sql = "SELECT password_hash, salt, active, role FROM users WHERE username = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, username.trim().toLowerCase());
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return new String[] {rs.getString("password_hash"), rs.getString("salt"), String.valueOf(rs.getInt("active")), rs.getString("role")};
            }
        }
    }

    public boolean usernameExists(String username) throws SQLException {
        String sql = "SELECT 1 FROM users WHERE username = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, username.trim().toLowerCase());
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next();
            }
        }
    }

    public User findUser(int id) throws SQLException {
        String sql = "SELECT id, first_name, last_name, username, contact, role, active FROM users WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return mapUser(rs);
            }
        }
    }

    public List<User> listByRole(Role role) throws SQLException {
        String sql = "SELECT id, first_name, last_name, username, contact, role, active FROM users WHERE role = ? ORDER BY last_name, first_name";
        List<User> users = new ArrayList<User>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, role.name());
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    users.add(mapUser(rs));
                }
            }
        }
        return users;
    }

    public List<User> listPendingCustomers() throws SQLException {
        String sql = "SELECT id, first_name, last_name, username, contact, role, active FROM users WHERE role = 'CUSTOMER' AND active = 0 ORDER BY created_at DESC";
        List<User> users = new ArrayList<User>();
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(sql)) {
            while (rs.next()) {
                users.add(mapUser(rs));
            }
        }
        return users;
    }

    public void updateCustomerInfo(int userId, String firstName, String lastName, String username, String contact) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE users SET first_name = ?, last_name = ?, username = ?, contact = ? WHERE id = ? AND role = 'CUSTOMER'")) {
            statement.setString(1, firstName.trim());
            statement.setString(2, lastName.trim());
            statement.setString(3, username.trim().toLowerCase());
            statement.setString(4, contact == null ? "" : contact.trim());
            statement.setInt(5, userId);
            statement.executeUpdate();
        }
    }

    /** True if some account other than {@code userId} already uses this username. */
    public boolean usernameTakenByOther(String username, int userId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT 1 FROM users WHERE username = ? AND id <> ?")) {
            statement.setString(1, username.trim().toLowerCase());
            statement.setInt(2, userId);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next();
            }
        }
    }

    public void updateCustomerContact(int userId, String contact) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("UPDATE users SET contact = ? WHERE id = ? AND role = 'CUSTOMER'")) {
            statement.setString(1, contact == null ? "" : contact.trim());
            statement.setInt(2, userId);
            statement.executeUpdate();
        }
    }

    /** Sets a new password (fresh salt) for a user and clears any PIN lockout. */
    public void updatePassword(int userId, String newPassword) throws SQLException {
        String salt = PasswordUtil.newSalt();
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE users SET password_hash = ?, salt = ?, pin_attempts = 0, pin_locked_until = NULL WHERE id = ?")) {
            statement.setString(1, PasswordUtil.hash(newPassword, salt));
            statement.setString(2, salt);
            statement.setInt(3, userId);
            statement.executeUpdate();
        }
    }

    public void setActive(int userId, boolean active) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("UPDATE users SET active = ? WHERE id = ?")) {
            statement.setInt(1, active ? 1 : 0);
            statement.setInt(2, userId);
            statement.executeUpdate();
        }
    }

    public int addTransaction(int customerId, int staffId, String item, double quantity, double unitPrice, double amount, String type, String note) throws SQLException {
        String sql = "INSERT INTO transactions (customer_id, staff_id, item_description, quantity, unit_price, amount, txn_type, note, created_at, voided) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 0)";
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, customerId);
            statement.setInt(2, staffId);
            statement.setString(3, item);
            statement.setDouble(4, quantity);
            statement.setDouble(5, unitPrice);
            statement.setDouble(6, round(amount));
            statement.setString(7, type);
            statement.setString(8, note == null ? "" : note.trim());
            statement.setString(9, AppTime.now());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                keys.next();
                return keys.getInt(1);
            }
        }
    }

    public void voidTransaction(int id) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("UPDATE transactions SET voided = 1 WHERE id = ? AND voided = 0")) {
            statement.setInt(1, id);
            statement.executeUpdate();
        }
    }

    /** Permanently removes a customer account and all of their transactions. */
    public void deleteCustomer(int userId) throws SQLException {
        boolean previous = connection.getAutoCommit();
        connection.setAutoCommit(false);
        try {
            try (PreparedStatement t = connection.prepareStatement("DELETE FROM transactions WHERE customer_id = ?")) {
                t.setInt(1, userId);
                t.executeUpdate();
            }
            try (PreparedStatement u = connection.prepareStatement("DELETE FROM users WHERE id = ? AND role = 'CUSTOMER'")) {
                u.setInt(1, userId);
                u.executeUpdate();
            }
            connection.commit();
        } catch (SQLException ex) {
            connection.rollback();
            throw ex;
        } finally {
            connection.setAutoCommit(previous);
        }
    }

    public double balanceOf(int customerId) throws SQLException {
        String sql = "SELECT "
                + "COALESCE(SUM(CASE WHEN txn_type = 'UTANG' AND voided = 0 THEN amount ELSE 0 END), 0) - "
                + "COALESCE(SUM(CASE WHEN txn_type = 'PAYMENT' AND voided = 0 THEN amount ELSE 0 END), 0) "
                + "AS balance FROM transactions WHERE customer_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, customerId);
            try (ResultSet rs = statement.executeQuery()) {
                rs.next();
                return round(rs.getDouble("balance"));
            }
        }
    }

    public List<LedgerEntry> history(Integer customerId, String search, boolean includeVoided) throws SQLException {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT t.id, t.customer_id, c.first_name AS customer_first, c.last_name AS customer_last, t.staff_id, ");
        sql.append("s.first_name AS staff_first, s.last_name AS staff_last, t.item_description, t.quantity, t.unit_price, t.amount, ");
        sql.append("t.txn_type, t.note, t.created_at, t.voided ");
        sql.append("FROM transactions t ");
        sql.append("JOIN users c ON c.id = t.customer_id ");
        sql.append("LEFT JOIN users s ON s.id = t.staff_id ");
        sql.append("WHERE 1 = 1 ");
        if (!includeVoided) {
            sql.append("AND t.voided = 0 ");
        }
        if (customerId != null) {
            sql.append("AND t.customer_id = ? ");
        }
        if (search != null && search.trim().length() > 0) {
            sql.append("AND (c.first_name LIKE ? OR c.last_name LIKE ? OR t.item_description LIKE ? OR IFNULL(t.note, '') LIKE ? OR IFNULL(s.first_name, '') LIKE ? OR IFNULL(s.last_name, '') LIKE ?) ");
        }
        sql.append("ORDER BY t.created_at DESC, t.id DESC");

        List<LedgerEntry> rows = new ArrayList<LedgerEntry>();
        try (PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            int index = 1;
            if (customerId != null) {
                statement.setInt(index++, customerId);
            }
            if (search != null && search.trim().length() > 0) {
                String like = "%" + search.trim() + "%";
                statement.setString(index++, like);
                statement.setString(index++, like);
                statement.setString(index++, like);
                statement.setString(index++, like);
                statement.setString(index++, like);
                statement.setString(index, like);
            }
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    rows.add(mapEntry(rs));
                }
            }
        }
        return rows;
    }

    public List<CustomerBalance> customersWithBalances(String search) throws SQLException {
        List<User> customers = listByRole(Role.CUSTOMER);
        List<CustomerBalance> rows = new ArrayList<CustomerBalance>();
        String needle = search == null ? "" : search.trim().toLowerCase();
        for (User customer : customers) {
            if (!customer.isActive()) {
                continue;
            }
            String haystack = ("cust-" + customer.getId() + " " + customer.getFirstName() + " " + customer.getLastName()
                    + " " + customer.getUsername() + " " + customer.getContact()).toLowerCase();
            if (needle.length() > 0 && haystack.indexOf(needle) < 0) {
                continue;
            }
            double balance = balanceOf(customer.getId());
            String last = lastActivity(customer.getId());
            rows.add(new CustomerBalance(customer, balance, last));
        }
        return rows;
    }

    public void audit(int actorId, String actorName, String action, String target, String detail) {
        String sql = "INSERT INTO audit_log (actor_id, actor_name, action, target, detail, created_at) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            if (actorId > 0) {
                statement.setInt(1, actorId);
            } else {
                statement.setNull(1, java.sql.Types.INTEGER);
            }
            statement.setString(2, actorName);
            statement.setString(3, action);
            statement.setString(4, target);
            statement.setString(5, detail);
            statement.setString(6, AppTime.now());
            statement.executeUpdate();
        } catch (SQLException ignored) {
        }
    }

    public List<String[]> recentAudit() throws SQLException {
        List<String[]> rows = new ArrayList<String[]>();
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("SELECT created_at, actor_name, action, target, detail FROM audit_log ORDER BY id DESC LIMIT 200")) {
            while (rs.next()) {
                rows.add(new String[] {rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5)});
            }
        }
        return rows;
    }

    public double collectionsSince(String since) throws SQLException {
        String sql = "SELECT COALESCE(SUM(amount), 0) FROM transactions WHERE txn_type = 'PAYMENT' AND voided = 0 AND created_at >= ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, since);
            try (ResultSet rs = statement.executeQuery()) {
                rs.next();
                return round(rs.getDouble(1));
            }
        }
    }

    public static final int RESET_OK = 0;
    public static final int RESET_BAD = 1;
    public static final int RESET_LOCKED = 2;
    private static final int MAX_PIN_TRIES = 5;
    private static final int LOCK_MINUTES = 15;

    /**
     * PIN-based password reset. Customer accounts only. After 5 wrong tries the
     * account's PIN reset is locked for 15 minutes.
     */
    public int resetPassword(String username, String pin, String newPassword) throws SQLException {
        int id;
        String role;
        String storedPin;
        int attempts;
        String lockedUntil;
        String sql = "SELECT id, role, recovery_pin, pin_attempts, pin_locked_until FROM users WHERE username = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, username.trim().toLowerCase());
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) {
                    return RESET_BAD;
                }
                id = rs.getInt("id");
                role = rs.getString("role");
                storedPin = rs.getString("recovery_pin");
                attempts = rs.getInt("pin_attempts");
                lockedUntil = rs.getString("pin_locked_until");
            }
        }
        if (lockedUntil != null && lockedUntil.compareTo(AppTime.now()) > 0) {
            return RESET_LOCKED;
        }
        boolean allowed = Role.CUSTOMER.name().equals(role) && PasswordUtil.matchesPin(pin, storedPin);
        if (!allowed) {
            attempts++;
            if (attempts >= MAX_PIN_TRIES) {
                setPinState(id, 0, AppTime.minutesFromNow(LOCK_MINUTES));
                return RESET_LOCKED;
            }
            setPinState(id, attempts, null);
            return RESET_BAD;
        }
        String salt = PasswordUtil.newSalt();
        try (PreparedStatement update = connection.prepareStatement(
                "UPDATE users SET password_hash = ?, salt = ?, pin_attempts = 0, pin_locked_until = NULL WHERE id = ?")) {
            update.setString(1, PasswordUtil.hash(newPassword, salt));
            update.setString(2, salt);
            update.setInt(3, id);
            update.executeUpdate();
        }
        return RESET_OK;
    }

    private void setPinState(int userId, int attempts, String lockedUntil) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE users SET pin_attempts = ?, pin_locked_until = ? WHERE id = ?")) {
            statement.setInt(1, attempts);
            statement.setString(2, lockedUntil);
            statement.setInt(3, userId);
            statement.executeUpdate();
        }
    }

    public void saveRecoveryPin(int userId, String pin) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("UPDATE users SET recovery_pin = ? WHERE id = ?")) {
            statement.setString(1, PasswordUtil.isHashedPin(pin) ? pin : PasswordUtil.hashPin(pin));
            statement.setInt(2, userId);
            statement.executeUpdate();
        }
    }

    /** Finds an active customer by contact number (digits only). */
    public User findCustomerByContact(String digits) throws SQLException {
        if (digits == null || digits.isEmpty()) {
            return null;
        }
        String sql = "SELECT id, first_name, last_name, username, contact, role, active FROM users "
                + "WHERE REPLACE(REPLACE(REPLACE(contact, ' ', ''), '-', ''), '+', '') = ? AND role = 'CUSTOMER' LIMIT 1";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, digits);
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return mapUser(rs);
            }
        }
    }

    /** Returns true if the given plain PIN matches the stored recovery PIN for the user. */
    public boolean matchesRecoveryPin(int userId, String pin) throws SQLException {
        String sql = "SELECT recovery_pin FROM users WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) {
                    return false;
                }
                String stored = rs.getString("recovery_pin");
                return PasswordUtil.matchesPin(pin, stored);
            }
        }
    }

    /** Finds an active customer by contact number and resets their recovery PIN to the given value. */
    public User resetPinByContact(String contact, String newPin) throws SQLException {
        String digits = contact == null ? "" : contact.replaceAll("[^0-9]", "");
        if (digits.isEmpty()) {
            return null;
        }
        String sql = "SELECT id, first_name, last_name, username, contact, role, active FROM users "
                + "WHERE REPLACE(REPLACE(REPLACE(contact, ' ', ''), '-', ''), '+', '') = ? AND role = 'CUSTOMER' AND active = 1 LIMIT 1";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, digits);
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                User user = mapUser(rs);
                saveRecoveryPin(user.getId(), newPin);
                // Also clear any PIN lock so they can use it immediately
                setPinState(user.getId(), 0, null);
                return user;
            }
        }
    }

    public String lastActivity(int customerId) throws SQLException {
        String sql = "SELECT created_at FROM transactions WHERE customer_id = ? AND voided = 0 ORDER BY created_at DESC, id DESC LIMIT 1";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, customerId);
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) {
                    return "—";
                }
                return rs.getString(1);
            }
        }
    }

    public KpiSnapshot kpis() throws SQLException {
        double unpaid = 0;
        int withUtang = 0;
        List<User> customers = listByRole(Role.CUSTOMER);
        for (User customer : customers) {
            double balance = balanceOf(customer.getId());
            if (balance > 0.009) {
                unpaid += balance;
                withUtang++;
            }
        }
        int todayCount = 0;
        double collected = 0;
        String prefix = AppTime.todayPrefix();
        String sql = "SELECT txn_type, amount FROM transactions WHERE voided = 0 AND created_at LIKE ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, prefix + "%");
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    todayCount++;
                    if ("PAYMENT".equals(rs.getString("txn_type"))) {
                        collected += rs.getDouble("amount");
                    }
                }
            }
        }
        return new KpiSnapshot(round(unpaid), withUtang, todayCount, round(collected));
    }

    private User mapUser(ResultSet rs) throws SQLException {
        return new User(
                rs.getInt("id"),
                rs.getString("first_name"),
                rs.getString("last_name"),
                rs.getString("username"),
                rs.getString("contact"),
                Role.from(rs.getString("role")),
                rs.getInt("active") == 1);
    }

    private LedgerEntry mapEntry(ResultSet rs) throws SQLException {
        LedgerEntry entry = new LedgerEntry();
        entry.setId(rs.getInt("id"));
        entry.setCustomerId(rs.getInt("customer_id"));
        entry.setCustomerFirstName(rs.getString("customer_first"));
        entry.setCustomerLastName(rs.getString("customer_last"));
        int staffId = rs.getInt("staff_id");
        if (!rs.wasNull()) {
            entry.setStaffId(Integer.valueOf(staffId));
        }
        entry.setStaffFirstName(rs.getString("staff_first"));
        entry.setStaffLastName(rs.getString("staff_last"));
        entry.setItemDescription(rs.getString("item_description"));
        entry.setQuantity(rs.getDouble("quantity"));
        entry.setUnitPrice(rs.getDouble("unit_price"));
        entry.setAmount(rs.getDouble("amount"));
        entry.setType(rs.getString("txn_type"));
        entry.setNote(rs.getString("note"));
        entry.setCreatedAt(rs.getString("created_at"));
        entry.setVoided(rs.getInt("voided") == 1);
        return entry;
    }

    // ------------------------------------------------------------------
    // Activity Logs / Staff Management / Pending Approvals / Reports
    // ------------------------------------------------------------------

    /**
     * Activity Logs rows: {when, customer, staff, activity, description, amount}.
     * Pass a date as yyyy-MM-dd to show only that day, or null for the latest 500.
     */
    public List<String[]> activityLogs(String date) throws SQLException {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT a.created_at, CONCAT(c.first_name, ' ', c.last_name), ");
        sql.append("TRIM(CONCAT(IFNULL(s.first_name, ''), ' ', IFNULL(s.last_name, ''))), ");
        sql.append("a.activity, a.description, a.amount ");
        sql.append("FROM activity_logs a JOIN users c ON c.id = a.customer_id ");
        sql.append("LEFT JOIN users s ON s.id = a.staff_id ");
        boolean byDate = date != null && date.trim().length() > 0;
        if (byDate) {
            sql.append("WHERE DATE(a.created_at) = ? ");
        }
        sql.append("ORDER BY a.created_at DESC, a.id DESC LIMIT 500");
        List<String[]> rows = new ArrayList<String[]>();
        try (PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            if (byDate) {
                statement.setString(1, date.trim());
            }
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    rows.add(new String[] {rs.getString(1), rs.getString(2), rs.getString(3),
                        rs.getString(4), rs.getString(5), String.format("%.2f", rs.getDouble(6))});
                }
            }
        }
        return rows;
    }

    /** Staff Management rows: {userId, name, username, contact, position, hireDate, status, notes}. */
    public List<String[]> staffManagement() throws SQLException {
        String sql = "SELECT u.id, CONCAT(u.first_name, ' ', u.last_name), u.username, u.contact, "
                + "m.position, m.hire_date, m.status, IFNULL(m.notes, '') "
                + "FROM users u JOIN staff_management m ON m.user_id = u.id "
                + "WHERE u.role = 'STAFF' ORDER BY u.last_name, u.first_name";
        List<String[]> rows = new ArrayList<String[]>();
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(sql)) {
            while (rs.next()) {
                rows.add(new String[] {rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4),
                    rs.getString(5), rs.getString(6), rs.getString(7), rs.getString(8)});
            }
        }
        return rows;
    }

    public void updateStaffDetails(int userId, String position, String notes) throws SQLException {
        String sql = "UPDATE staff_management SET position = ?, notes = ? WHERE user_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, position == null || position.trim().isEmpty() ? "Cashier" : position.trim());
            statement.setString(2, notes == null ? "" : notes.trim());
            statement.setInt(3, userId);
            statement.executeUpdate();
        }
    }

    /** Pending Approvals rows: {userId, name, username, contact, requestedAt}. Only customers still waiting. */
    public List<String[]> pendingApprovals() throws SQLException {
        String sql = "SELECT u.id, CONCAT(u.first_name, ' ', u.last_name), u.username, u.contact, p.requested_at "
                + "FROM pending_approvals p JOIN users u ON u.id = p.user_id "
                + "WHERE p.status = 'PENDING' AND u.active = 0 ORDER BY p.requested_at DESC";
        List<String[]> rows = new ArrayList<String[]>();
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(sql)) {
            while (rs.next()) {
                rows.add(new String[] {rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5)});
            }
        }
        return rows;
    }

    /** Saved Reports rows: {id, type, from, to, totalUtang, totalPayments, outstanding, txnCount, generatedAt}. */
    public List<String[]> savedReports() throws SQLException {
        String sql = "SELECT id, report_type, period_start, period_end, total_utang, total_payments, "
                + "outstanding_balance, txn_count, generated_at FROM reports ORDER BY generated_at DESC, id DESC LIMIT 200";
        List<String[]> rows = new ArrayList<String[]>();
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(sql)) {
            while (rs.next()) {
                rows.add(new String[] {rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4),
                    String.format("%.2f", rs.getDouble(5)), String.format("%.2f", rs.getDouble(6)),
                    String.format("%.2f", rs.getDouble(7)), rs.getString(8), rs.getString(9)});
            }
        }
        return rows;
    }

    /**
     * Builds (or refreshes) a saved report for the given period.
     * type: DAILY, WEEKLY, MONTHLY or CUSTOM. Dates are yyyy-MM-dd. generatedBy may be null.
     */
    public void generateReport(String type, String startDate, String endDate, Integer generatedBy) throws SQLException {
        double utang;
        double paid;
        int count;
        String totals = "SELECT COALESCE(SUM(CASE WHEN txn_type = 'UTANG' THEN amount ELSE 0 END), 0), "
                + "COALESCE(SUM(CASE WHEN txn_type = 'PAYMENT' THEN amount ELSE 0 END), 0), COUNT(*) "
                + "FROM transactions WHERE voided = 0 AND created_at >= ? AND created_at < DATE_ADD(?, INTERVAL 1 DAY)";
        try (PreparedStatement statement = connection.prepareStatement(totals)) {
            statement.setString(1, startDate);
            statement.setString(2, endDate);
            try (ResultSet rs = statement.executeQuery()) {
                rs.next();
                utang = rs.getDouble(1);
                paid = rs.getDouble(2);
                count = rs.getInt(3);
            }
        }
        double outstanding;
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("SELECT COALESCE(SUM(CASE WHEN txn_type = 'UTANG' THEN amount ELSE -amount END), 0) "
                     + "FROM transactions WHERE voided = 0")) {
            rs.next();
            outstanding = rs.getDouble(1);
        }
        String save = "INSERT INTO reports (report_type, period_start, period_end, total_utang, total_payments, "
                + "outstanding_balance, txn_count, generated_by, generated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) "
                + "ON DUPLICATE KEY UPDATE total_utang = VALUES(total_utang), total_payments = VALUES(total_payments), "
                + "outstanding_balance = VALUES(outstanding_balance), txn_count = VALUES(txn_count), "
                + "generated_by = VALUES(generated_by), generated_at = VALUES(generated_at)";
        try (PreparedStatement statement = connection.prepareStatement(save)) {
            statement.setString(1, type);
            statement.setString(2, startDate);
            statement.setString(3, endDate);
            statement.setDouble(4, round(utang));
            statement.setDouble(5, round(paid));
            statement.setDouble(6, round(outstanding));
            statement.setInt(7, count);
            if (generatedBy == null) {
                statement.setNull(8, java.sql.Types.INTEGER);
            } else {
                statement.setInt(8, generatedBy.intValue());
            }
            statement.setString(9, AppTime.now());
            statement.executeUpdate();
        }
    }

    public static double round(double amount) {
        return Math.round(amount * 100.0) / 100.0;
    }
}