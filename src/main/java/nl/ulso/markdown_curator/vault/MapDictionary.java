package nl.ulso.markdown_curator.vault;

import java.time.LocalDate;
import java.util.*;

import static java.util.Collections.emptyList;

/**
 * Dictionary implementation on top of a map. Even though the map may contain many different types
 * of values, the dictionary only supports scalars ({@link String}, {@link Integer},
 * {@link Boolean}, {@link LocalDate}) and lists of those.
 * <p/>
 * Trying to read a value that has a different type than then the type being requested results in
 * default values: either the value provided by the user (for scalars), or the empty lists (for
 * lists of scalars).
 * <p/>
 * In other words: this dictionary never barfs on type mismatches. It always returns exactly what
 * you expect.
 * <p/>
 * Lists and scalars are interchangeable in that:
 * <ul>
 *     <li>When reading a scalar from a list, you get the first item.</li>
 *     <li>When reading a list from a scalar, you get a one-element list with the scalar.</li>
 * </ul>
 */
class MapDictionary
        implements Dictionary
{
    private final Map<String, ?> map;

    MapDictionary(Map<String, ?> map)
    {
        this.map = Map.copyOf(map);
    }

    @Override
    public final boolean equals(Object o)
    {
        if (o instanceof MapDictionary dictionary)
        {
            return Objects.equals(map, dictionary.map);
        }
        return false;
    }

    @Override
    public final int hashCode()
    {
        return Objects.hash(map);
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
    public LocalDate date(String property, LocalDate defaultDate)
    {
        return safeGet(property, LocalDate.class, defaultDate);
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
        return safeGetList(property, LocalDate.class);
    }

    protected <T> T safeGet(String property, Class<? extends T> propertyClass, T defaultValue)
    {
        Object value = map.get(property);
        if (value == null)
        {
            return defaultValue;
        }
        if (value instanceof List<?> list)
        {
            if (list.isEmpty())
            {
                return defaultValue;
            }
            value = list.get(0);
        }
        if (!propertyClass.isInstance(value))
        {
            return defaultValue;
        }
        return propertyClass.cast(value);
    }

    @SuppressWarnings("unchecked")
    protected <T> List<T> safeGetList(String property, Class<? extends T> propertyClass)
    {
        Object value = map.get(property);
        if (value == null)
        {
            return emptyList();
        }
        if (!(value instanceof List<?>))
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
