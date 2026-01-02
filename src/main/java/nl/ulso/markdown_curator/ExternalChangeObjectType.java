package nl.ulso.markdown_curator;

import jakarta.inject.Qualifier;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/// Qualifier for external [Change] object types; these are types ([Class<?>]) that are provided
/// from other sources than [ChangeProcessor]s, but are consumed by them.
///
/// By default, if a [ChangeProcessor] declares to consume an object type that is not produced by
@Qualifier
@Retention(RetentionPolicy.RUNTIME)
public @interface ExternalChangeObjectType
{
}
