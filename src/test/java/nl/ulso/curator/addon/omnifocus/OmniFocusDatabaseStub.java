package nl.ulso.curator.addon.omnifocus;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
final class OmniFocusDatabaseStub
    implements OmniFocusDatabase
{
    @Inject
    OmniFocusDatabaseStub()
    {
    }

    @Override
    public boolean isAccessible()
    {
        return true;
    }

    @Override
    public String path()
    {
        return "/path/to/stub";
    }

    @Override
    public long lastModified()
    {
        return System.currentTimeMillis();
    }
}
