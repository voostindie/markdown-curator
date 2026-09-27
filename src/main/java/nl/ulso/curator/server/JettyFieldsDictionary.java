package nl.ulso.curator.server;

import nl.ulso.dictionary.Dictionary;
import org.eclipse.jetty.util.Fields;

import java.time.LocalDate;
import java.util.*;

/// Wraps a Jetty [Fields] object as a [Dictionary].
final class JettyFieldsDictionary
    implements Dictionary
{
    private final Fields fields;

    JettyFieldsDictionary(Fields fields)
    {
        this.fields = fields;
    }

    @Override
    public Set<String> propertyNames()
    {
        return fields.getNames();
    }

    @Override
    public boolean isEmpty()
    {
        return fields.isEmpty();
    }

    @Override
    public Optional<Object> getProperty(String name)
    {
        return Optional.of(fields.getValuesOrEmpty(name));
    }

    @Override
    public String string(String property, String defaultValue)
    {
        var value = fields.getValue(property);
        if (value == null)
        {
            return defaultValue;
        }
        return value;
    }

    @Override
    public int integer(String property, int defaultValue)
    {
        var field = fields.get(property);
        if (field == null)
        {
            return defaultValue;
        }
        try
        {
            return field.getValueAsInt();
        }
        catch (NumberFormatException _)
        {
            return defaultValue;
        }
    }

    @Override
    public LocalDate date(String property, LocalDate defaultDate)
    {
        throw new UnsupportedOperationException("Not yet implemented!");
    }

    @Override
    public boolean bool(String property, boolean defaultValue)
    {
        var string = string(property, Boolean.toString(defaultValue));
        return Boolean.parseBoolean(string);
    }

    @Override
    public List<String> listOfStrings(String property)
    {
        throw new UnsupportedOperationException("Not yet implemented!");
    }

    @Override
    public List<Integer> listOfIntegers(String property)
    {
        throw new UnsupportedOperationException("Not yet implemented!");
    }

    @Override
    public List<LocalDate> listOfDates(String property)
    {
        throw new UnsupportedOperationException("Not yet implemented!");
    }

    @Override
    public boolean hasProperty(String property)
    {
        return fields.get(property) != null;
    }

    @Override
    public String toYamlString()
    {
        throw new UnsupportedOperationException("Not yet implemented!");
    }
}
