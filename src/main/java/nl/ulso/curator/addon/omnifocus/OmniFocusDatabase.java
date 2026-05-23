package nl.ulso.curator.addon.omnifocus;

/// Simple wrapper around the OmniFocus database file.
interface OmniFocusDatabase
{
    boolean isAccessible();

    String path();

    long lastModified();
}
