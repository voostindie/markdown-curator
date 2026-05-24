package nl.ulso.curator;

import nl.ulso.curator.main.MusicCuratorFactory;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@ExtendWith(SoftAssertionsExtension.class)
class ApplicationTest
{
    @Test
    void testVersion()
    {
        var application = new Application(null);
        assertThat(application.resolveVersion()).isNotEqualTo("<UNKNOWN>");
    }

    @Test
    void runCuratorOnce()
    {
        assertDoesNotThrow(() -> {
            var factory = new MusicCuratorFactory();
            new Application(null).runCuratorsInSeparateThreads(List.of(factory));
        });
    }
}
