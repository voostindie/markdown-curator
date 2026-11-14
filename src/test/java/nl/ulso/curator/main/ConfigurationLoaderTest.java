package nl.ulso.curator.main;

import nl.ulso.curator.change.Change;
import nl.ulso.curator.change.Changelog;
import nl.ulso.curator.change.Reset;
import nl.ulso.curator.vault.Document;
import nl.ulso.curator.vault.VaultStub;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static nl.ulso.curator.change.Change.isPayloadType;
import static nl.ulso.curator.change.Changelog.changelogFor;
import static nl.ulso.curator.change.Reset.RESET;
import static nl.ulso.curator.main.CuratorSettings.DEFAULT_DELAY_IN_SECONDS;
import static nl.ulso.curator.main.CuratorSettings.DEFAULT_LOCALE;
import static nl.ulso.curator.main.ExtensionTestSupport.newExtensionRegistry;

/// Tests the loading of the settings of the curator itself by the [ConfigurationLoader]. The
/// loading of extensions is tested in [ExtensionLoadingTest].
@ExtendWith(SoftAssertionsExtension.class)
class ConfigurationLoaderTest
{
    private static final String CONFIGURATION_DOCUMENT = "config";
    private static final Locale DEFAULT = Locale.forLanguageTag(DEFAULT_LOCALE);

    @InjectSoftAssertions
    private SoftAssertions softly;

    private VaultStub vault;
    private CuratorSettings settings;
    private ConfigurationLoader loader;

    @BeforeEach
    void setUp()
    {
        vault = new VaultStub();
        settings = new CuratorSettings();
        loader = newLoader(Optional.of(CONFIGURATION_DOCUMENT));
    }

    private ConfigurationLoader newLoader(Optional<String> configurationDocumentName)
    {
        return new ConfigurationLoader(configurationDocumentName, vault,
            new ExtensionResolver(Map.of(), Set.of(), Set.of()), newExtensionRegistry(), settings
        );
    }

    private Document configure(String yaml)
    {
        return vault.addDocument(CONFIGURATION_DOCUMENT,
            "Some text.\n\n```yaml\n" + yaml + "\n```\n");
    }

    private Changelog startUp()
    {
        return loader.apply(changelogFor(RESET));
    }

    private Changelog configurationChanged(String yaml)
    {
        return loader.apply(changelogFor(Change.update(configure(yaml), Document.class)));
    }

    private static boolean hasReset(Changelog changelog)
    {
        return changelog.changes().anyMatch(isPayloadType(Reset.class));
    }

    @Test
    void defaultDelayWithoutConfigurationDocument()
    {
        startUp();
        softly.assertThat(settings.delayInSeconds()).isEqualTo(DEFAULT_DELAY_IN_SECONDS);
    }

    @Test
    void defaultDelayWithoutConfiguredDocumentName()
    {
        loader = newLoader(Optional.empty());
        configure("delay: 10");
        startUp();
        softly.assertThat(settings.delayInSeconds()).isEqualTo(DEFAULT_DELAY_IN_SECONDS);
    }

    @Test
    void defaultDelayIfNotConfigured()
    {
        configure("extensions:");
        startUp();
        softly.assertThat(settings.delayInSeconds()).isEqualTo(DEFAULT_DELAY_IN_SECONDS);
    }

    @Test
    void loadDelayAtStartup()
    {
        configure("delay: 10");
        startUp();
        softly.assertThat(settings.delayInSeconds()).isEqualTo(10);
    }

    @Test
    void zeroDelayIsAllowed()
    {
        configure("delay: 0");
        startUp();
        softly.assertThat(settings.delayInSeconds()).isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = {"-1", "1.5", "soon", "[1, 2]", "12345678901"})
    void invalidDelayFallsBackToDefault(String delay)
    {
        configure("delay: 10");
        startUp();
        configurationChanged("delay: " + delay);
        softly.assertThat(settings.delayInSeconds()).isEqualTo(DEFAULT_DELAY_IN_SECONDS);
    }

    @Test
    void changingDelayAppliesImmediatelyWithoutReset()
    {
        configure("delay: 10");
        startUp();
        var changelog = configurationChanged("delay: 1");
        softly.assertThat(settings.delayInSeconds()).isEqualTo(1);
        softly.assertThat(hasReset(changelog)).isFalse();
        softly.assertThat(vault.reloadCount()).isZero();
    }

    @Test
    void removingDelayRestoresDefault()
    {
        configure("delay: 10");
        startUp();
        configurationChanged("extensions:");
        softly.assertThat(settings.delayInSeconds()).isEqualTo(DEFAULT_DELAY_IN_SECONDS);
    }

    @Test
    void deletingConfigurationDocumentRestoresDefault()
    {
        var document = configure("delay: 10");
        startUp();
        loader.apply(changelogFor(Change.delete(document, Document.class)));
        softly.assertThat(settings.delayInSeconds()).isEqualTo(DEFAULT_DELAY_IN_SECONDS);
    }

    @Test
    void delayIsReloadedOnReset()
    {
        configure("delay: 10");
        startUp();
        configure("delay: 7");
        startUp();
        softly.assertThat(settings.delayInSeconds()).isEqualTo(7);
    }

    @Test
    void changesToOtherDocumentsAreIgnored()
    {
        configure("delay: 10");
        startUp();
        var other = vault.addDocument("other", "```yaml\ndelay: 1\n```\n");
        loader.apply(changelogFor(Change.update(other, Document.class)));
        softly.assertThat(settings.delayInSeconds()).isEqualTo(10);
    }

    // Locale

    @Test
    void defaultLocaleIsEnglish()
    {
        softly.assertThat(DEFAULT_LOCALE).isEqualTo(Locale.ENGLISH.toLanguageTag());
        configure("extensions:");
        startUp();
        softly.assertThat(settings.locale()).isEqualTo(Locale.ENGLISH);
    }

    @ParameterizedTest
    @ValueSource(strings = {"nl", "nl-NL", "nl_NL", "en-GB", " de "})
    void loadLocaleAtStartup(String tag)
    {
        configure("locale: " + tag);
        startUp();
        softly.assertThat(settings.locale())
            .isEqualTo(Locale.forLanguageTag(tag.strip().replace('_', '-')));
    }

    @ParameterizedTest
    @ValueSource(strings = {"english", "xx", "123", "''", "[nl]", "42"})
    void invalidLocaleFallsBackToDefault(String tag)
    {
        configure("locale: nl");
        startUp();
        configurationChanged("locale: " + tag);
        softly.assertThat(settings.locale()).isEqualTo(DEFAULT);
    }

    @Test
    void changingLocaleForcesCompleteRefresh()
    {
        configure("locale: en-US");
        startUp();
        var changelog = configurationChanged("locale: nl");
        softly.assertThat(settings.locale()).isEqualTo(Locale.forLanguageTag("nl"));
        softly.assertThat(hasReset(changelog)).isTrue();
        softly.assertThat(vault.reloadCount()).isOne();
    }

    @Test
    void unchangedLocaleDoesNotRefresh()
    {
        configure("locale: nl");
        startUp();
        var changelog = configurationChanged("locale: nl\ndelay: 1");
        softly.assertThat(hasReset(changelog)).isFalse();
        softly.assertThat(vault.reloadCount()).isZero();
    }

    @Test
    void equivalentLocaleDoesNotRefresh()
    {
        configure("locale: en-US");
        startUp();
        var changelog = configurationChanged("locale: en_US");
        softly.assertThat(hasReset(changelog)).isFalse();
    }

    @Test
    void removingLocaleRestoresDefaultAndRefreshes()
    {
        configure("locale: nl");
        startUp();
        var changelog = configurationChanged("delay: 3");
        softly.assertThat(settings.locale()).isEqualTo(DEFAULT);
        softly.assertThat(hasReset(changelog)).isTrue();
    }

    @Test
    void localeIsReloadedOnResetWithoutPublishingAnotherReset()
    {
        configure("locale: nl");
        var changelog = startUp();
        softly.assertThat(settings.locale()).isEqualTo(Locale.forLanguageTag("nl"));
        softly.assertThat(hasReset(changelog)).isFalse();
    }
}
