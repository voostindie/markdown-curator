package nl.ulso.obsidian.watcher.vault;

import java.util.Date;
import java.util.List;

import static nl.ulso.obsidian.watcher.vault.Dictionary.emptyDictionary;
import static nl.ulso.obsidian.watcher.vault.Dictionary.yamlDictionary;

public final class FrontMatter
        extends LineContainer
        implements Fragment, Dictionary
{
    private final Dictionary dictionary;

    public FrontMatter(List<String> lines)
    {
        super(lines);
        if (lines.isEmpty())
        {
            dictionary = emptyDictionary();
        }
        else
        {
            dictionary = yamlDictionary(lines);
        }
    }

    @Override
    public void accept(Visitor visitor)
    {
        visitor.visitFrontMatter(this);
    }

    public Dictionary dictionary()
    {
        return dictionary;
    }

    @Override
    public boolean isEmpty()
    {
        return dictionary.isEmpty();
    }

    @Override
    public String string(String property, String defaultValue)
    {
        return dictionary.string(property, defaultValue);
    }

    @Override
    public int integer(String property, int defaultValue)
    {
        return dictionary.integer(property, defaultValue);
    }

    @Override
    public Date date(String property, Date defaultDate)
    {
        return dictionary.date(property, defaultDate);
    }

    @Override
    public List<String> listOfStrings(String property)
    {
        return dictionary.listOfStrings(property);
    }

    @Override
    public List<Integer> listOfIntegers(String property)
    {
        return dictionary.listOfIntegers(property);
    }

    @Override
    public List<Date> listOfDates(String property)
    {
        return dictionary.listOfDates(property);
    }
}
