package utangtracker.model;

public enum Role {
    CUSTOMER,
    STAFF,
    OWNER;

    public String label() {
        switch (this) {
            case CUSTOMER:
                return "Customer";
            case STAFF:
                return "Staff";
            case OWNER:
                return "Owner";
            default:
                return name();
        }
    }

    public static Role from(String value) {
        return Role.valueOf(value.trim().toUpperCase());
    }
}
