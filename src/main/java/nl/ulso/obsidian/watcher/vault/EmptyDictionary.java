package nl.ulso.obsidian.watcher.vault;

import java.util.*;

class EmptyDictionary
        implements Dictionary
{

    static Dictionary INSTANCE = new EmptyDictionary();

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
