package nl.ulso.curator;

import nl.ulso.curator.main.MusicCuratorFactory;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static java.lang.System.getProperty;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

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
        assertDoesNotThrow(() -> {
            var factory = new MusicCuratorFactory();
            new Application(null).runCuratorsInSeparateThreads(List.of(factory),
                    Application.RunMode.ONCE);
        });
    }

    private Path tempPidPath()
    {
        return Path.of(getProperty("java.io.tmpdir"), "markdown-curator-temp.pid");
    }
}
