package nl.ulso.curator.main;

import nl.ulso.curator.change.*;
import nl.ulso.curator.vault.Document;

import java.util.*;

import static nl.ulso.curator.change.Changelog.emptyChangelog;

/// Change processor that keeps track of the names of all documents in a specific folder, for
/// testing extensions.
final class DocumentRecorder
    implements ChangeProcessor
{
    private final String folderName;
    private final Set<String> documents = new TreeSet<>();
    private int resetCount = 0;

    DocumentRecorder(String folderName)
    {
        this.folderName = folderName;
    }

    @Override
    public Changelog apply(Changelog changelog)
    {
        changelog.changesFor(Document.class)
            .filter(change -> change.value().folder().name().equals(folderName))
            .forEach(change -> {
                if (change.kind() == Change.Kind.DELETE)
                {
                    documents.remove(change.value().name());
                }
                else
                {
                    documents.add(change.value().name());
                }
            });
        return emptyChangelog();
    }

    @Override
    public void reset()
    {
        resetCount++;
        documents.clear();
    }

    @Override
    public Set<Class<?>> consumedPayloadTypes()
    {
        return Set.of(Document.class);
    }

    @Override
    public String name()
    {
        return "DocumentRecorder-" + folderName;
    }

    Set<String> documents()
    {
        return documents;
    }

    int resetCount()
    {
        return resetCount;
    }
}
