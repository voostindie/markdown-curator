package nl.ulso.markdown_curator;

import org.junit.jupiter.api.Test;

import static nl.ulso.markdown_curator.Change.Kind.CREATION;
import static nl.ulso.markdown_curator.Change.Kind.DELETION;
import static nl.ulso.markdown_curator.Change.Kind.MODIFICATION;
import static org.assertj.core.api.Assertions.assertThat;

class ChangeTest
{
    @Test
    void creationKind()
    {
        var change = Change.creation(42, Integer.class);
        assertThat(change.kind()).isSameAs(CREATION);
    }

    @Test
    void modificationKind()
    {
        var change = Change.modification(42, Integer.class);
        assertThat(change.kind()).isSameAs(MODIFICATION);
    }

    @Test
    void deletionKind()
    {
        var change = Change.deletion(42, Integer.class);
        assertThat(change.kind()).isSameAs(DELETION);
    }
}
