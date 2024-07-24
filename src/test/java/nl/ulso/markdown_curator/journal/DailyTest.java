package nl.ulso.markdown_curator.journal;

import nl.ulso.markdown_curator.vault.Section;
import nl.ulso.markdown_curator.vault.VaultStub;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.LocalDate;
import java.util.Set;
import java.util.stream.Stream;

import static nl.ulso.markdown_curator.journal.Daily.parseDailiesFrom;
import static nl.ulso.markdown_curator.journal.JournalBuilder.parseDateFrom;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SoftAssertionsExtension.class)
class DailyTest
{
    @Test
    void emptySection()
    {
        var vault = new VaultStub();
        var document = vault.addDocument("2023-01-27", "## Activities");
        var section = (Section) document.fragments().get(1);
        var entry = parseDailiesFrom(section);
        assertThat(entry).map(Daily::date).map(LocalDate::toString).hasValue("2023-01-27");
    }

    @Test
    void documentNameIsNotADate()
    {
        var vault = new VaultStub();
        var document = vault.addDocument("Not a date", "## Activities");
        var date = parseDateFrom(document);
        assertThat(date).isNotPresent();
    }

    @ParameterizedTest
    @MethodSource("provideDocumentNames")
    void entryRefersToDocument(String documentName, boolean hasReference)
    {
        var vault = new VaultStub();
        var document = vault.addDocument("2023-01-27", """
                ## Activities
                
                - [[X]]
                    - Y
                    - Z
                - [[foo]]
                    - [[bar]]
                        - baz
                """);
        var section = (Section) document.fragments().get(1);
        var daily = parseDailiesFrom(section);
        assertThat(daily).map(e -> e.refersTo(documentName)).hasValue(hasReference);
    }

    public static Stream<Arguments> provideDocumentNames()
    {
        return Stream.of(
                Arguments.of("X", true),
                Arguments.of("Y", false),
                Arguments.of("Z", false),
                Arguments.of("foo", true),
                Arguments.of("bar", true),
                Arguments.of("baz", false)
        );
    }

    @Test
    void summary()
    {
        var vault = new VaultStub();
        var document = vault.addDocument("2023-01-27", """
                ## Activities
                
                - Top item
                    - Reference to [[X]]
                        - And a child for [[X]]
                    - Reference to [[Y]]
                        - And a child for Y
                        - Which will not show up in the summary
                    - Reference to [[Z]]
                        - And another reference to [[X]]
                        - And another reference to [[Y]]
                """);
        var section = (Section) document.fragments().get(1);
        var daily = parseDailiesFrom(section);
        assertThat(daily).map(e -> e.summaryFor("X")).hasValue("""
                - Top item
                    - Reference to [[X]]
                        - And a child for [[X]]
                    - Reference to [[Z]]
                        - And another reference to [[X]]
                """);
    }

    @Test
    void referencedDocuments()
    {
        var vault = new VaultStub();
        var document = vault.addDocument("2023-03-18", """
                ## Activities
                
                - [[X]], [[Y]] and [[Z]]
                - [[foo]]
                    - [[bar]]
                        - [[baz]]
                """);
        var section = (Section) document.fragments().get(1);
        var daily = parseDailiesFrom(section);
        assertThat(daily).map(Daily::referencedDocuments).contains(
                Set.of("X", "Y", "Z", "foo", "bar", "baz"));
    }
}
