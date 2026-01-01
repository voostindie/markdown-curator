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
class DataModelTemplateTest
{
    @ParameterizedTest
    @MethodSource("provideEvents")
    void processFullRefresh(Change<?> change)
    {
        var model = new DummyDataModel();
        assertThat(model.refreshed).isFalse();
        model.process(changelogFor(change));
        assertThat(model.refreshed).isTrue();
    }

    public static Stream<Arguments> provideEvents()
    {
        return Stream.of(
            Arguments.of(Change.modification(null, Vault.class)),
            Arguments.of(Change.creation(null, Folder.class)),
            Arguments.of(Change.deletion(null, Folder.class)),
            Arguments.of(Change.creation(null, Document.class)),
            Arguments.of(Change.modification(null, Document.class)),
            Arguments.of(Change.deletion(null, Document.class))
        );
    }

    private static class DummyDataModel
        extends DataModelTemplate
    {
        private boolean refreshed = false;

        @Override
        public Collection<Change<?>> fullRefresh()
        {
            refreshed = true;
            return emptyList();
        }
    }
}
