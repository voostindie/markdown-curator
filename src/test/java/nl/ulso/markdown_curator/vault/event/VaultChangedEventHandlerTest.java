package nl.ulso.markdown_curator.vault.event;

import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static nl.ulso.markdown_curator.vault.event.VaultChangedEvent.*;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SoftAssertionsExtension.class)
class VaultChangedEventHandlerTest
{
    @InjectSoftAssertions
    private SoftAssertions softly;

    @ParameterizedTest
    @MethodSource("provideEvents")
    void processVaultRefreshed(VaultChangedEvent event, String action)
    {
        var recorder = new EventRecorder();
        event.dispatch(recorder);
        assertThat(recorder.action).isEqualTo(action);
    }

    public static Stream<Arguments> provideEvents()
    {
        return Stream.of(
                Arguments.of(vaultRefreshed(), "vault refreshed"),
                Arguments.of(folderAdded(null), "folder added"),
                Arguments.of(folderRemoved(null), "folder removed"),
                Arguments.of(documentAdded(null), "document added"),
                Arguments.of(documentChanged(null), "document changed"),
                Arguments.of(documentRemoved(null), "document removed")
        );
    }

    private static class EventRecorder
            implements VaultChangedEventHandler
    {
        private String action;

        @Override
        public void process(VaultRefreshed event)
        {
            action = "vault refreshed";
        }

        @Override
        public void process(FolderAdded event)
        {
            action = "folder added";
        }

        @Override
        public void process(FolderRemoved event)
        {
            action = "folder removed";
        }

        @Override
        public void process(DocumentAdded event)
        {
            action = "document added";
        }

        @Override
        public void process(DocumentChanged event)
        {
            action = "document changed";
        }

        @Override
        public void process(DocumentRemoved event)
        {
            action = "document removed";
        }
    }
}
