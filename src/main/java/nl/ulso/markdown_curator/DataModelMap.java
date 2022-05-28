package nl.ulso.markdown_curator;

import java.util.Collection;
import java.util.Map;

/**
 * A map of all custom {@link DataModel}s used by a single curator.
 * <p/>
 * This map is used to implement a simple, crude form of dependency injection. I prefer this over
 * having to add a complete depedency injection framework, however small.
 */
public class DataModelMap
{
    private final Map<Class<? extends DataModel>, DataModel> dataModels;

    public DataModelMap(Map<Class<? extends DataModel>, DataModel> dataModels)
    {
        this.dataModels = Map.copyOf(dataModels);
    }

    public <M extends DataModel> M get(Class<M> modelClass)
    {
        var model = dataModels.get(modelClass);
        if (model == null)
        {
            throw new IllegalStateException("Expected data model of class " + modelClass.getName());
        }
        return modelClass.cast(model);
    }

    Collection<DataModel> models()
    {
        return dataModels.values();
    }
}
