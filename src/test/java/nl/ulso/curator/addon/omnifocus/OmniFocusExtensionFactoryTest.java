package nl.ulso.curator.addon.omnifocus;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static nl.ulso.curator.addon.omnifocus.OmniFocusSettings.from;
import static nl.ulso.dictionary.Dictionary.emptyDictionary;
import static nl.ulso.dictionary.Dictionary.mapDictionary;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OmniFocusExtensionFactoryTest
{
    @Test
    void folderIsRequired()
    {
        assertThatThrownBy(() -> from(emptyDictionary()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("'folder' is required");
    }

    @Test
    void factoryFailsEagerlyOnMissingFolder()
    {
        var extensionFactory = new OmniFocusExtensionFactory(
            (configuration, projectRepository, attributeDefinitions) -> null);
        var configuration = emptyDictionary();
        assertThatThrownBy(() -> extensionFactory.create(configuration, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("'folder' is required");
    }

    @Test
    void includeEverythingByDefault()
    {
        var settings = from(mapDictionary(Map.of("folder", "Work")));
        assertThat(settings.omniFocusFolder()).isEqualTo("Work");
        assertThat(settings.includePredicate().test("⚡️ Anything")).isTrue();
        assertThat(settings.database()).isEqualTo(OmniFocusSettings.DEFAULT_DATABASE);
    }

    @Test
    void customDatabase()
    {
        var settings = from(mapDictionary(Map.of(
            "folder", "Work",
            "database", "/tmp/OmniFocus.ofocus"
        )));
        assertThat(settings.database()).isEqualTo(Path.of("/tmp/OmniFocus.ofocus"));
    }

    @Test
    void excludeOnPrefixAndName()
    {
        var settings = from(mapDictionary(Map.of(
            "folder", "Work",
            "exclude-prefixes", List.of("⚡️", "💤"),
            "exclude", List.of("🤖 Routine")
        )));
        var include = settings.includePredicate();
        assertThat(include.test("Project")).isTrue();
        assertThat(include.test("⚡️ Quick")).isFalse();
        assertThat(include.test("💤 Someday")).isFalse();
        assertThat(include.test("🤖 Routine")).isFalse();
        assertThat(include.test("🤖 Routine 2")).isTrue();
    }
}
