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
    void createObject()
    {
        var change = create(42, Integer.class);
        assertThat(change.newObject()).isEqualTo(42);
    }

    @Test
    void updateKind()
    {
        var change = update(42, Integer.class);
        assertThat(change.kind()).isSameAs(UPDATE);
    }

    @Test
    void updateWithSingleObject()
    {
        var change = update(42, Integer.class);
        assertThat(change.object()).isEqualTo(42);
    }

    @Test
    void updateWithOldObject()
    {
        var change = update(0, 42, Integer.class);
        assertThat(change.oldObject()).isEqualTo(0);
    }

    @Test
    void updateWithNewObject()
    {
        var change = update(0, 42, Integer.class);
        assertThat(change.newObject()).isEqualTo(42);
    }

    @Test
    void deleteKind()
    {
        var change = delete(42, Integer.class);
        assertThat(change.kind()).isSameAs(DELETE);
    }

    @Test
    void deleteObject()
    {
        var change = delete(42, Integer.class);
        assertThat(change.oldObject()).isEqualTo(42);
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
        var integer = change.as(Integer.class).object();
        assertThat(integer).isEqualTo(42);
    }

    @Test
    void asObjectInvalid()
    {
        var change = create(42, Integer.class);
        assertThatThrownBy(() -> change.as(String.class).object())
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void updateWithSingleObjectHasNoOldObject()
    {
        var change = update(42, Integer.class);
        assertThatThrownBy(change::oldObject)
            .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void updateWithSingleObjectHasNewObject()
    {
        var change = update(42, Integer.class);
        var integer = change.as(Integer.class).newObject();
        assertThat(integer).isEqualTo(42);
    }

    @Test
    void updateWithOldAndNewObjectsReturnsNewObject()
    {
        var change = update(0, 42, Integer.class);
        var integer = change.as(Integer.class).newObject();
        assertThat(integer).isEqualTo(42);
    }

    @Test
    void createHasNoOldObject()
    {
        var change = create(42, Integer.class);
        assertThatThrownBy(change::oldObject)
            .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void deleteHasNoNewObject()
    {
        var change = delete(42, Integer.class);
        assertThatThrownBy(change::newObject)
            .isInstanceOf(UnsupportedOperationException.class);
    }
}
