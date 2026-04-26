package nl.ulso.curator.change;

import nl.ulso.curator.statistics.MeasurementCollectorStub;
import nl.ulso.curator.vault.*;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static nl.ulso.curator.change.Change.create;
import static nl.ulso.curator.change.Change.delete;
import static nl.ulso.curator.change.Change.update;
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
        assertThat(repository.consumedPayloadTypes()).containsExactlyInAnyOrder(
            Document.class,
            Vault.class
        );
    }

    @Test
    void producedPayloadTypes()
    {
        var repository = new SetBasedDummyRepository();
        assertThat(repository.producedPayloadTypes()).containsExactly(Dummy.class);
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

    @ParameterizedTest
    @MethodSource("changes")
    void documentChanges(Dummy initialState, Change<?> inputChange, Change<?> expectedChange)
    {
        var repository = new SetBasedDummyRepository(initialState);
        var size = repository.set().size();
        var changelog = repository.apply(Changelog.changelogFor(inputChange));
        if (expectedChange == null)
        {
            softly.assertThat(changelog.isEmpty()).isTrue();
            softly.assertThat(repository.set().size()).isEqualTo(size);
        }
        else
        {
            softly.assertThat(changelog.changes().findFirst().get()).isEqualTo(expectedChange);
            var expectedSize = switch (expectedChange.kind())
            {
                case CREATE -> size + 1;
                case UPDATE -> size;
                case DELETE -> size - 1;
            };
            softly.assertThat(repository.set().size()).isEqualTo(expectedSize);
            var expectedPresent = switch (expectedChange.kind())
            {
                case CREATE, UPDATE -> true;
                case DELETE -> false;
            };
            var expectedDummy = new Dummy(expectedChange.as(Dummy.class).value().name());
            var isPresent = repository.set().contains(expectedDummy);
            softly.assertThat(isPresent).isEqualTo(expectedPresent);
        }
    }

    public static Stream<Arguments> changes()
    {
        var vault = new VaultStub();
        return Stream.of(
            Arguments.of(
                null,
                create(vault.addDocument("New", ""), Document.class),
                null
            ),
            Arguments.of(
                null,
                create(vault.addDocument("New", """
                        ---
                        dummy: true
                        ---
                        """
                    ), Document.class
                ),
                create(new Dummy("New"), Dummy.class)
            ),
            Arguments.of(
                null,
                delete(vault.addDocument("Delete", ""), Document.class),
                null
            ),
            Arguments.of(
                new Dummy("Delete"),
                delete(vault.addDocument("Delete", """
                        ---
                        dummy: true
                        ---
                        """
                    ), Document.class
                ),
                delete(new Dummy("Delete"), Dummy.class)
            ),
            Arguments.of(
                null,
                update(
                    vault.addDocument("Update", ""),
                    vault.addDocument("Update", ""),
                    Document.class
                ),
                null
            ),
            Arguments.of(
                null,
                update(
                    vault.addDocument("Update", ""),
                    vault.addDocument("Update", """
                        ---
                        dummy: true
                        ---
                        """
                    ),
                    Document.class
                ),
                create(new Dummy("Update"), Dummy.class)
            ),
            Arguments.of(
                new Dummy("Update"),
                update(
                    vault.addDocument("Update", """
                        ---
                        dummy: true
                        ---
                        """
                    ),
                    vault.addDocument("Update", ""),
                    Document.class
                ),
                delete(new Dummy("Update"), Dummy.class)
            ),
            Arguments.of(
                new Dummy("Update"),
                update(
                    vault.addDocument("Update", """
                        ---
                        dummy: true
                        ---
                        """
                    ),
                    vault.addDocument("Update", """
                        ---
                        dummy: true
                        ---
                        """
                    ),
                    Document.class
                ),
                update(new Dummy("Update"), new Dummy("Update"), Dummy.class)
            )
        );
    }
}
