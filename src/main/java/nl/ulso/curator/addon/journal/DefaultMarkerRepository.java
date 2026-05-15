package nl.ulso.curator.addon.journal;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import nl.ulso.curator.change.MapBasedEntityRepository;
import nl.ulso.curator.vault.Document;
import nl.ulso.dictionary.Dictionary;

import java.util.Map;

import static nl.ulso.dictionary.Dictionary.emptyDictionary;

@Singleton
final class DefaultMarkerRepository
    extends MapBasedEntityRepository<Document, String, Marker>
    implements MarkerRepository
{
    private final String[] markerPath;

    @Inject
    DefaultMarkerRepository(JournalSettings settings)
    {
        this.markerPath = new String[] {
            settings.journalFolderName(),
            settings.markerSubFolderName()
        };
    }

    @Override
    protected Class<Document> sourceEntityClass()
    {
        return Document.class;
    }

    @Override
    protected Class<Marker> targetEntityClass()
    {
        return Marker.class;
    }

    @Override
    protected boolean isEntity(Document document)
    {
        return document.isInPath(markerPath);
    }

    @Override
    protected String entityKeyFrom(Document document)
    {
        return document.name();
    }

    @Override
    protected Marker createEntityFrom(String name, Document document)
    {
        return new Marker(document);
    }

    @Override
    public Map<String, Marker> markers()
    {
        return map();
    }

    @Override
    public Dictionary markerSettings(String markerName)
    {
        var marker = map().get(markerName);
        return marker != null ? marker.settings() : emptyDictionary();
    }

    @Override
    public boolean isMarkerDocument(Document document)
    {
        return isEntity(document);
    }

    @Override
    public String name()
    {
        return MarkerRepository.class.getSimpleName();
    }
}
