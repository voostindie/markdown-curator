package nl.ulso.curator.change;

/// Special change that triggers all change processors to be reset and all queries to be run.
///
/// When a [Reset] change is present in the changelog, first all change processors are reset. Next,
/// all changes up to the [Reset] change are discarded.
///
/// The [Reset] change is a reserved change: only the system itself can produce it. It does so
/// at application startup, and when the optional watch document is changed on disk.
public final class Reset
{
    public static final Change<Reset> RESET = Change.create(new Reset(), Reset.class);

    private Reset()
    {
    }

    @Override
    public String toString()
    {
        return "⏻";
    }
}
