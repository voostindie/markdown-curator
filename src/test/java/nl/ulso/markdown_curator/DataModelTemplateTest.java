package nl.ulso.markdown_curator;

import nl.ulso.markdown_curator.vault.event.VaultChangedEvent;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static nl.ulso.markdown_curator.vault.event.VaultChangedEvent.*;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SoftAssertionsExtension.class)
class DataModelTemplateTest
{
    @ParameterizedTest
    @MethodSource("provideEvents")
    void processFullRefresh(VaultChangedEvent event)
    {
        var model = new DummyDataModel();
        assertThat(model.refreshed).isFalse();
        model.vaultChanged(event);
        assertThat(model.refreshed).isTrue();
    }

    public static Stream<Arguments> provideEvents()
    {
        return Stream.of(
                Arguments.of(vaultRefreshed()),
                Arguments.of(folderAdded(null)),
                Arguments.of(folderRemoved(null)),
                Arguments.of(documentAdded(null)),
                Arguments.of(documentChanged(null)),
                Arguments.of(documentRemoved(null))
        );
    }


    private static class DummyDataModel
            extends DataModelTemplate
    {
        private boolean refreshed = false;

        @Override
        protected void fullRefresh()
        {
            refreshed = true;
        }
    }
}
