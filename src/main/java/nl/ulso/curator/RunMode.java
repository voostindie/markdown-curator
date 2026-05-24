package nl.ulso.curator;

/// Defines how the application is running: as a one-off, or as a daemon.
///
/// The [Application] sets the run mode prior to firing of the curators. Any code in the curator can
/// check the current run mode calling [#get()]. It's not the prettiest way of doing it, but
/// properly injecting the run mode into Dagger modules that are created through
/// [java.util.ServiceLoader]s is actually trickier...
public enum RunMode
{
    ONCE,
    DAEMON;

    private static RunMode RUN_MODE;

    static void set(RunMode runMode)
    {
        RUN_MODE = runMode;
    }

    public static RunMode get()
    {
        return RUN_MODE;
    }
}
