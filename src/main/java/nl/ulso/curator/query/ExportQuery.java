package nl.ulso.curator.query;

import jakarta.inject.Inject;
import jakarta.inject.Provider;
import nl.ulso.curator.change.Changelog;
import nl.ulso.curator.vault.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringWriter;
import java.util.Map;

import static java.lang.System.lineSeparator;
import static nl.ulso.curator.query.OutputFormat.MARKDOWN;
import static nl.ulso.emoji.EmojiStripper.stripEmojisFrom;

/// Exports a document from the vault to vanilla Markdown.
///
/// Queries in the content are executed on demand and always output as Markdown.
final class ExportQuery
    implements Query
{
    private static final Logger LOGGER = LoggerFactory.getLogger(ExportQuery.class);

    private final Vault vault;
    private final Provider<QueryCatalog> queryCatalogProvider;
    private final QueryResultFormatterRegistry formatterRegistry;
    private final QueryResultFactory resultFactory;

    @Inject
    ExportQuery(
        Vault vault,
        Provider<QueryCatalog> queryCatalogProvider,
        QueryResultFormatterRegistry formatterRegistry,
        QueryResultFactory resultFactory)
    {
        this.vault = vault;
        this.queryCatalogProvider = queryCatalogProvider;
        this.formatterRegistry = formatterRegistry;
        this.resultFactory = resultFactory;
    }

    ExportQuery(
        Vault vault,
        QueryCatalog queryCatalog,
        QueryResultFormatterRegistry formatterRegistry,
        QueryResultFactory resultFactory)
    {
        this(vault, () -> queryCatalog, formatterRegistry, resultFactory);
    }

    @Override
    public String name()
    {
        return "export";
    }

    @Override
    public String description()
    {
        return "Exports a document to vanilla Markdown.";
    }

    @Override
    public Map<String, String> supportedConfiguration()
    {
        return Map.of(
            "document", "Name of the document to export.",
            "strip-emojis-from-links", "Whether to remove emojis from internal links (default: true).",
            "add-title", "Whether to add the document title as a level 1 header (default: true)."
        );
    }

    @Override
    public boolean isImpactedBy(Changelog changelog, QueryDefinition definition)
    {
        if (LOGGER.isWarnEnabled())
        {
            LOGGER.warn(
                "{} is not intended to be embedded in documents directly; " +
                "it is meant for the embedded HTTP server only.",
                this.getClass().getSimpleName()
            );
        }
        return false;
    }

    @Override
    public QueryResult run(QueryDefinition definition)
    {
        var documentName = definition.configuration().string("document", null);
        if (documentName == null || documentName.isBlank())
        {
            return resultFactory.error("No document specified in query configuration.");
        }
        var stripEmojis = definition.configuration().bool("strip-emojis-from-links", true);
        var addTitle = definition.configuration().bool("add-title", true);
        return vault.findDocument(documentName).map(document -> {
            var visitor = new ExportVisitor(
                queryCatalogProvider.get(),
                formatterRegistry,
                stripEmojis
            );
            document.accept(visitor);
            var output = visitor.output();
            if (addTitle && !output.startsWith("# "))
            {
                output = "# " + document.title() + lineSeparator() + lineSeparator() + output;
            }
            return resultFactory.string(output);

        }).orElseGet(() ->
            resultFactory.error("Document not found: " + documentName)
        );
    }

    private static class ExportVisitor
        extends BreadthFirstVaultVisitor
    {
        private final QueryCatalog queryCatalog;
        private final QueryResultFormatterRegistry formatterRegistry;
        private final boolean stripEmojisFromLinks;
        private final StringWriter out;

        ExportVisitor(
            QueryCatalog queryCatalog,
            QueryResultFormatterRegistry formatterRegistry,
            boolean stripEmojisFromLinks)
        {
            this.queryCatalog = queryCatalog;
            this.formatterRegistry = formatterRegistry;
            this.stripEmojisFromLinks = stripEmojisFromLinks;
            this.out = new StringWriter();
        }

        @Override
        public void visit(FrontMatter frontMatter)
        {
            // Front matter is omitted in vanilla Markdown export.
        }

        @Override
        public void visit(Section section)
        {
            out.write(replaceWikilinks(section.toMarkdown()));
            super.visit(section);
        }

        @Override
        public void visit(CodeBlock codeBlock)
        {
            out.write(codeBlock.toMarkdown());
        }

        @Override
        public void visit(QueryBlock queryBlock)
        {
            var query = queryCatalog.query(queryBlock.queryName());
            final QueryResult result;
            try
            {
                result = query.run(queryBlock);
            }
            catch (RuntimeException e)
            {
                LOGGER.warn(
                    "Ignoring output due to exception while running query '{}' in document '{}'.",
                    query.name(),
                    queryBlock.document() != null ? queryBlock.document().name() : "unknown",
                    e
                );
                return;
            }
            var output = formatterRegistry.format(result, MARKDOWN);
            if (!output.isEmpty() && !output.endsWith(lineSeparator()))
            {
                output += lineSeparator();
            }
            out.write(replaceWikilinks(output));
        }

        @Override
        public void visit(TextBlock textBlock)
        {
            out.write(replaceWikilinks(textBlock.markdown()));
        }

        private String replaceWikilinks(String text)
        {
            if (text == null || text.isEmpty())
            {
                return text;
            }
            var builder = new StringBuilder();
            var index = 0;
            while (true)
            {
                var start = text.indexOf("[[", index);
                if (start == -1)
                {
                    builder.append(text.substring(index));
                    break;
                }
                var end = text.indexOf("]]", start + 2);
                if (end == -1)
                {
                    builder.append(text.substring(index));
                    break;
                }
                builder.append(text, index, start);
                var link = text.substring(start + 2, end);
                builder.append(convertWikilink(link, stripEmojisFromLinks));
                index = end + 2;
            }
            return builder.toString();
        }

        private static String convertWikilink(String rawLink, boolean stripEmojis)
        {
            var link = rawLink;
            String alias = null;
            var pipeIndex = link.indexOf('|');
            if (pipeIndex != -1)
            {
                alias = link.substring(pipeIndex + 1);
                link = link.substring(0, pipeIndex);
            }
            String anchor = null;
            var hashIndex = link.indexOf('#');
            if (hashIndex != -1)
            {
                anchor = link.substring(hashIndex + 1);
                link = link.substring(0, hashIndex);
            }
            if (stripEmojis)
            {
                if (alias != null)
                {
                    alias = stripEmojisFrom(alias).trim();
                }
                if (anchor != null)
                {
                    anchor = stripEmojisFrom(anchor).trim();
                }
                link = stripEmojisFrom(link).trim();
            }
            if (alias != null)
            {
                return alias;
            }
            if (anchor != null)
            {
                if (!link.isEmpty())
                {
                    return link + " > " + anchor;
                }
                return anchor;
            }
            return link;
        }

        String output()
        {
            return out.toString();
        }
    }
}
