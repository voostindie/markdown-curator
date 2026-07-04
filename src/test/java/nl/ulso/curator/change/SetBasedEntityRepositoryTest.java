package nl.ulso.curator.change;

import nl.ulso.curator.statistics.MeasurementCollectorStub;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static nl.ulso.curator.change.Change.create;
import static nl.ulso.curator.change.Change.delete;
import static nl.ulso.curator.change.Change.update;
import static nl.ulso.curator.change.Changelog.changelogFor;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;

@ExtendWith(SoftAssertionsExtension.class)
class SetBasedEntityRepositoryTest
{
    @InjectSoftAssertions
    private SoftAssertions softly;

    @Test
    void emptyRepository()
    {
        var repository = new SetBasedDummyRepository();
        assertThat(repository.set()).isEmpty();
    }

    @Test
    void consumedPayloadTypes()
    {
        var repository = new SetBasedDummyRepository();
        assertThat(repository.consumedPayloadTypes()).containsExactlyInAnyOrder(Dummy.class);
    }

    @Test
    void producedPayloadTypes()
    {
        var repository = new SetBasedDummyRepository();
        assertThat(repository.producedPayloadTypes()).containsExactly(
            SetBasedDummyRepository.class);
    }

    @Test
    void navigableSet()
    {
        var repository = new SetBasedDummyRepository();
        assertThrowsExactly(ClassCastException.class, repository::navigableSet);
    }

    @Test
    void sortedSet()
    {
        var repository = new SetBasedDummyRepository();
        assertThrowsExactly(ClassCastException.class, repository::sortedSet);
    }

    @Test
    void reset()
    {
        var repository = new SetBasedDummyRepository(new Dummy("Initial"));
        repository.reset();
        assertThat(repository.set()).isEmpty();
    }

    @Test
    void measure()
    {
        var repository = new SetBasedDummyRepository(new Dummy("Initial"));
        var collector = new MeasurementCollectorStub();
        repository.collectMeasurements(collector);
        assertThat(collector.totalFor("change", "dummy")).isEqualTo(1);
    }

    @Test
    void testCreate()
    {
        var repository = new SetBasedDummyRepository();
        var changelog = repository.apply(changelogFor(create(new Dummy("New"), Dummy.class)));
        softly.assertThat(changelog.changes().findFirst().orElseThrow().payloadType())
            .isEqualTo(SetBasedDummyRepository.class);
        softly.assertThat(repository.set().size()).isEqualTo(1);
    }

    @Test
    void testUpdate()
    {
        var dummy = new Dummy("Update");
        var repository = new SetBasedDummyRepository(dummy);
        var changelog = repository.apply(changelogFor(update(dummy, dummy, Dummy.class)));
        softly.assertThat(changelog.changes().findFirst().orElseThrow().payloadType())
            .isEqualTo(SetBasedDummyRepository.class);
        softly.assertThat(repository.set().size()).isEqualTo(1);
    }

    @Test
    void testDelete()
    {
        var dummy = new Dummy("Delete");
        var repository = new SetBasedDummyRepository(dummy);
        var changelog = repository.apply(changelogFor(delete(dummy, Dummy.class)));
        softly.assertThat(changelog.changes().findFirst().orElseThrow().payloadType())
            .isEqualTo(SetBasedDummyRepository.class);
        softly.assertThat(repository.set().size()).isZero();
    }
}
