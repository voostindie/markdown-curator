package nl.ulso.jxa;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JavaScriptForAutomationFromClasspathTest
{
    @Test
    @EnabledOnOs({OS.MAC})
    void helloWorldNoArgs()
    {
        var output = new JavaScriptForAutomationFromClasspath().runScriptForObject("hello");
        var message = output.getString("message");
        assertThat(message).isEqualTo("Hello, world");
    }

    @Test
    @EnabledOnOs({OS.MAC})
    void helloWorldWithArgs()
    {
        var output = new JavaScriptForAutomationFromClasspath().runScriptForObject(
            "hello", "Vincent");
        var message = output.getString("message");
        assertThat(message).isEqualTo("Hello, Vincent");
    }

    @Test
    @EnabledOnOs({OS.MAC})
    void nonExistingScript()
    {
        assertThatThrownBy(
            () -> new JavaScriptForAutomationFromClasspath().runScriptForArray("non-existing")
        ).isInstanceOf(IllegalStateException.class);
    }
}
