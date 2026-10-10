package nl.ulso.curator.query;

import jakarta.inject.Inject;

import static jakarta.json.Json.createArrayBuilder;
import static jakarta.json.Json.createObjectBuilder;
import static jakarta.json.Json.createValue;
import static nl.ulso.json.JsonToString.jsonToString;

final class FolderResultJsonFormatter
    extends JsonFormatter
    implements QueryResultFormatter<FolderResult>
{
    @Inject
    FolderResultJsonFormatter()
    {
    }

    @Override
    public Class<FolderResult> queryResultType()
    {
        return FolderResult.class;
    }

    @Override
    public String format(FolderResult result)
    {
        var documentListBuilder = createArrayBuilder();
        result.folder().documents().forEach(document ->
        {
            var documentBuilder = createObjectBuilder();
            documentBuilder.add("name", document.name());
            documentBuilder.add(
                "path",
                result.documentPathResolver().resolveAbsolutePath(document).toString()
            );
            documentBuilder.add("last_modified", createValue(document.lastModified()));
            documentBuilder.add("properties", jsonObjectFor(document.frontMatter()));
            documentListBuilder.add(documentBuilder);
        });
        var folderListBuilder = createArrayBuilder();
        result.folder().folders().forEach(folder -> folderListBuilder.add(folder.name()));
        var objectBuilder = createObjectBuilder()
            .add("documents", documentListBuilder)
            .add("folders", folderListBuilder)
            .build();
        return jsonToString(objectBuilder);
    }
}
