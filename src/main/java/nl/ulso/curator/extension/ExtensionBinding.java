package nl.ulso.curator.extension;

import jakarta.inject.Qualifier;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

/// Qualifier for the [nl.ulso.curator.change.ChangeProcessor]s,
/// [nl.ulso.curator.query.Query]s and [nl.ulso.curator.statistics.MeasurementTracker]s that
/// belong to an [Extension].
///
/// Dagger subcomponents inherit the multibindings of their parent. Without this qualifier, the
/// sets exposed by an extension would also contain all statically configured instances.
@Qualifier
@Documented
@Retention(RUNTIME)
public @interface ExtensionBinding
{
}
