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
class MapBasedEntityRepositoryTest
{
    @InjectSoftAssertions
    private SoftAssertions softly;

    @Test
    void emptyRepository()
    {
        var repository = new MapBasedDummyRepository();
        assertThat(repository.map()).isEmpty();
    }

    @Test
    void consumedPayloadTypes()
    {
        var repository = new MapBasedDummyRepository();
        assertThat(repository.consumedPayloadTypes()).containsExactlyInAnyOrder(Dummy.class);
    }

    @Test
    void producedPayloadTypes()
    {
        var repository = new MapBasedDummyRepository();
        assertThat(repository.producedPayloadTypes()).containsExactly(
            MapBasedDummyRepository.class);
    }

    @Test
    void navigableMap()
    {
        var repository = new MapBasedDummyRepository();
        assertThrowsExactly(ClassCastException.class, repository::navigableMap);
    }

    @Test
    void sortedMap()
    {
        var repository = new MapBasedDummyRepository();
        assertThrowsExactly(ClassCastException.class, repository::sortedMap);
    }

    @Test
    void sequencedMap()
    {
        var repository = new MapBasedDummyRepository();
        assertThrowsExactly(ClassCastException.class, repository::sequencedMap);
    }

    @Test
    void reset()
    {
        var repository = new MapBasedDummyRepository(new Dummy("Initial"));
        repository.reset();
        assertThat(repository.map()).isEmpty();
    }

    @Test
    void measure()
    {
        var repository = new MapBasedDummyRepository(new Dummy("Initial"));
        var collector = new MeasurementCollectorStub();
        repository.collectMeasurements(collector);
        assertThat(collector.totalFor("change", "dummy")).isEqualTo(1);
    }

    @Test
    void testCreate()
    {
        var repository = new MapBasedDummyRepository();
        var changelog = repository.apply(changelogFor(create(new Dummy("New"), Dummy.class)));
        softly.assertThat(changelog.changes().findFirst().orElseThrow().payloadType())
            .isEqualTo(MapBasedDummyRepository.class);
        softly.assertThat(repository.entities().size()).isEqualTo(1);
    }

    @Test
    void testUpdate()
    {
        var dummy = new Dummy("Update");
        var repository = new MapBasedDummyRepository(dummy);
        var changelog = repository.apply(changelogFor(update(dummy, dummy, Dummy.class)));
        softly.assertThat(changelog.changes().findFirst().orElseThrow().payloadType())
            .isEqualTo(MapBasedDummyRepository.class);
        softly.assertThat(repository.entities().size()).isEqualTo(1);
    }

    @Test
    void testDelete()
    {
        var dummy = new Dummy("Delete");
        var repository = new MapBasedDummyRepository(dummy);
        var changelog = repository.apply(changelogFor(delete(dummy, Dummy.class)));
        softly.assertThat(changelog.changes().findFirst().orElseThrow().payloadType())
            .isEqualTo(MapBasedDummyRepository.class);
        softly.assertThat(repository.entities().size()).isZero();
    }
}
