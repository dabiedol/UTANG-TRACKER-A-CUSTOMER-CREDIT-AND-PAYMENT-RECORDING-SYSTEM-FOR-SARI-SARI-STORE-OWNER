package utangtracker;

import utangtracker.model.User;

public final class Session {
    private static User current;

    private Session() {
    }

    public static User get() {
        return current;
    }

    public static void set(User user) {
        current = user;
    }

    public static void clear() {
        current = null;
    }

    public static boolean isOwner() {
        return current != null && current.getRole() == utangtracker.model.Role.OWNER;
    }

    public static boolean isStaff() {
        return current != null && current.getRole() == utangtracker.model.Role.STAFF;
    }
}
