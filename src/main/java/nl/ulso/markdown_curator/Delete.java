package nl.ulso.markdown_curator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static nl.ulso.markdown_curator.Change.Kind.DELETE;

record Delete<T>(T oldObject, Class<T> objectType)
    implements Change<T>
{
    private static final Logger LOGGER = LoggerFactory.getLogger(Delete.class);

    @Override
    public T object()
    {
        return oldObject;
    }

    @Override
    public T newObject()
    {
        var error =
            new UnsupportedOperationException("DELETE does not have a new object.");
        LOGGER.error(error.getMessage(), error);
        throw error;
    }

    @Override
    public Kind kind()
    {
        return DELETE;
    }
}
