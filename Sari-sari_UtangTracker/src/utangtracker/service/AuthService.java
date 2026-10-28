package utangtracker.service;

import utangtracker.Session;
import utangtracker.db.Database;
import utangtracker.model.Role;
import utangtracker.model.User;
import utangtracker.util.PasswordUtil;

import java.sql.SQLException;
import java.util.List;

public class AuthService {
    private final Database database = Database.get();

    public User login(String username, String password) throws AppException {
        if (isBlank(username) || isBlank(password)) {
            throw new AppException("Enter both username and password.");
        }
        try {
            // Allow login with contact number + recovery PIN (after owner resets PIN)
            String digits = username.replaceAll("[^0-9]", "");
            if (digits.length() >= 10 && password.trim().matches("\\d{4}")) {
                User byContact = database.findCustomerByContact(digits);
                if (byContact != null && database.matchesRecoveryPin(byContact.getId(), password.trim())) {
                    if (!byContact.isActive()) {
                        throw new AppException("Your account is waiting for the owner's approval. Please present a valid ID to the owner.");
                    }
                    return byContact;
                }
            }

            String[] credentials = database.credentials(username);
            if (credentials == null || !PasswordUtil.matches(password, credentials[1], credentials[0])) {
                throw new AppException("Wrong username or password.");
            }
            if (!"1".equals(credentials[2])) {
                if (Role.CUSTOMER.name().equals(credentials[3])) {
                    throw new AppException("Your account is waiting for the owner's approval. Please present a valid ID to the owner.");
                }
                throw new AppException("This account is inactive. Ask the owner to restore it.");
            }
            return database.findByUsername(username);
        } catch (SQLException ex) {
            throw new AppException("Cannot sign in right now. Please try again.");
        }
    }

    /**
     * Owner resets a customer's recovery PIN by contact number to the new 4-digit PIN
     * the customer chose. Also clears any PIN lock on that account.
     */
    public User resetPinByContact(String contact, String newPin, String confirmPin) throws AppException {
        requireOwner();
        if (contact == null || contact.replaceAll("[^0-9]", "").length() < 10) {
            throw new AppException("Enter a valid contact number (at least 10 digits).");
        }
        if (newPin == null || !newPin.trim().matches("\\d{4}")) {
            throw new AppException("New PIN must be exactly 4 numbers.");
        }
        if (!newPin.trim().equals(confirmPin == null ? null : confirmPin.trim())) {
            throw new AppException("New PIN and Confirm PIN do not match.");
        }
        try {
            User user = database.resetPinByContact(contact, newPin.trim());
            if (user == null) {
                throw new AppException("No active customer found with that contact number.");
            }
            database.audit(Session.get().getId(), actor(), "UPDATE", "CUST-" + user.getId(),
                    "Reset recovery PIN for " + user.getFirstName() + " " + user.getLastName());
            return user;
        } catch (SQLException ex) {
            throw new AppException("Cannot reset the PIN right now.");
        }
    }

    public void resetPassword(String username, String pin, String newPassword, String confirm) throws AppException {
        if (isBlank(username)) {
            throw new AppException("Enter the username.");
        }
        if (pin == null || !pin.trim().matches("\\d{4}")) {
            throw new AppException("Recovery PIN must be exactly 4 numbers.");
        }
        if (isBlank(newPassword) || !isStrongPassword(newPassword)) {
            throw new AppException("New password must be 8–12 characters and include at least one capital letter, one number, and one special character.");
        }
        if (!newPassword.equals(confirm)) {
            throw new AppException("Password and confirmation do not match.");
        }
        try {
            int result = database.resetPassword(username, pin.trim(), newPassword);
            if (result == Database.RESET_LOCKED) {
                throw new AppException("Too many wrong PIN tries. Try again in 15 minutes.");
            }
            if (result != Database.RESET_OK) {
                throw new AppException("Username or recovery PIN is not correct, or this account cannot use PIN recovery. Staff and owner accounts: ask the owner.");
            }
        } catch (SQLException ex) {
            throw new AppException("Cannot reset the password right now.");
        }
    }

    public User registerCustomer(String firstName, String lastName, String username, String password, String confirm, String contact, String pin) throws AppException {
        validatePerson(firstName, lastName, username, password, confirm, contact, true);
        if (pin == null || !pin.trim().matches("\\d{4}")) {
            throw new AppException("Recovery PIN must be exactly 4 numbers.");
        }
        try {
            if (database.usernameExists(username)) {
                throw new AppException("That username is already taken.");
            }
            int id = database.insertUser(firstName, lastName, username, password, contact, Role.CUSTOMER);
            database.saveRecoveryPin(id, pin.trim());
            database.audit(id, firstName.trim() + " " + lastName.trim(), "CREATE", "CUST-" + id, "Registered customer account");
            return database.findUser(id);
        } catch (SQLException ex) {
            throw new AppException("Cannot create the account right now.");
        }
    }

    public User createStaff(String firstName, String lastName, String username, String password, String contact) throws AppException {
        requireOwner();
        validatePerson(firstName, lastName, username, password, password, contact, false);
        try {
            if (database.usernameExists(username)) {
                throw new AppException("That username is already taken.");
            }
            int id = database.insertUser(firstName, lastName, username, password, contact, Role.STAFF);
            database.audit(Session.get().getId(), actor(), "CREATE", "STAFF-" + id, "Created staff account " + username.trim().toLowerCase());
            return database.findUser(id);
        } catch (SQLException ex) {
            throw new AppException("Cannot create the staff account right now.");
        }
    }

    public void updateCustomerInfo(int userId, String firstName, String lastName, String username, String contact) throws AppException {
        requireOwner();
        if (firstName == null || firstName.trim().length() < 2) {
            throw new AppException("Enter a valid first name.");
        }
        if (lastName == null || lastName.trim().length() < 2) {
            throw new AppException("Enter a valid last name.");
        }
        if (isBlank(username) || !username.trim().matches("[A-Za-z0-9._]{4,20}")) {
            throw new AppException("Username must be 4–20 letters, numbers, dots, or underscores.");
        }
        String digits = contact == null ? "" : contact.replaceAll("[^0-9]", "");
        if (digits.length() < 10 || digits.length() > 13) {
            throw new AppException("Enter a valid contact number (10–13 digits).");
        }
        try {
            User user = database.findUser(userId);
            if (user == null || user.getRole() != Role.CUSTOMER) {
                throw new AppException("Only customer accounts can be updated here.");
            }
            String newUsername = username.trim().toLowerCase();
            boolean usernameChanged = !newUsername.equalsIgnoreCase(user.getUsername());
            if (usernameChanged && database.usernameTakenByOther(newUsername, userId)) {
                throw new AppException("That username is already taken.");
            }
            database.updateCustomerInfo(userId, firstName, lastName, newUsername, contact);
            database.audit(Session.get().getId(), actor(), "UPDATE", "CUST-" + userId,
                    usernameChanged
                            ? "Updated customer info (username " + user.getUsername() + " -> " + newUsername + ")"
                            : "Updated customer info");
        } catch (java.sql.SQLIntegrityConstraintViolationException ex) {
            throw new AppException("That username is already taken.");
        } catch (SQLException ex) {
            throw new AppException("Cannot update customer information.");
        }
    }

    public void updateCustomerContact(int userId, String contact) throws AppException {
        requireOwner();
        try {
            User user = database.findUser(userId);
            if (user == null || user.getRole() != Role.CUSTOMER) {
                throw new AppException("Only customer accounts can be updated here.");
            }
            String digits = contact == null ? "" : contact.replaceAll("[^0-9]", "");
            if (digits.length() < 10 || digits.length() > 13) {
                throw new AppException("Enter a valid contact number (10–13 digits).");
            }
            database.updateCustomerContact(userId, contact);
            database.audit(Session.get().getId(), Session.get().getFirstName() + " " + Session.get().getLastName(),
                    "UPDATE", "CUST-" + userId, "Updated contact to " + contact);
        } catch (SQLException ex) {
            throw new AppException("Cannot update customer contact.");
        }
    }

    public void approveCustomer(int userId) throws AppException {
        requireOwner();
        try {
            User user = database.findUser(userId);
            if (user == null || user.getRole() != Role.CUSTOMER) {
                throw new AppException("Only customer accounts can be approved here.");
            }
            database.setActive(userId, true);
            database.audit(Session.get().getId(), Session.get().getFirstName() + " " + Session.get().getLastName(),
                    "APPROVE", "CUST-" + userId, "Approved customer registration");
        } catch (SQLException ex) {
            throw new AppException("Cannot approve that customer.");
        }
    }

    /** Owner sets a new password for a staff account (staff only, never other owners/customers). */
    public void resetStaffPassword(int userId, String newPassword, String confirm) throws AppException {
        requireOwner();
        if (isBlank(newPassword) || !isStrongPassword(newPassword)) {
            throw new AppException("New password must be 8\u201312 characters and include at least one capital letter, one number, and one special character.");
        }
        if (!newPassword.equals(confirm)) {
            throw new AppException("Password and confirmation do not match.");
        }
        try {
            User user = database.findUser(userId);
            if (user == null || user.getRole() != Role.STAFF) {
                throw new AppException("Only staff accounts can be reset here.");
            }
            database.updatePassword(userId, newPassword);
            database.audit(Session.get().getId(), actor(), "UPDATE", "STAFF-" + userId,
                    "Changed password for staff " + user.getUsername());
        } catch (SQLException ex) {
            throw new AppException("Cannot change the password right now.");
        }
    }

    /** Owner deletes a customer account. Refused while the customer still owes money. */
    public void deleteCustomer(int userId) throws AppException {
        requireOwner();
        try {
            User user = database.findUser(userId);
            if (user == null || user.getRole() != Role.CUSTOMER) {
                throw new AppException("Only customer accounts can be deleted here.");
            }
            double balance = database.balanceOf(userId);
            if (balance > 0.009) {
                throw new AppException("This customer still owes " + String.format("\u20B1%.2f", balance)
                        + ".\nRecord their payment first, then delete the account.");
            }
            database.deleteCustomer(userId);
            database.audit(Session.get().getId(), actor(), "DELETE", "CUST-" + userId,
                    "Deleted customer account " + user.getFirstName() + " " + user.getLastName());
        } catch (SQLException ex) {
            throw new AppException("Cannot delete that customer right now.");
        }
    }

    public List<User> pendingCustomers() throws AppException {
        requireOwner();
        try {
            return database.listPendingCustomers();
        } catch (SQLException ex) {
            throw new AppException("Cannot load pending customers.");
        }
    }

    public void setStaffActive(int userId, boolean active) throws AppException {
        requireOwner();
        try {
            User user = database.findUser(userId);
            if (user == null || user.getRole() != Role.STAFF) {
                throw new AppException("Only staff accounts can be activated or deactivated here.");
            }
            database.setActive(userId, active);
            database.audit(Session.get().getId(), actor(), active ? "RESTORE" : "DEACTIVATE", "STAFF-" + userId,
                    active ? "Reactivated staff account" : "Deactivated staff account");
        } catch (SQLException ex) {
            throw new AppException("Cannot update that staff account.");
        }
    }

    private void validatePerson(String firstName, String lastName, String username, String password, String confirm, String contact, boolean customer) throws AppException {
        if (isBlank(firstName) || firstName.trim().length() < 2) {
            throw new AppException("Enter a first name.");
        }
        if (isBlank(lastName) || lastName.trim().length() < 2) {
            throw new AppException("Enter a last name.");
        }
        if (isBlank(username) || !username.trim().matches("[A-Za-z0-9._]{4,20}")) {
            throw new AppException("Username must be 4–20 letters, numbers, dots, or underscores.");
        }
        if (isBlank(password) || !isStrongPassword(password)) {
            throw new AppException("Password must be 8–12 characters and include at least one capital letter, one number, and one special character.");
        }
        if (!password.equals(confirm)) {
            throw new AppException("Password and confirmation do not match.");
        }
        String digits = contact == null ? "" : contact.replaceAll("[^0-9]", "");
        if (customer && (digits.length() < 10 || digits.length() > 13)) {
            throw new AppException("Enter a valid contact number so the owner can send a reminder.");
        }
        if (!customer && digits.length() > 0 && (digits.length() < 10 || digits.length() > 13)) {
            throw new AppException("Contact number should be 10–13 digits, or leave it blank.");
        }
    }

    private boolean isStrongPassword(String password) {
        if (password == null || password.length() < 8 || password.length() > 12) {
            return false;
        }
        boolean hasUpper = false;
        boolean hasDigit = false;
        boolean hasSpecial = false;
        for (char c : password.toCharArray()) {
            if (Character.isUpperCase(c)) hasUpper = true;
            else if (Character.isDigit(c)) hasDigit = true;
            else if (!Character.isLetterOrDigit(c)) hasSpecial = true;
        }
        return hasUpper && hasDigit && hasSpecial;
    }

    private void requireOwner() throws AppException {
        if (Session.get() == null || Session.get().getRole() != Role.OWNER) {
            throw new AppException("Only the owner can do that.");
        }
    }

    private String actor() {
        return Session.get().getFirstName() + " " + Session.get().getLastName();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
