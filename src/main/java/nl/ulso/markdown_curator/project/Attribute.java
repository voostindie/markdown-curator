package nl.ulso.markdown_curator.project;

import nl.ulso.markdown_curator.vault.Document;

import java.time.LocalDate;

public interface Attribute<T>
{
    Attribute<LocalDate> LAST_MODIFIED = () -> LocalDate.class;
    Attribute<Document> LEAD = () -> Document.class;
    Attribute<Integer> PRIORITY = () -> Integer.class;
    Attribute<String> STATUS = () -> String.class;

    Class<T> valueType();
}
