package nl.ulso.curator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.stream.Stream;

import static nl.ulso.curator.Change.Kind.CREATE;

record Create<T>(T newValue, Class<T> payloadType)
    implements Change<T>
{
    private static final Logger LOGGER = LoggerFactory.getLogger(Create.class);

    @Override
    public T value()
    {
        return newValue;
    }

    @Override
    public Stream<T> values()
    {
        return Stream.of(newValue);
    }

    @Override
    public T oldValue()
    {
        var error = new UnsupportedOperationException("CREATE does not have an old value.");
        LOGGER.error(error.getMessage(), error);
        throw error;
    }

    @Override
    public Kind kind()
    {
        return CREATE;
    }
}
