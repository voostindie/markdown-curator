package nl.ulso.markdown_curator;

import org.junit.jupiter.api.Test;

import static nl.ulso.markdown_curator.Change.Kind.CREATE;
import static nl.ulso.markdown_curator.Change.Kind.DELETE;
import static nl.ulso.markdown_curator.Change.Kind.UPDATE;
import static nl.ulso.markdown_curator.Change.create;
import static nl.ulso.markdown_curator.Change.delete;
import static nl.ulso.markdown_curator.Change.update;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ChangeTest
{
    @Test
    void createKind()
    {
        var change = create(42, Integer.class);
        assertThat(change.kind()).isSameAs(CREATE);
    }

    @Test
    void updateKind()
    {
        var change = update(42, Integer.class);
        assertThat(change.kind()).isSameAs(UPDATE);
    }

    @Test
    void deleteKind()
    {
        var change = delete(42, Integer.class);
        assertThat(change.kind()).isSameAs(DELETE);
    }

    @Test
    void asChange()
    {
        Change<?> change1 = create(42, Integer.class);
        Change<Integer> change2 = change1.as(Integer.class);
        assertThat(change2).isNotNull();
    }

    @Test
    void asChangeInvalid()
    {
        Change<?> change1 = create(42, Integer.class);
        assertThatThrownBy(() -> change1.as(String.class))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void asObject()
    {
        var change = create(42, Integer.class);
        var integer = change.objectAs(Integer.class);
        assertThat(integer).isEqualTo(42);
    }

    @Test
    void asObjectInvalid()
    {
        var change = create(42, Integer.class);
        assertThatThrownBy(() -> change.objectAs(String.class))
            .isInstanceOf(IllegalStateException.class);
    }
}
