package nl.ulso.curator.change;

import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.ArrayList;
import java.util.List;

import static nl.ulso.curator.change.Change.create;
import static nl.ulso.curator.change.Change.delete;
import static nl.ulso.curator.change.Change.update;
import static nl.ulso.curator.change.Changelog.changelogFor;

@ExtendWith(SoftAssertionsExtension.class)
class EntityProcessorTest
{
    @InjectSoftAssertions
    private SoftAssertions softly;

    @Test
    void integerLog()
    {
        var processor = new EntityProcessorStub();
        processor.apply(changelogFor(
            create(42, Integer.class),
            update(49, 50, Integer.class),
            delete(50, Integer.class),
            create(0, Integer.class),
            create(1, Integer.class),
            create(2, Integer.class),
            delete(2, Integer.class),
            update(1, 2, Integer.class)
        ));
        softly.assertThat(processor.created).containsExactly(42, 0, 1, 2);
        softly.assertThat(processor.updated).containsExactly(49, 50, 1, 2);
        softly.assertThat(processor.deleted).containsExactly(50, 2);
    }

    private static class EntityProcessorStub
        extends EntityProcessor<Integer>
    {
        private final List<Integer> created = new ArrayList<>();
        private final List<Integer> updated = new ArrayList<>();
        private final List<Integer> deleted = new ArrayList<>();

        @Override
        protected Class<Integer> entityClass()
        {
            return Integer.class;
        }

        @Override
        protected void entityCreated(Integer newInteger, ChangeCollector collector)
        {
            created.add(newInteger);
        }

        @Override
        protected void entityUpdated(
            Integer oldInteger, Integer newInteger, ChangeCollector collector)
        {
            updated.add(oldInteger);
            updated.add(newInteger);
        }

        @Override
        protected void entityDeleted(Integer oldInteger, ChangeCollector collector)
        {
            deleted.add(oldInteger);
        }
    }
}