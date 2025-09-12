package nl.ulso.markdown_curator.vault;

public interface MutableDictionary
        extends Dictionary
{
    void removeProperty(String property);

    void setProperty(String property, Object value);
}
