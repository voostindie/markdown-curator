package nl.ulso.curator.addon.journal;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import nl.ulso.curator.change.EntityTransformer;
import nl.ulso.curator.vault.Document;

import java.util.Optional;

/// Creates markers from documents in a special subfolder of the journal folder.
@Singleton
final class MarkerProducer
    extends EntityTransformer<Document, Marker>
{
    private final String[] markerPath;

    @Inject
    MarkerProducer(JournalSettings settings)
    {
        this.markerPath = new String[] {
            settings.journalFolderName(),
            settings.markerSubFolderName()
        };
    }

    @Override
    protected Class<Document> sourceClass()
    {
        return Document.class;
    }

    @Override
    protected Class<Marker> targetClass()
    {
        return Marker.class;
    }

    @Override
    protected Optional<Marker> transform(Document document)
    {
        if (!isMarkerDocument(document))
        {
            return Optional.empty();
        }
        return Optional.of(new Marker(document));
    }

    boolean isMarkerDocument(Document document)
    {
        return document.isInPath(markerPath);
    }
}
