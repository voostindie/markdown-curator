package nl.ulso.curator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.stream.Stream;

import static nl.ulso.curator.Change.Kind.UPDATE;

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
    public Stream<T> values()
    {
        return Stream.of(value);
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
