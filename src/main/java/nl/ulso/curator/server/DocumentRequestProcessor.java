package nl.ulso.curator.server;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.json.*;
import nl.ulso.curator.Curator;
import nl.ulso.curator.query.OutputFormat;
import nl.ulso.curator.vault.*;
import nl.ulso.dictionary.Dictionary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringWriter;
import java.util.List;
import java.util.Map;

import static jakarta.json.Json.createArrayBuilder;
import static jakarta.json.Json.createObjectBuilder;
import static jakarta.json.Json.createValue;
import static nl.ulso.curator.query.OutputFormat.JSON;
import static nl.ulso.curator.query.OutputFormat.MARKDOWN;
import static nl.ulso.curator.server.Result.clientError;
import static nl.ulso.curator.server.Result.ok;
import static nl.ulso.json.JsonToString.jsonToString;

/// Outputs the content of a document as it lives in memory. Most notably: output from queries is
/// not present!
///
/// This processor supports both Markdown and JSON output. The JSON output is probably more useful,
/// at it outputs the document as an array of document fragments, where each fragment can be of a
/// different type.
@Singleton
final class DocumentRequestProcessor
    extends CuratorRequestProcessorTemplate
{
    private static final Logger LOGGER = LoggerFactory.getLogger(DocumentRequestProcessor.class);

    @Inject
    DocumentRequestProcessor(Map<String, Curator> curators)
    {
        super(curators);
    }

    @Override
    protected String subPathTemplate()
    {
        return "/document";
    }

    @Override
    protected Result process(
        Curator curator,
        Dictionary pathParameters,
        Dictionary queryParameters,
        OutputFormat outputFormat)
    {
        var documentName = queryParameters.string("name", null);
        if (documentName == null)
        {
            return clientError("Document name is missing in the query parameters.");
        }
        LOGGER.info("Retrieving document '{}'.", documentName);
        return curator.vault().findDocument(documentName)
            .map(document ->
            {
                var writer = outputFormat.equals(MARKDOWN) ? new MarkdownWriter()
                                                           : new JsonWriter();
                document.accept(writer);
                return ok(writer.outputFormat(), writer.asString());
            })
            .orElseGet(() -> Result.notFound("Document not found: " + documentName));
    }

    interface DocumentWriter
    {
        String asString();
        OutputFormat outputFormat();
    }

    private static class MarkdownWriter
        extends BreadthFirstVaultVisitor
        implements DocumentWriter
    {
        private final StringWriter out = new StringWriter();

        @Override
        public void visit(FrontMatter frontMatter)
        {
            out.write(frontMatter.markdown());
        }

        @Override
        public void visit(Section section)
        {
            out.write(section.toMarkdown());
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
            out.write(queryBlock.toMarkdown("", ""));
        }

        @Override
        public void visit(TextBlock textBlock)
        {
            out.write(textBlock.markdown());
        }

        @Override
        public String asString()
        {
            return out.toString();
        }

        @Override
        public OutputFormat outputFormat()
        {
            return MARKDOWN;
        }
    }

    private static class JsonWriter
        extends BreadthFirstVaultVisitor
        implements DocumentWriter
    {
        private static final String CONTENTS = "contents";

        private JsonObject rootObject;
        private JsonArrayBuilder arrayBuilder;

        @Override
        public void visit(Document document)
        {
            arrayBuilder = createArrayBuilder();
            var builder = createObjectBuilder();
            builder.add("name", document.name());
            builder.add("title", document.title());
            builder.add("sortable-title", document.sortableTitle());
            builder.add("last-modified", document.lastModified());
            super.visit(document);
            builder.add(CONTENTS, arrayBuilder);
            rootObject = builder.build();
        }

        @Override
        public void visit(FrontMatter frontMatter)
        {
            var builder = createObjectBuilder();
            builder.add("type", "frontmatter");
            builder.add("configuration", toJson(frontMatter));
            arrayBuilder.add(builder);
        }

        @Override
        public void visit(Section section)
        {
            var builder = createObjectBuilder();
            builder.add("type", "section");
            builder.add("title", section.title());
            builder.add("sortable-title", section.sortableTitle());
            builder.add("level", section.level());
            arrayBuilder.add(builder);
            super.visit(section);
        }

        @Override
        public void visit(TextBlock textBlock)
        {
            var builder = createObjectBuilder();
            builder.add("type", "text");
            builder.add(CONTENTS, textBlock.markdown());
            arrayBuilder.add(builder);
        }

        @Override
        public void visit(CodeBlock codeBlock)
        {
            var builder = createObjectBuilder();
            builder.add("type", "code");
            builder.add("language", codeBlock.language());
            builder.add(CONTENTS, codeBlock.code());
            arrayBuilder.add(builder);
        }

        @Override
        public void visit(QueryBlock queryBlock)
        {
            var builder = createObjectBuilder();
            builder.add("type", "query");
            builder.add("name", queryBlock.queryName());
            builder.add("configuration", toJson(queryBlock.configuration()));
            arrayBuilder.add(builder);
        }

        private JsonObjectBuilder toJson(Dictionary dictionary)
        {
            var builder = createObjectBuilder();
            for (var propertyName : dictionary.propertyNames())
            {
                var value = dictionary.getProperty(propertyName).orElseThrow();
                if (value instanceof List<?> values)
                {
                    var listBuilder = createArrayBuilder();
                    values.forEach(listValue -> listBuilder.add(createValue(listValue.toString())));
                    builder.add(propertyName, listBuilder);
                }
                else
                {
                    builder.add(propertyName, createValue(value.toString()));
                }
            }
            return builder;
        }

        @Override
        public String asString()
        {
            return jsonToString(rootObject);
        }

        @Override
        public OutputFormat outputFormat()
        {
            return JSON;
        }
    }
}
