package nl.ulso.markdown_curator;

import nl.ulso.markdown_curator.vault.*;

import java.io.StringWriter;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static java.util.stream.Collectors.toMap;

/**
 * Writes a Markdown version of a Document in memory, with new query outputs. Everything but the
 * query output should be written as is; there should be no changes with the file on disk.
 * <p/>
 * Two things to note:
 * <ul>
 *     <li>Every query in the document MUST be provided with new output, otherwise this is
 *     an error.</li>
 *     <li>An end-of-file newline is ALWAYS written.</li>
 * </ul>
 */
final class DocumentWriter
        extends BreadthFirstVaultVisitor
{
    private final StringWriter out;
    private final Map<QueryBlock, QueryOutput> queryOutputs;

    public DocumentWriter(List<QueryOutput> queryOutputs)
    {
        this.queryOutputs = queryOutputs.stream().collect(
                toMap(QueryOutput::queryBlock, Function.identity()));
        this.out = new StringWriter();
    }

    @Override
    public void visit(Section section)
    {
        out.write(section.markdown());
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
        out.write(codeBlock.markdown());
    }

    @Override
    public void visit(QueryBlock queryBlock)
    {
        var queryOutput = queryOutputs.get(queryBlock);
        if (queryOutput == null)
        {
            throw new IllegalStateException("Missing output for query block");
        }
        out.write(queryBlock.markdown(queryOutput.content(), queryOutput.hash()));
    }

    @Override
    public void visit(TextBlock textBlock)
    {
        out.write(textBlock.markdown());
    }

    static String writeUpdatedDocument(Document document, List<QueryOutput> queryOutputs)
    {
        var writer = new DocumentWriter(queryOutputs);
        document.accept(writer);
        return writer.out.toString();
    }
}
