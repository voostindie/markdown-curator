package nl.ulso.curator.query;

import org.junit.jupiter.api.Test;

import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class ResourceBundleGeneralMessagesTest
{
    @Test
    void followsLocaleChangesAtRuntime()
    {
        var locale = new AtomicReference<>(Locale.US);
        var messages = new ResourceBundleGeneralMessages(locale::get);
        var english = messages.noResults();
        locale.set(Locale.forLanguageTag("nl"));
        var dutch = messages.noResults();
        assertThat(english).isNotEqualTo(dutch);
    }

    @Test
    void doesNotFallBackToDefaultLocaleOfJvm()
    {
        var english = new ResourceBundleGeneralMessages(Locale.US).noResults();
        var defaultLocale = Locale.getDefault();
        try
        {
            Locale.setDefault(Locale.forLanguageTag("nl"));
            var messages = new ResourceBundleGeneralMessages(Locale.US);
            assertThat(messages.noResults()).isEqualTo(english);
            var unknown = new ResourceBundleGeneralMessages(Locale.forLanguageTag("fr"));
            assertThat(unknown.noResults()).isEqualTo(english);
        }
        finally
        {
            Locale.setDefault(defaultLocale);
        }
    }

    @Test
    void emptyResultFollowsLocaleChangesAtRuntime()
    {
        var locale = new AtomicReference<>(Locale.US);
        var formatter = new EmptyMarkdownFormatter(new ResourceBundleGeneralMessages(locale::get));
        var english = formatter.format(null);
        locale.set(Locale.forLanguageTag("nl"));
        assertThat(formatter.format(null)).isNotEqualTo(english);
    }
}
