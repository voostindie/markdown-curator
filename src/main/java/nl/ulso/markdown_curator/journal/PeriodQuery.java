package nl.ulso.markdown_curator.journal;

import nl.ulso.markdown_curator.query.*;
import nl.ulso.markdown_curator.vault.*;

import jakarta.inject.Inject;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Stream;

import static java.util.Comparator.comparing;

public class PeriodQuery
        implements Query
{
    private final Journal journal;
    private final String defaultFolder;
    private final QueryResultFactory resultFactory;


    @Inject
    PeriodQuery(Journal journal, JournalSettings settings, QueryResultFactory resultFactory)
    {
        this.journal = journal;
        this.defaultFolder = settings.projectFolderName();
        this.resultFactory = resultFactory;
    }

    @Override
    public String name()
    {
        return "period";
    }

    @Override
    public String description()
    {
        return "Generates an overview of notes touched in a certain period, " +
               "extracted from the journal";
    }

    @Override
    public Map<String, String> supportedConfiguration()
    {
        return Map.of(
                "start", "First date to include in the period, in YYYY-MM-DD format",
                "end", "Last date to include in the period, in YYYY-MM-DD format",
                "folder", "Folder of notes to report on; defaults to '" + defaultFolder + "'"
        );
    }

    @Override
    public final QueryResult run(QueryDefinition definition)
    {
        var entries = resolveDailies(definition).toList();
        var documentNames = journal.referencedDocumentsIn(entries);
        var folder = definition.configuration().string("folder", defaultFolder);
        var finder = new DocumentFinder(documentNames, folder);
        journal.vault().accept(finder);
        return resultFactory.unorderedList(
                finder.selectedDocuments.stream()
                        .sorted(comparing(Document::sortableTitle))
                        .map(Document::link)
                        .toList());
    }

    protected Journal journal()
    {
        return journal;
    }

    protected Stream<Daily> resolveDailies(QueryDefinition definition)
    {
        var start = resolveStartDate(definition);
        var end = resolveEndDate(definition);
        if (start == null || end == null)
        {
            return Stream.empty();
        }
        return journal.entriesUntilIncluding(start, end);
    }

    protected LocalDate resolveStartDate(QueryDefinition definition)
    {
        return definition.configuration().date("start", null);
    }

    protected LocalDate resolveEndDate(QueryDefinition definition)
    {
        return definition.configuration().date("end", null);
    }

    protected String defaultFolder()
    {
        return defaultFolder;
    }

    private static class DocumentFinder
            extends BreadthFirstVaultVisitor
    {
        private final Set<String> documentNames;
        private final String selectedFolderName;
        private final Set<Document> selectedDocuments;

        DocumentFinder(Set<String> documentNames, String folderName)
        {
            this.documentNames = documentNames;
            this.selectedFolderName = folderName;
            selectedDocuments = new HashSet<>();
        }

        @Override
        public void visit(Vault vault)
        {
            vault.folder(selectedFolderName).ifPresent(folder -> folder.accept(this));
        }

        @Override
        public void visit(Document document)
        {
            if (documentNames.contains(document.name()))
            {
                selectedDocuments.add(document);
            }
        }
    }
}
