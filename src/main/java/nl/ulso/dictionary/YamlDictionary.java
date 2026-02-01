package nl.ulso.dictionary;

import nl.ulso.date.LocalDates;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snakeyaml.engine.v2.api.Load;
import org.snakeyaml.engine.v2.api.LoadSettings;
import org.snakeyaml.engine.v2.exceptions.YamlEngineException;

import java.time.LocalDate;
import java.util.*;

import static java.lang.String.join;
import static java.lang.System.lineSeparator;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.toMap;

/// Extremely lenient implementation of a dictionary on top of YAML.
///
/// Extremely lenient means:
///
/// - The input lines need to be a single document
/// - If the YAML is invalid (not a map), the dictionary is empty.
/// - Reading a value that has a different type results in the default value.
/// - Reading a single value from a list results in the first value.
/// - Reading a list from a single value results in a list with one item.
///
/// Dates are supported only in one format: "yyyy-MM-dd"
final class YamlDictionary
        extends MapDictionary
{
    private static final Logger LOGGER = LoggerFactory.getLogger(YamlDictionary.class);
    private static final String DOCUMENT_SEPARATOR = "---";

    private final Map<String, List<LocalDate>> dateCache;

    YamlDictionary(String string)
    {
        super(parseYaml(string));
        dateCache = new HashMap<>();
    }

    YamlDictionary(List<String> lines)
    {
        this(join(lineSeparator(), singleYamlNode(lines)));
    }

    private static Map<String, Object> parseYaml(String string)
    {
        LoadSettings settings = LoadSettings.builder().build();
        Load load = new Load(settings);
        Map<String, ?> yaml = null;
        try
        {
            yaml = loadYaml(string, load);
        }
        catch (YamlEngineException | ClassCastException _)
        {
            LOGGER.warn("Invalid YAML found; ignoring it: {}", string);
        }
        if (yaml == null)
        {
            return emptyMap();
        }
        return yaml.entrySet().stream()
                .filter(e -> e.getValue() != null)
                .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> loadYaml(String string, Load load)
    {
        return (Map<String, Object>) load.loadFromString(string);
    }

    private static List<String> singleYamlNode(List<String> lines)
    {
        int from = 0;
        int to = lines.size();
        if (to > 0 && lines.getFirst().contentEquals(DOCUMENT_SEPARATOR))
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
    public LocalDate date(String property, LocalDate defaultValue)
    {
        var dates = listOfDates(property);
        if (dates.isEmpty())
        {
            return defaultValue;
        }
        return dates.getFirst();
    }

    @Override
    public List<LocalDate> listOfDates(String property)
    {
        if (dateCache.containsKey(property))
        {
            return dateCache.get(property);
        }
        var dates = safeGetList(property, String.class).stream()
                .map(LocalDates::parseDateOrNull)
                .filter(Objects::nonNull)
                .toList();
        dateCache.put(property, dates.isEmpty() ? emptyList() : dates);
        return listOfDates(property);
    }
}
