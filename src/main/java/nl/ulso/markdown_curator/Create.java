package nl.ulso.markdown_curator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static nl.ulso.markdown_curator.Change.Kind.CREATE;

record Create<T>(T newObject, Class<T> objectType)
    implements Change<T>
{
    private static final Logger LOGGER = LoggerFactory.getLogger(Create.class);

    @Override
    public T object()
    {
        return newObject;
    }

    @Override
    public T oldObject()
    {
        var error =
            new UnsupportedOperationException("CREATE does not have an old object.");
        LOGGER.error(error.getMessage(), error);
        throw error;
    }

    @Override
    public Kind kind()
    {
        return CREATE;
    }
}
