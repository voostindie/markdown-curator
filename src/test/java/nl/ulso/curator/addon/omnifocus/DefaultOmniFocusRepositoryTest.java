package nl.ulso.curator.addon.omnifocus;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DefaultOmniFocusRepositoryTest
{
    @Test
    void inaccessibleDatabaseIsRejected()
    {
        var settings = new OmniFocusSettings("Area", name -> true, Path.of("NOTHING"));
        assertThatThrownBy(() -> new DefaultOmniFocusRepository(null, null, settings))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("OmniFocus database is inaccessible")
            .hasMessageEndingWith("NOTHING");
    }
}
