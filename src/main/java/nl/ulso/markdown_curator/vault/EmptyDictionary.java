package nl.ulso.markdown_curator.vault;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

final class EmptyDictionary
        implements Dictionary
{
    static final Dictionary INSTANCE = new EmptyDictionary();

    private EmptyDictionary()
    {
    }

    @Override
    public boolean equals(Object o)
    {
        return o instanceof EmptyDictionary;
    }

    @Override
    public int hashCode()
    {
        return 31;
    }

    @Override
    public boolean isEmpty()
    {
        return true;
    }

    @Override
    public String string(String property, String defaultValue)
    {
        return defaultValue;
    }

    @Override
    public int integer(String property, int defaultValue)
    {
        return defaultValue;
    }

    @Override
    public LocalDate date(String property, LocalDate defaultDate)
    {
        return defaultDate;
    }

    @Override
    public boolean bool(String property, boolean defaultValue)
    {
        return defaultValue;
    }

    @Override
    public List<String> listOfStrings(String property)
    {
        return Collections.emptyList();
    }

    @Override
    public List<Integer> listOfIntegers(String property)
    {
        return Collections.emptyList();
    }

    @Override
    public List<LocalDate> listOfDates(String property)
    {
        return Collections.emptyList();
    }

    @Override
    public boolean hasProperty(String property)
    {
        return false;
    }
}
