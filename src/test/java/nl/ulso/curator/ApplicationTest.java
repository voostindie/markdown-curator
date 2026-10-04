package nl.ulso.curator;

import nl.ulso.curator.main.MusicCuratorFactory;
import nl.ulso.curator.server.CuratorStub;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.*;

import static nl.ulso.curator.RunMode.DAEMON;
import static nl.ulso.curator.RunMode.ONCE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@ExtendWith(SoftAssertionsExtension.class)
class ApplicationTest
{
    @AfterEach
    void resetRunMode()
    {
        RunMode.set(ONCE);
    }

    @Test
    void testVersion()
    {
        var application = new Application(null, false);
        assertThat(application.resolveVersion()).isNotEqualTo("<UNKNOWN>");
    }

    @Test
    void mainWithOnceAndServerOption()
    {
        assertDoesNotThrow(() ->
            Application.main(new String[]{"--once", "--server", "music"})
        );
        assertThat(RunMode.get()).isEqualTo(ONCE);
    }

    @Test
    void mainWithShortOptions()
    {
        assertDoesNotThrow(() ->
            Application.main(new String[]{"-1", "-s", "MUSIC"})
        );
        assertThat(RunMode.get()).isEqualTo(ONCE);
    }

    @Test
    void mainWithServerInDaemonModeAndUnknownCurator()
    {
        assertDoesNotThrow(() ->
            Application.main(new String[]{"--server", "unknown"})
        );
        assertThat(RunMode.get()).isEqualTo(DAEMON);
    }

    @Test
    void mainWithShortServerInDaemonModeAndUnknownCurator()
    {
        assertDoesNotThrow(() ->
            Application.main(new String[]{"-s", "unknown"})
        );
        assertThat(RunMode.get()).isEqualTo(DAEMON);
    }

    @Test
    void mainWithPortOption()
    {
        assertDoesNotThrow(() ->
            Application.main(new String[]{"--server:8080", "unknown"})
        );
        assertThat(RunMode.get()).isEqualTo(DAEMON);
    }

    @Test
    void mainWithInvalidPortBelow1024()
    {
        assertDoesNotThrow(() ->
            Application.main(new String[]{"--server:80", "unknown"})
        );
        assertThat(RunMode.get()).isEqualTo(DAEMON);
    }

    @Test
    void mainWithInvalidPortNotANumber()
    {
        assertDoesNotThrow(() ->
            Application.main(new String[]{"--server:invalid", "unknown"})
        );
        assertThat(RunMode.get()).isEqualTo(DAEMON);
    }

    @Test
    void runWithoutMatchingCuratorFilter()
    {
        assertDoesNotThrow(() -> {
            var application = new Application(new StubPidManager(false, true), false);
            application.run(Set.of("nonexistent"));
        });
    }

    @Test
    void runWithExistingPidAborts()
    {
        assertDoesNotThrow(() -> {
            var application = new Application(new StubPidManager(true, true), false);
            application.run(Collections.emptySet());
        });
    }

    @Test
    void runWithHttpServerInOnceMode()
    {
        RunMode.set(ONCE);
        assertDoesNotThrow(() -> {
            var application = new Application(new StubPidManager(false, true), true);
            application.run(Set.of("music"));
        });
    }

    @Test
    void createCuratorsSuccess()
    {
        var factory = new MusicCuratorFactory();
        var application = new Application(new StubPidManager(false, true), false);
        var curators = application.createCurators(List.of(factory));
        assertThat(curators).containsKey("music");
    }

    @Test
    void createCuratorsPidCreationFails()
    {
        var factory = new MusicCuratorFactory();
        var application = new Application(new StubPidManager(false, false), false);
        var curators = application.createCurators(List.of(factory));
        assertThat(curators).isEmpty();
    }

    @Test
    void createCuratorsFactoryThrowsException()
    {
        var factory = new ThrowingCuratorFactory();
        var application = new Application(new StubPidManager(false, true), false);
        var curators = application.createCurators(List.of(factory));
        assertThat(curators).isEmpty();
    }

    @Test
    void createCuratorsInterrupted()
    {
        var factory = new MusicCuratorFactory();
        var application = new Application(new StubPidManager(false, true), false);
        Thread.currentThread().interrupt();
        application.createCurators(List.of(factory));
        assertThat(Thread.interrupted()).isTrue();
    }

    @Test
    void runCuratorsInParallelSuccess()
    {
        var stub = new CuratorStub();
        var application = new Application(null, false);
        assertDoesNotThrow(() ->
            application.runCuratorsInParallel(Map.of("test", stub))
        );
    }

    @Test
    void runCuratorsInParallelExceptionHandled()
    {
        var stub = new ThrowingCurator();
        var application = new Application(null, false);
        assertDoesNotThrow(() ->
            application.runCuratorsInParallel(Map.of("test", stub))
        );
    }

    @Test
    void runCuratorsInParallelInterrupted()
    {
        var stub = new CuratorStub();
        var application = new Application(null, false);
        Thread.currentThread().interrupt();
        assertDoesNotThrow(() ->
            application.runCuratorsInParallel(Map.of("test", stub))
        );
        assertThat(Thread.interrupted()).isTrue();
    }

    @Test
    void runEmbeddedHttpServer()
    {
        var application = new Application(null, false);
        assertDoesNotThrow(() ->
            application.runEmbeddedHttpServer(Map.of("test", new CuratorStub()))
        );
    }

    private static class StubPidManager implements PidManager
    {
        private final boolean anyExists;
        private final boolean createSuccess;

        StubPidManager(boolean anyExists, boolean createSuccess)
        {
            this.anyExists = anyExists;
            this.createSuccess = createSuccess;
        }

        @Override
        public boolean anyPidExists(Collection<CuratorFactory> curatorFactories)
        {
            return anyExists;
        }

        @Override
        public boolean createPidFor(CuratorFactory curatorFactory)
        {
            return createSuccess;
        }
    }

    private static class ThrowingCuratorFactory implements CuratorFactory
    {
        @Override
        public String name()
        {
            return "throwing";
        }

        @Override
        public Curator createCurator()
        {
            throw new RuntimeException("Creation failed intentionally");
        }
    }

    private static class ThrowingCurator extends CuratorStub
    {
        @Override
        public void run(RunMode runMode)
        {
            throw new RuntimeException("Run failed intentionally");
        }
    }
}
