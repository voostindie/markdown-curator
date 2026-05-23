package nl.ulso.curator.addon.omnifocus;

import static nl.ulso.curator.addon.omnifocus.Status.UNKNOWN;

public record OmniFocusProject(String id, String name, Status status, int priority)
{
    private static final String NULL_ID = "Null ID";
    static final OmniFocusProject NULL_PROJECT =
        new OmniFocusProject(NULL_ID, "Null Name", UNKNOWN, 0);

    public OmniFocusProject(String id, String name, Status status)
    {
        this(id, name, status, 0);
    }

    OmniFocusProject withUpdatedPriority(int priority)
    {
        return new OmniFocusProject(id, name, status, priority);
    }

    public boolean exists()
    {
        return !id.contentEquals(NULL_ID);
    }

    public String link()
    {
        return "omnifocus:///task/" + id;
    }
}
