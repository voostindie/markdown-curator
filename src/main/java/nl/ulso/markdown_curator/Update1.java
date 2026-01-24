package nl.ulso.markdown_curator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static nl.ulso.markdown_curator.Change.Kind.UPDATE;

record Update1<T>(T value, Class<T> payloadType)
    implements Change<T>
{
    private static final Logger LOGGER = LoggerFactory.getLogger(Update1.class);

    @Override
    public T oldValue()
    {
        var error = new UnsupportedOperationException(
            "UPDATE on a single object does not have an old value.");
        LOGGER.error(error.getMessage(), error);
        throw error;
    }

    @Override
    public T newValue()
    {
        return value;
    }

    @Override
    public Kind kind()
    {
        return UPDATE;
    }
}
