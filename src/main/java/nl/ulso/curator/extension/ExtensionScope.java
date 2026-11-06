package nl.ulso.curator.extension;

import jakarta.inject.Scope;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

/// Scope for everything inside an [Extension].
///
/// Change processors are stateful and must exist only once per extension instance. Use this
/// scope instead of [jakarta.inject.Singleton] for every class in an extension; Dagger does not
/// allow a subcomponent to use the scope of its parent.
@Scope
@Documented
@Retention(RUNTIME)
public @interface ExtensionScope
{
}
