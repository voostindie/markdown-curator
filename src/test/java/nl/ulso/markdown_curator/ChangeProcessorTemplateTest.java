package nl.ulso.markdown_curator;

import nl.ulso.markdown_curator.vault.*;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Collection;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;
import static nl.ulso.markdown_curator.Changelog.changelogFor;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(SoftAssertionsExtension.class)
class ChangeProcessorTemplateTest
{
    @ParameterizedTest
    @MethodSource("provideChanges")
    void runFullRefresh(Change<?> change)
    {
        var processor = new DummyChangeProcessor();
        assertThat(processor.refreshed).isFalse();
        processor.run(changelogFor(change));
        assertThat(processor.refreshed).isTrue();
    }

    @Test
    void processorWithoutFullRefreshAndChangeHandlersDoesNothing()
    {
        var log = new NoOpChangeProcessor(false)
            .run(changelogFor(Change.create(null, Vault.class)));
        assertThat(log.isEmpty()).isTrue();
    }

    @Test
    void processorWithFullRefreshButNoImplementationThrows()
    {
        var processor = new NoOpChangeProcessor(true);
        var changelog = changelogFor(Change.create(null, Vault.class));
        assertThatThrownBy(() -> processor.run(changelog))
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
        public Collection<Change<?>> fullRefresh()
        {
            refreshed = true;
            return emptyList();
        }

        @Override
        protected boolean isFullRefreshRequired(Changelog changelog)
        {
            return true;
        }
    }

    private static class NoOpChangeProcessor
        extends ChangeProcessorTemplate
    {
        private final boolean fullRefresh;

        NoOpChangeProcessor(boolean fullRefresh)
        {
            this.fullRefresh = fullRefresh;
        }

        @Override
        protected boolean isFullRefreshRequired(Changelog changelog)
        {
            return fullRefresh;
        }
    }
}
