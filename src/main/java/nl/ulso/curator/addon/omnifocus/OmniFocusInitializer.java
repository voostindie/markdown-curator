package nl.ulso.curator.addon.omnifocus;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import nl.ulso.curator.change.*;

import java.util.Set;

import static nl.ulso.curator.change.Changelog.changelogFor;

/// OmniFocus projects are fetched in a background process; this processor blocks until the initial
/// fetch is complete. It is triggered only on [Reset].
@Singleton
final class OmniFocusInitializer
    implements ChangeProcessor
{
    private final DefaultOmniFocusRepository omniFocusRepository;

    @Inject
    OmniFocusInitializer(DefaultOmniFocusRepository omniFocusRepository)
    {
        this.omniFocusRepository = omniFocusRepository;
    }

    @Override
    public Set<Class<?>> consumedPayloadTypes()
    {
        return Set.of(Reset.class);
    }

    @Override
    public Set<Class<?>> producedPayloadTypes()
    {
        return Set.of(OmniFocusUpdate.class);
    }

    @Override
    public Changelog apply(Changelog changelog)
    {
        omniFocusRepository.waitForInitialFetchToComplete();
        return changelogFor(OmniFocusUpdate.OMNIFOCUS_CHANGE);
    }
}
