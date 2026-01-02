package nl.ulso.markdown_curator;

import jakarta.inject.Qualifier;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/// Qualifier for external change object types; these are types ([Class<?>]) that are provided from
/// other sources than [ChangeProcessor]s, but are consumed by them.
///
/// By default, if a [ChangeProcessor publishes to consume an event type that is not produced by any other
/// [[ChangeProcessor] in the system, the curator will fall to start up.
@Qualifier
@Retention(RetentionPolicy.RUNTIME)
public @interface ExternalChangeObjectType
{
}
