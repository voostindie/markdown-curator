package nl.ulso.curator.addon.omnifocus;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import nl.ulso.curator.change.Change;
import nl.ulso.curator.change.ExternalChangeHandler;

@Singleton
final class ExternalChangeHandlerStub
    implements ExternalChangeHandler
{
    @Inject
    ExternalChangeHandlerStub()
    {
    }

    @Override
    public void process(Change<?> change)
    {
        // Do nothing
    }
}
