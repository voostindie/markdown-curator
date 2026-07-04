package nl.ulso.curator.change;

import nl.ulso.curator.vault.Document;
import nl.ulso.curator.vault.Folder;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;
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
        assertThat(processor.didWork).isFalse();
        processor.apply(changelogFor(change));
        assertThat(processor.didWork).isTrue();
    }

    @Test
    void processorWithoutChangeHandlersThroes()
    {
        assertThatThrownBy(InvalidChangeProcessor::new)
            .isInstanceOf(IllegalStateException.class);
    }

    public static Stream<Arguments> provideChanges()
    {
        return Stream.of(
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
        private boolean didWork = false;

        @Override
        protected List<? extends ChangeHandler> createChangeHandlers()
        {
            return List.of(
                ChangeHandler.newChangeHandler(
                    (_) -> true, (_, _) -> didWork = true
                )
            );
        }

        @Override
        public Set<Class<?>> consumedPayloadTypes()
        {
            return Set.of(Folder.class, Document.class);
        }
    }

    private static class InvalidChangeProcessor
        extends ChangeProcessorTemplate
    {
        InvalidChangeProcessor()
        {
        }

        @Override
        protected List<? extends ChangeHandler> createChangeHandlers()
        {
            return emptyList();
        }

        @Override
        public Set<Class<?>> consumedPayloadTypes()
        {
            return Set.of(Document.class);
        }
    }
}
