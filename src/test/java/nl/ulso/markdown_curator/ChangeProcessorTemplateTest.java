package nl.ulso.markdown_curator;

import nl.ulso.markdown_curator.vault.*;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Collection;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;
import static nl.ulso.markdown_curator.Changelog.changelogFor;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SoftAssertionsExtension.class)
class ChangeProcessorTemplateTest
{
    @ParameterizedTest
    @MethodSource("provideEvents")
    void runFullRefresh(Change<?> change)
    {
        var model = new DummyChangeProcessor();
        assertThat(model.refreshed).isFalse();
        model.run(changelogFor(change));
        assertThat(model.refreshed).isTrue();
    }

    public static Stream<Arguments> provideEvents()
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
}
