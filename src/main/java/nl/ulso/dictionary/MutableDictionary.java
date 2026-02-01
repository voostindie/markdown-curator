package nl.ulso.dictionary;

public interface MutableDictionary
        extends Dictionary
{
    void removeProperty(String property);

    void setProperty(String property, Object value);
}
