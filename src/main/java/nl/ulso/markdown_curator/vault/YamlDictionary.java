package nl.ulso.markdown_curator.vault;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snakeyaml.engine.v2.api.Load;
import org.snakeyaml.engine.v2.api.LoadSettings;
import org.snakeyaml.engine.v2.exceptions.YamlEngineException;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;

import static java.lang.String.join;
import static java.lang.System.lineSeparator;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Objects.requireNonNullElse;

/**
 * Extremely lenient implementation of a dictionary on top of YAML.
 * <p>
 * Extremely lenient means:
 * <ul>
 *     <li>The input lines need to be a single document</li>
 *     <li>If the YAML is invalid (not a map), the dictionary is empty.</li>
 *     <li>Reading a value that has a different type results in the default value.</li>
 *     <li>Reading a single value from a list results in the first value.</li>
 *     <li>Reading a list from a single value results in a list with one item.</li>
 * </ul>
 * Dates are supported only in one format: "yyyy-MM-dd"
 */
final class YamlDictionary
        implements Dictionary
{
    private static final Logger LOGGER = LoggerFactory.getLogger(YamlDictionary.class);
    private static final String DOCUMENT_SEPARATOR = "---";

    private final Map<String, ?> map;
    private final Map<String, List<LocalDate>> dateCache;

    YamlDictionary(String string)
    {
        dateCache = new HashMap<>();
        LoadSettings settings = LoadSettings.builder().build();
        Load load = new Load(settings);
        Map<String, Object> yaml = null;
        try
        {
            //noinspection unchecked
            yaml = (Map<String, Object>) load.loadFromString(string);
        }
        catch (YamlEngineException | ClassCastException e)
        {
            LOGGER.warn("Invalid YAML found; ignoring it");
        }
        map = requireNonNullElse(yaml, emptyMap());
    }

    YamlDictionary(List<String> lines)
    {
        this(join(lineSeparator(), singleYamlNode(lines)));
    }

    @Override
    public boolean equals(Object o)
    {
        if (o instanceof YamlDictionary dictionary)
        {
            return Objects.equals(map, dictionary.map);
        }
        return false;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(map);
    }

    private static List<String> singleYamlNode(List<String> lines)
    {
        int from = 0;
        int to = lines.size();
        if (to > 0 && lines.get(0).contentEquals(DOCUMENT_SEPARATOR))
        {
            from = 1;
        }
        if (to > 2 && lines.get(to - 1).contentEquals(DOCUMENT_SEPARATOR))
        {
            to--;
        }
        return lines.subList(from, to);
    }

    @Override
    public boolean isEmpty()
    {
        return map.isEmpty();
    }

    @Override
    public String string(String property, String defaultValue)
    {
        return safeGet(property, String.class, defaultValue);
    }

    @Override
    public int integer(String property, int defaultValue)
    {
        return safeGet(property, Integer.class, defaultValue);
    }

    @Override
    public LocalDate date(String property, LocalDate defaultValue)
    {
        var dates = listOfDates(property);
        if (dates.isEmpty())
        {
            return defaultValue;
        }
        return dates.get(0);
    }

    @Override
    public boolean bool(String property, boolean defaultValue)
    {
        return safeGet(property, Boolean.class, defaultValue);
    }

    @Override
    public List<String> listOfStrings(String property)
    {
        return safeGetList(property, String.class);
    }

    @Override
    public List<Integer> listOfIntegers(String property)
    {
        return safeGetList(property, Integer.class);
    }

    @Override
    public List<LocalDate> listOfDates(String property)
    {
        if (dateCache.containsKey(property))
        {
            return dateCache.get(property);
        }
        var dates = safeGetList(property, String.class).stream().map(string -> {
            try
            {
                return LocalDate.parse(string);
            }
            catch (DateTimeParseException e)
            {
                return null;
            }
        }).filter(Objects::nonNull).toList();
        dateCache.put(property, dates.isEmpty() ? emptyList() : dates);
        return listOfDates(property);
    }

    private <T> T safeGet(String property, Class<? extends T> propertyClass, T defaultValue)
    {
        var list = safeGetList(property, propertyClass);
        if (list.isEmpty())
        {
            return defaultValue;
        }
        return propertyClass.cast(list.get(0));
    }

    @SuppressWarnings("unchecked")
    private <T> List<T> safeGetList(String property, Class<? extends T> propertyClass)
    {
        Object value = map.get(property);
        if (value == null)
        {
            return emptyList();
        }
        if (!(value instanceof List))
        {
            value = List.of(value);
        }
        List<?> list = (List<?>) value;
        if (list.isEmpty())
        {
            return emptyList();
        }
        var first = list.get(0);
        if (!propertyClass.isInstance(first))
        {
            return emptyList();
        }
        return (List<T>) list;
    }
}
