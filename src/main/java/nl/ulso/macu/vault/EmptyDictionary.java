package nl.ulso.macu.vault;

import java.util.*;

final class EmptyDictionary
        implements nl.ulso.macu.vault.Dictionary
{
    static Dictionary INSTANCE = new EmptyDictionary();

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
    public Date date(String property, Date defaultDate)
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
    public List<Date> listOfDates(String property)
    {
        return Collections.emptyList();
    }
}
