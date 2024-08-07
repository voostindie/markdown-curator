package nl.ulso.markdown_curator;

import nl.ulso.markdown_curator.vault.*;

import java.io.StringWriter;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static java.util.stream.Collectors.toMap;

/**
 * Writes a Markdown version of a Document, with new query outputs. Everything but the query output
 * is written as is; there are no other changes compared to the file on disk, with one minor
 * exception; see below.
 * <p/>
 * Two things to note:
 * <ul>
 *     <li>Every query in the document MUST be provided with new output, otherwise this is
 *     a programming error.</li>
 *     <li>A newline is ALWAYS written at the end of the file, even if the source document didn't
 *     have one.</li>
 * </ul>
 */
final class DocumentRewriter
        extends BreadthFirstVaultVisitor
{
    private final StringWriter out;
    private final Map<QueryBlock, QueryOutput> queryOutputs;

    public DocumentRewriter(List<QueryOutput> queryOutputs)
    {
        this.queryOutputs = queryOutputs.stream()
                .collect(toMap(QueryOutput::queryBlock, Function.identity()));
        this.out = new StringWriter();
    }

    @Override
    public void visit(Section section)
    {
        out.write(section.toMarkdown());
        super.visit(section);
    }

    @Override
    public void visit(FrontMatter frontMatter)
    {
        out.write(frontMatter.markdown());
    }

    @Override
    public void visit(CodeBlock codeBlock)
    {
        out.write(codeBlock.toMarkdown());
    }

    @Override
    public void visit(QueryBlock queryBlock)
    {
        var queryOutput = queryOutputs.get(queryBlock);
        if (queryOutput == null)
        {
            throw new IllegalStateException("Missing output for query block");
        }
        out.write(queryBlock.toMarkdown(queryOutput.content(), queryOutput.hash()));
    }

    @Override
    public void visit(TextBlock textBlock)
    {
        out.write(textBlock.markdown());
    }

    static String rewriteDocument(Document document, List<QueryOutput> queryOutputs)
    {
        var writer = new DocumentRewriter(queryOutputs);
        document.accept(writer);
        return writer.out.toString();
    }
}
