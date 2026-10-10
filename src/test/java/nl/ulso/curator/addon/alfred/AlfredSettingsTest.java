package nl.ulso.curator.addon.alfred;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AlfredSettingsTest
{
    @Test
    void createSettings()
    {
        var settings = new AlfredSettings("Projects");
        assertThat(settings.projectFolder()).isEqualTo("Projects");
    }
}
