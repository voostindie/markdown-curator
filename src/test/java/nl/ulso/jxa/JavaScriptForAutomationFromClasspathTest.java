package nl.ulso.jxa;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JavaScriptForAutomationFromClasspathTest
{
    @Test
    @Disabled("Only works on macOS")
    void helloWorldNoArgs()
    {
        var output = new JavaScriptForAutomationFromClasspath().runScriptForObject("hello");
        var message = output.getString("message");
        assertThat(message).isEqualTo("Hello, world");
    }

    @Test
    @Disabled("Only works on macOS")
    void helloWorldWithArgs()
    {
        var output = new JavaScriptForAutomationFromClasspath().runScriptForObject(
            "hello", "Vincent");
        var message = output.getString("message");
        assertThat(message).isEqualTo("Hello, Vincent");
    }

    @Test
    @Disabled("Only works on macOS")
    void nonExistingScript()
    {
        assertThatThrownBy(
            () -> new JavaScriptForAutomationFromClasspath().runScriptForArray("non-existing")
        ).isInstanceOf(IllegalStateException.class);
    }
}
