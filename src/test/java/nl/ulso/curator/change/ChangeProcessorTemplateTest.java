package nl.ulso.curator.change;

import nl.ulso.curator.vault.*;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static nl.ulso.curator.change.Changelog.changelogFor;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(SoftAssertionsExtension.class)
class ChangeProcessorTemplateTest
{
    @ParameterizedTest
    @MethodSource("provideChanges")
    void runReset(Change<?> change)
    {
        var processor = new DummyChangeProcessor();
        assertThat(processor.refreshed).isFalse();
        processor.apply(changelogFor(change));
        assertThat(processor.refreshed).isTrue();
    }

    @Test
    void processorWithoutResetAndChangeHandlersDoesNothing()
    {
        var log = new NoOpChangeProcessor(false)
            .apply(changelogFor(Change.create(null, Vault.class)));
        assertThat(log.isEmpty()).isTrue();
    }

    @Test
    void processorWithResetButNoImplementationThrows()
    {
        var processor = new NoOpChangeProcessor(true);
        var changelog = changelogFor(Change.create(null, Vault.class));
        assertThatThrownBy(() -> processor.apply(changelog))
            .isInstanceOf(IllegalStateException.class);
    }

    public static Stream<Arguments> provideChanges()
    {
        return Stream.of(
            Arguments.of(Change.update(null, Vault.class)),
            Arguments.of(Change.create(null, Folder.class)),
            Arguments.of(Change.delete(null, Folder.class)),
            Arguments.of(Change.create(null, Document.class)),
            Arguments.of(Change.update(null, Document.class)),
            Arguments.of(Change.delete(null, Document.class))
        );
    }

    private static class DummyChangeProcessor
        extends ChangeProcessorTemplate
    {
        private boolean refreshed = false;

        @Override
        public void reset(ChangeCollector collector)
        {
            refreshed = true;
        }

        @Override
        protected boolean isResetRequired(Changelog changelog)
        {
            return true;
        }
    }

    private static class NoOpChangeProcessor
        extends ChangeProcessorTemplate
    {
        private final boolean doReset;

        NoOpChangeProcessor(boolean doReset)
        {
            this.doReset = doReset;
        }

        @Override
        protected boolean isResetRequired(Changelog changelog)
        {
            return doReset;
        }
    }
}
