package nl.ulso.markdown_curator;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static nl.ulso.markdown_curator.Change.create;
import static nl.ulso.markdown_curator.Changelog.changelogFor;
import static nl.ulso.markdown_curator.Changelog.emptyChangelog;
import static org.assertj.core.api.Assertions.assertThat;

class ChangelogTest
{
    @Test
    void emptyChangelogIsEmpty()
    {
        var changelog = emptyChangelog();
        assertThat(changelog.isEmpty()).isTrue();
    }

    @Test
    void emptyChangelogHasZeroItems()
    {
        var items = emptyChangelog().changes().toList();
        assertThat(items).isEmpty();
    }

    @Test
    void changelogWithOneEntryisNotEmpty()
    {
        var changelog = changelogFor(create(new Object(), Object.class));
        assertThat(changelog.isEmpty()).isFalse();
    }

    @Test
    void changelogWithOneEntryHasOneItem()
    {
        var items = changelogFor(create(new Object(), Object.class)).changes().toList();
        assertThat(items).hasSize(1);

    }

    @Test
    void emptyChangelogIsSingleton()
    {
        var log1 = emptyChangelog();
        var log2 = emptyChangelog();
        assertThat(log1).isSameAs(log2);
    }

    @Test
    void appendingTwoEmptyChangelogsResultsInEmptyChangelog()
    {
        var log1 = emptyChangelog();
        var log2 = emptyChangelog();
        var result = log1.append(log2);
        assertThat(result).isSameAs(emptyChangelog());
    }

    @Test
    void appendingEmptyChangelogToNonEmptyChangelogResultsInFirstChangelog()
    {
        var log1 = changelogFor(create(new Object(), Object.class));
        var log2 = emptyChangelog();
        assertThat(log1.append(log2)).isSameAs(log1);
    }

    @Test
    void appendingNonEmptyChangelogToEmptyChangelogResultsInSecondChangelog()
    {
        var log1 = emptyChangelog();
        var log2 = changelogFor(create(new Object(), Object.class));
        assertThat(log1.append(log2)).isSameAs(log2);
    }

    @Test
    void payloadsCanBeMixed()
    {
        var changelog = changelogFor(
            create(new Object(), Object.class),
            create(42, Integer.class),
            create(true, Boolean.class)
        );
        assertThat(changelog.changes().toList()).hasSize(3);
    }

    @Test
    void mixedPayloadsCanBeSplit()
    {
        var changelog = changelogFor(
            create(new Object(), Object.class),
            create(42, Integer.class),
            create(true, Boolean.class)
        );
        var changes = changelog.changesFor(Object.class).toList();
        assertThat(changes).hasSize(1);
    }

    @Test
    void changedObjectsAreTypeSafe()
    {
        var changelog = changelogFor(
            create(new Object(), Object.class),
            create(42, Integer.class),
            create(true, Boolean.class)
        );
        var changes = changelog.changesFor(Integer.class).toList();
        var object = changes.getFirst().value();
        assertThat(object).isEqualTo(42);
    }

    @Test
    void changeLogsCanBeFiltered()
    {
        var changelog = changelogFor(
            create(new Object(), Object.class),
            create(42, Integer.class),
            create(true, Boolean.class)
        );
        var filteredChangelog = changelog.changelogFor(Set.of(Integer.class, Boolean.class));
        assertThat(filteredChangelog.changes().toList()).hasSize(2);
    }
}
