package nl.ulso.curator.addon.omnifocus;

import nl.ulso.curator.change.Change;
import nl.ulso.curator.change.ChangeProcessor;

import static nl.ulso.curator.change.Change.update;

/// Represents a change in OmniFocus.
///
/// The [DefaultOmniFocusRepository] produces an event of this type whenever a relevant change in OmniFocus
/// is detected. This repository is not a [ChangeProcessor] however, instead it runs autonomously on
/// its own scheduled thread.
///
/// The [OmniFocusProjectAttributeValueProducer] triggers on changes of this type and produces
/// attribute values for all available projects.
///
/// The [OmniFocusInitializer] ensures the projects are loaded from OmniFocus at application
/// startup.
record OmniFocusUpdate()
{
    static final Change<?> OMNIFOCUS_CHANGE = update(new OmniFocusUpdate(), OmniFocusUpdate.class);

    @Override
    public String toString()
    {
        return ".";
    }
}
