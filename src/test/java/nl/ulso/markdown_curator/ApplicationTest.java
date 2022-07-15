package nl.ulso.markdown_curator;

import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.file.Path;
import java.util.List;
import java.util.ServiceLoader.Provider;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static java.lang.System.getProperty;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

@ExtendWith(SoftAssertionsExtension.class)
class ApplicationTest
{
    @InjectSoftAssertions
    private SoftAssertions softly;

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
    void oneThreadPerCurator()
    {
        List<CuratorModule> modules = List.of(new MusicCuratorModule());
        var executor = new Application(null).runCuratorsInSeparateThreads(modules);
        softly.assertThat(executor).isInstanceOf(ThreadPoolExecutor.class);
        var threadPoolExecutor = (ThreadPoolExecutor) executor;
        softly.assertThat(threadPoolExecutor.getActiveCount()).isEqualTo(modules.size());
    }

    private Path tempPidPath()
    {
        return Path.of(getProperty("java.io.tmpdir"), "markdown-curator-temp.pid");
    }
}
