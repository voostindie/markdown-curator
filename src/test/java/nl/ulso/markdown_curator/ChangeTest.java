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
    void createValue()
    {
        var change = create(42, Integer.class);
        assertThat(change.newValue()).isEqualTo(42);
    }

    @Test
    void updateKind()
    {
        var change = update(42, Integer.class);
        assertThat(change.kind()).isSameAs(UPDATE);
    }

    @Test
    void updateWithSingleValue()
    {
        var change = update(42, Integer.class);
        assertThat(change.value()).isEqualTo(42);
    }

    @Test
    void updateWithOldValue()
    {
        var change = update(0, 42, Integer.class);
        assertThat(change.oldValue()).isEqualTo(0);
    }

    @Test
    void updateWithNewValue()
    {
        var change = update(0, 42, Integer.class);
        assertThat(change.newValue()).isEqualTo(42);
    }

    @Test
    void deleteKind()
    {
        var change = delete(42, Integer.class);
        assertThat(change.kind()).isSameAs(DELETE);
    }

    @Test
    void deleteValue()
    {
        var change = delete(42, Integer.class);
        assertThat(change.oldValue()).isEqualTo(42);
    }

    @Test
    void asChange()
    {
        Change<?> change1 = create(42, Integer.class);
        Change<Integer> change2 = change1.as(Integer.class);
        assertThat(change2).isNotNull();
    }

    @Test
    void asValue()
    {
        var change = create(42, Integer.class);
        var integer = change.as(Integer.class).value();
        assertThat(integer).isEqualTo(42);
    }

    @Test
    void asInvalidValue()
    {
        var change = create(42, Integer.class);
        assertThatThrownBy(() -> change.as(String.class).value())
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void updateWithSingleValueHasNoOldValue()
    {
        var change = update(42, Integer.class);
        assertThatThrownBy(change::oldValue)
            .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void updateWithSingleValueHasNewValue()
    {
        var change = update(42, Integer.class);
        var integer = change.as(Integer.class).newValue();
        assertThat(integer).isEqualTo(42);
    }

    @Test
    void updateWithOldAndNewValuesReturnsNewValue()
    {
        var change = update(0, 42, Integer.class);
        var integer = change.as(Integer.class).newValue();
        assertThat(integer).isEqualTo(42);
    }

    @Test
    void createHasNoOldValue()
    {
        var change = create(42, Integer.class);
        assertThatThrownBy(change::oldValue)
            .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void deleteHasNoNewValue()
    {
        var change = delete(42, Integer.class);
        assertThatThrownBy(change::newValue)
            .isInstanceOf(UnsupportedOperationException.class);
    }
}
