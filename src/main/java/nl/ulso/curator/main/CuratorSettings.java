package nl.ulso.curator.main;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.Locale;

import static java.util.Locale.ENGLISH;
import static java.util.Objects.requireNonNull;

/// Holds the settings of the curator itself, as read from the configuration document by the
/// [ConfigurationLoader]. Until the configuration is loaded, and for settings that aren't
/// configured, the defaults apply.
///
/// Settings can change at any moment, from the thread that processes changes. Readers must
/// therefore not cache them, but ask for them every time they need them.
@Singleton
final class CuratorSettings
{
    /// Default delay, in seconds, between the last detected change and running the queries.
    static final int DEFAULT_DELAY_IN_SECONDS = 3;

    /// Default locale for translations in query output.
    static final String DEFAULT_LOCALE = ENGLISH.toLanguageTag();

    private volatile int delayInSeconds;
    private volatile Locale locale;

    @Inject
    CuratorSettings()
    {
        this.delayInSeconds = DEFAULT_DELAY_IN_SECONDS;
        this.locale = Locale.forLanguageTag(DEFAULT_LOCALE);
    }

    /// @return the delay, in seconds, between the last detected change and running the queries and
    /// writing the results to disk; see [DefaultCurator].
    int delayInSeconds()
    {
        return delayInSeconds;
    }

    void setDelayInSeconds(int delayInSeconds)
    {
        this.delayInSeconds = delayInSeconds < 0 ? DEFAULT_DELAY_IN_SECONDS : delayInSeconds;
    }

    /// @return the locale for translations in query output.
    Locale locale()
    {
        return locale;
    }

    void setLocale(Locale locale)
    {
        this.locale = requireNonNull(locale);
    }
}
