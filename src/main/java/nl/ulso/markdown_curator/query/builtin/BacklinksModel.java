package nl.ulso.markdown_curator.query.builtin;

import nl.ulso.markdown_curator.DataModel;
import nl.ulso.markdown_curator.vault.*;

import java.util.*;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.groupingBy;

public final class BacklinksModel
        implements DataModel
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
    public void refreshOnVaultChange()
    {
        var finder = new InternalLinkFinder();
        vault.accept(finder);
        backlinks = finder.internalLinks().stream()
                .collect(groupingBy(InternalLink::targetDocument));
    }
}
