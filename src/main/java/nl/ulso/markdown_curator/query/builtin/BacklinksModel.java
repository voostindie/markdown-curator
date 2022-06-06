package nl.ulso.markdown_curator.query.builtin;

import nl.ulso.markdown_curator.DataModelTemplate;
import nl.ulso.markdown_curator.vault.*;

import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.groupingBy;

public final class BacklinksModel
        extends DataModelTemplate
{
    private final Vault vault;
    private Map<String, List<InternalLink>> backlinks;

    public BacklinksModel(Vault vault)
    {
        this.vault = vault;
        this.backlinks = null;
    }

    List<InternalLink> linksFor(String documentName)
    {
        return backlinks.computeIfAbsent(documentName, key -> emptyList());
    }

    @Override
    protected void fullRefresh()
    {
        var finder = new InternalLinkFinder();
        vault.accept(finder);
        backlinks = finder.internalLinks().stream()
                .collect(groupingBy(InternalLink::targetDocument));
    }
}
