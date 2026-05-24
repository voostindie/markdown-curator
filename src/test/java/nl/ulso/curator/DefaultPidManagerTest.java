package nl.ulso.curator;

import com.google.common.jimfs.Jimfs;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.nio.file.*;
import java.util.Set;
import java.util.UUID;

import static java.lang.System.lineSeparator;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SoftAssertionsExtension.class)
class DefaultPidManagerTest
{
    private DefaultPidManager pidManager;
    private FileSystem fileSystem;

    @BeforeEach
    void setUp()
        throws IOException
    {
        this.fileSystem = Jimfs.newFileSystem(UUID.randomUUID().toString());
        Files.createDirectories(fileSystem.getPath(System.getProperty("java.io.tmpdir")));
        this.pidManager = new DefaultPidManager(fileSystem, false);
    }

    @Test
    void newCurators()
    {
        var result = pidManager.anyPidExists(Set.of(cf("stub1"), cf("stub2")));
        assertThat(result).isFalse();
    }

    @Test
    void existingCurators()
        throws IOException
    {
        Files.writeString(pidPath("stub2"), "42" + lineSeparator());
        var result = pidManager.anyPidExists(Set.of(cf("stub1"), cf("stub2")));
        assertThat(result).isTrue();
    }

    @Test
    void createPidForCurator()
    {
        pidManager.createPidFor(cf("stub1"));
        var path = pidPath("stub1");
        assertThat(Files.exists(path)).isTrue();
    }

    @Test
    void onlyLowerCaseAndLetersAndDigits()
    {
        pidManager.createPidFor(cf("FOO{⚠️}_BAR_42!"));
        assertThat(Files.exists(pidPath("foobar42"))).isTrue();
    }

    private Path pidPath(String curatorName)
    {
        return fileSystem.getPath(
            System.getProperty("java.io.tmpdir"),
            "markdown-curator-" + curatorName + ".pid"
        );
    }

    private CuratorFactory cf(String name)
    {
        return new DummyCuratorFactory(name);
    }

    private static final class DummyCuratorFactory
        implements CuratorFactory
    {
        private final String name;

        private DummyCuratorFactory(String name)
        {
            this.name = name;
        }

        @Override
        public String name()
        {
            return name;
        }

        @Override
        public Curator createCurator()
        {
            throw new UnsupportedOperationException();
        }
    }
}
