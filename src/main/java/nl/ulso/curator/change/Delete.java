package nl.ulso.curator.change;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.stream.Stream;

import static nl.ulso.curator.change.Change.Kind.DELETE;

record Delete<T>(T oldValue, Class<T> payloadType)
    implements Change<T>
{
    private static final Logger LOGGER = LoggerFactory.getLogger(Delete.class);

    @Override
    public T value()
    {
        return oldValue;
    }

    @Override
    public Stream<T> values()
    {
        return Stream.of(oldValue);
    }

    @Override
    public T newValue()
    {
        var error = new UnsupportedOperationException("DELETE does not have a new value.");
        LOGGER.error(error.getMessage(), error);
        throw error;
    }

    @Override
    public Kind kind()
    {
        return DELETE;
    }
}
