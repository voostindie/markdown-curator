package nl.ulso.curator.addon.omnifocus;

public enum Status
{
    ACTIVE,
    ON_HOLD,
    DONE,
    DROPPED,
    UNKNOWN;

    public static Status fromString(String status)
    {
        return switch (status)
        {
            case "active status" -> ACTIVE;
            case "on hold status" -> ON_HOLD;
            case "done status" -> DONE;
            case "dropped status" -> DROPPED;
            default -> throw new IllegalStateException("Unexpected value: " + status);
        };
    }
}
