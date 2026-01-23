package nl.ulso.markdown_curator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static nl.ulso.markdown_curator.Change.Kind.UPDATE;

record Update1<T>(T object, Class<T> objectType)
    implements Change<T>
{
    private static final Logger LOGGER = LoggerFactory.getLogger(Update1.class);

    @Override
    public T oldObject()
    {
        var error =
            new UnsupportedOperationException("UPDATE on a single object does not have an old object.");
        LOGGER.error(error.getMessage(), error);
        throw error;
    }

    @Override
    public T newObject()
    {
        return object;
    }

    @Override
    public Kind kind()
    {
        return UPDATE;
    }
}
