package nl.ulso.markdown_curator.vault.event;

import nl.ulso.markdown_curator.vault.*;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static nl.ulso.markdown_curator.vault.event.VaultChangedEvent.*;

@ExtendWith(SoftAssertionsExtension.class)
class VaultChangedEventHandlerTest
{
    @InjectSoftAssertions
    private SoftAssertions softly;

    @ParameterizedTest
    @MethodSource("provideEvents")
    void processVaultRefreshed(VaultChangedEvent event, String action, Class<?> expectedObjectClass)
    {
        var recorder = new EventRecorder();
        event.dispatch(recorder);
        softly.assertThat(recorder.action).isEqualTo(action);
        if (expectedObjectClass != null)
        {
            softly.assertThat(recorder.object).isNotNull();
            softly.assertThat(recorder.object).isInstanceOf(expectedObjectClass);
        }
    }

    public static Stream<Arguments> provideEvents()
    {
        var vault = new VaultStub();
        vault.addDocumentInPath("foo/bar", "Dummy!");
        var folder = vault.folder("foo").orElseThrow();
        var document = folder.document("bar").orElseThrow();
        return Stream.of(
                Arguments.of(vaultRefreshed(), "vault refreshed", null),
                Arguments.of(folderAdded(folder), "folder added", Folder.class),
                Arguments.of(folderRemoved(folder), "folder removed", Folder.class),
                Arguments.of(documentAdded(document), "document added", Document.class),
                Arguments.of(documentChanged(document), "document changed", Document.class),
                Arguments.of(documentRemoved(document), "document removed", Document.class)
        );
    }

    private static class EventRecorder
            implements VaultChangedEventHandler
    {
        private String action;
        private Object object;

        @Override
        public void process(VaultRefreshed event)
        {
            action = "vault refreshed";
            object = null;
        }

        @Override
        public void process(FolderAdded event)
        {
            action = "folder added";
            object = event.folder();
        }

        @Override
        public void process(FolderRemoved event)
        {
            action = "folder removed";
            object = event.folder();
        }

        @Override
        public void process(DocumentAdded event)
        {
            action = "document added";
            object = event.document();
        }

        @Override
        public void process(DocumentChanged event)
        {
            action = "document changed";
            object = event.document();
        }

        @Override
        public void process(DocumentRemoved event)
        {
            action = "document removed";
            object = event.document();
        }
    }
}
