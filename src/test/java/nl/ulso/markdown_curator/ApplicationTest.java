package nl.ulso.markdown_curator;

import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static java.lang.System.getProperty;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SoftAssertionsExtension.class)
class ApplicationTest
{
    @Test
    void testVersion()
    {
        var application = new Application(null);
        assertThat(application.resolveVersion()).isNotEqualTo(Application.UNKNOWN_VERSION);
    }

    @Test
    void newPidFileReturnsTrue()
    {
        var path = tempPidPath();
        var application = new Application(path);
        try
        {
            assertThat(application.ensureNewPidFile()).isTrue();
        }
        finally
        {
            path.toFile().delete();
        }
    }

    @Test
    void existingPidFileReturnsTrue()
            throws IOException
    {
        Path path = tempPidPath();
        path.toFile().createNewFile();
        var application = new Application(path);
        try
        {
            assertThat(application.ensureNewPidFile()).isFalse();
        }
        finally
        {
            path.toFile().delete();
        }
    }

    @Test
    void runCuratorOnce()
    {
        var module = new MusicCuratorModule();
        assertThat(module.isConfigured()).isFalse();
        new Application(null).runCuratorsInSeparateThreads(List.of(module),
                Application.RunMode.ONCE);
        assertThat(module.isConfigured()).isTrue();
    }

    private Path tempPidPath()
    {
        return Path.of(getProperty("java.io.tmpdir"), "markdown-curator-temp.pid");
    }
}
