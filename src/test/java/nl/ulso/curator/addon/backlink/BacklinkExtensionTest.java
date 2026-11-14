package nl.ulso.curator.addon.backlink;

import nl.ulso.curator.change.Change;
import nl.ulso.curator.change.Changelog;
import nl.ulso.curator.query.*;
import nl.ulso.curator.vault.Document;
import nl.ulso.curator.vault.VaultStub;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;

import static nl.ulso.curator.change.Change.create;
import static nl.ulso.curator.change.Change.delete;
import static nl.ulso.curator.change.Change.update;
import static nl.ulso.curator.change.Changelog.changelogFor;
import static nl.ulso.curator.query.OutputFormat.MARKDOWN;
import static nl.ulso.dictionary.Dictionary.emptyDictionary;

/// Tests the backlinks extension as a whole: all its change processors, repositories and its query,
/// loaded into a change processor orchestrator on top of a vault stub.
@ExtendWith(SoftAssertionsExtension.class)
class BacklinkExtensionTest
{
    private static final String BACKLINKS_QUERY = """
        <!--query:backlinks-->
        <!--/query-->
        """;

    private final QueryResultFormatterRegistry formatterRegistry =
        QueryTestModule.createQueryResultFormatterRegistry();

    @InjectSoftAssertions
    private SoftAssertions softly;

    private BacklinkTestContext context;
    private VaultStub vault;

    @BeforeEach
    void setUp()
    {
        context = DaggerBacklinkTestContext.create();
        vault = context.vaultStub();
    }

    @Test
    void extensionLoadsWithoutFailures()
    {
        context.start();
        softly.assertThat(context.harness().failures()).isEmpty();
        softly.assertThat(context.harness().activeExtensions()).containsExactly("backlinks");
    }

    @Test
    void factoryDescribesExtension()
    {
        var factory = new BacklinkExtensionFactory(() -> null);
        softly.assertThat(factory.description()).contains("`backlinks`");
        softly.assertThat(factory.supportedConfiguration()).isEmpty();
        softly.assertThat(factory.requires()).isEmpty();
        softly.assertThat(factory.provides()).isEmpty();
        softly.assertThat(factory.contributions(emptyDictionary())).isEmpty();
    }

    @Test
    void queryDescribesItself()
    {
        context.start();
        var query = context.query();
        softly.assertThat(query.name()).isEqualTo("backlinks");
        softly.assertThat(query.description()).isNotBlank();
        softly.assertThat(query.supportedConfiguration()).containsOnlyKeys("document");
    }

    @Test
    void noBacklinksTrackedWithoutQueries()
    {
        vault.addDocument("Target", "");
        vault.addDocument("Source", "Link to [[Target]].");
        context.start();
        softly.assertThat(context.referenceRepository().backlinkDocumentNames()).isEmpty();
        softly.assertThat(context.backlinkRepository().backlinksFor("Target")).isEmpty();
    }

    @Test
    void backlinksForCurrentDocumentOnReset()
    {
        vault.addDocument("Target", BACKLINKS_QUERY);
        vault.addDocument("Source 1", "Link to [[Target]].");
        vault.addDocument("Source 2", "Two links to [[Target]] and [[Target|the target]].");
        vault.addDocument("Unrelated", "Link to [[Other]].");
        vault.addDocument("Other", "No links.");
        context.start();
        softly.assertThat(context.referenceRepository().backlinkDocumentNames())
            .containsExactly("Target");
        softly.assertThat(context.backlinkRepository().backlinksFor("Target"))
            .containsExactlyInAnyOrder("Source 1", "Source 2");
        softly.assertThat(context.backlinkRepository().backlinksFor("Other")).isEmpty();
        softly.assertThat(runQuery("Target")).isEqualTo("""
            - [[Source 1]]
            - [[Source 2]]
            """);
    }

    @Test
    void backlinksForOtherDocumentOnReset()
    {
        vault.addDocument("Overview", """
            <!--query:backlinks
            document: Target
            -->
            <!--/query-->
            """
        );
        vault.addDocument("Target", "");
        vault.addDocument("Source", "Link to [[Target]].");
        context.start();
        softly.assertThat(context.referenceRepository().hasBacklinkReference("Target")).isTrue();
        softly.assertThat(context.referenceRepository().hasBacklinkReference("Overview"))
            .isFalse();
        softly.assertThat(runQuery("Overview", "Target")).isEqualTo("- [[Source]]\n");
    }

    @Test
    void queryWithoutBacklinksReturnsEmptyResult()
    {
        vault.addDocument("Target", BACKLINKS_QUERY);
        context.start();
        softly.assertThat(context.backlinkRepository().backlinksFor("Target")).isEmpty();
        softly.assertThat(runQuery("Target")).isEqualTo("No results");
    }

    @Test
    void querySortsBacklinksIgnoringEmojis()
    {
        vault.addDocument("Target", BACKLINKS_QUERY);
        vault.addDocument("Charlie", "[[Target]]");
        vault.addDocument("🎸Bravo", "[[Target]]");
        vault.addDocument("Alpha", "[[Target]]");
        context.start();
        // Without stripping, the emoji would sort after all letters.
        softly.assertThat(runQuery("Target")).isEqualTo("""
            - [[Alpha]]
            - [[🎸Bravo]]
            - [[Charlie]]
            """);
    }

    @Test
    void newDocumentWithLinkCreatesBacklink()
    {
        vault.addDocument("Target", BACKLINKS_QUERY);
        vault.addDocument("Other", BACKLINKS_QUERY.replace("backlinks", "toc"));
        context.start();
        var source = vault.addDocument("Source", "Link to [[Target]] and [[Other]].");
        var changelog = run(create(source, Document.class));
        softly.assertThat(backlinkChanges(changelog))
            .containsExactly(create(new Backlink("Target", "Source"), Backlink.class));
        softly.assertThat(context.backlinkRepository().backlinksFor("Target"))
            .containsExactly("Source");
        softly.assertThat(isImpacted(changelog, "Target")).isTrue();
        softly.assertThat(isImpacted(changelog, "Other")).isFalse();
    }

    @Test
    void newDocumentWithoutTrackedLinksCreatesNothing()
    {
        vault.addDocument("Target", BACKLINKS_QUERY);
        context.start();
        var source = vault.addDocument("Source", "Link to [[Other]].");
        var changelog = run(create(source, Document.class));
        softly.assertThat(backlinkChanges(changelog)).isEmpty();
        softly.assertThat(isImpacted(changelog, "Target")).isFalse();
    }

    @Test
    void addingLinkToDocumentCreatesBacklink()
    {
        vault.addDocument("Target", BACKLINKS_QUERY);
        var oldSource = vault.addDocument("Source", "No links.");
        context.start();
        var newSource = vault.addDocument("Source", "Link to [[Target]].");
        var changelog = run(update(oldSource, newSource, Document.class));
        softly.assertThat(backlinkChanges(changelog))
            .containsExactly(create(new Backlink("Target", "Source"), Backlink.class));
        softly.assertThat(context.backlinkRepository().backlinksFor("Target"))
            .containsExactly("Source");
    }

    @Test
    void removingLinkFromDocumentDeletesBacklink()
    {
        vault.addDocument("Target", BACKLINKS_QUERY);
        var oldSource = vault.addDocument("Source", "Link to [[Target]].");
        context.start();
        var newSource = vault.addDocument("Source", "No more links.");
        var changelog = run(update(oldSource, newSource, Document.class));
        softly.assertThat(backlinkChanges(changelog))
            .containsExactly(delete(new Backlink("Target", "Source"), Backlink.class));
        softly.assertThat(context.backlinkRepository().backlinksFor("Target")).isEmpty();
        softly.assertThat(isImpacted(changelog, "Target")).isTrue();
    }

    @Test
    void keepingLinkInDocumentChangesNothing()
    {
        vault.addDocument("Target", BACKLINKS_QUERY);
        var oldSource = vault.addDocument("Source", "Link to [[Target]].");
        context.start();
        var newSource = vault.addDocument("Source", "Still a link to [[Target]].");
        var changelog = run(update(oldSource, newSource, Document.class));
        softly.assertThat(backlinkChanges(changelog)).isEmpty();
        softly.assertThat(context.backlinkRepository().backlinksFor("Target"))
            .containsExactly("Source");
        softly.assertThat(isImpacted(changelog, "Target")).isFalse();
    }

    @Test
    void replacingLinkInDocumentDeletesAndCreatesBacklinks()
    {
        vault.addDocument("Target 1", BACKLINKS_QUERY);
        vault.addDocument("Target 2", BACKLINKS_QUERY);
        vault.addDocument("Target 3", BACKLINKS_QUERY);
        var oldSource = vault.addDocument("Source", "[[Target 1]] and [[Target 2]]");
        context.start();
        var newSource = vault.addDocument("Source", "[[Target 2]] and [[Target 3]]");
        var changelog = run(update(oldSource, newSource, Document.class));
        softly.assertThat(backlinkChanges(changelog)).containsExactlyInAnyOrder(
            delete(new Backlink("Target 1", "Source"), Backlink.class),
            create(new Backlink("Target 3", "Source"), Backlink.class)
        );
        softly.assertThat(context.backlinkRepository().backlinksFor("Target 1")).isEmpty();
        softly.assertThat(context.backlinkRepository().backlinksFor("Target 2"))
            .containsExactly("Source");
        softly.assertThat(context.backlinkRepository().backlinksFor("Target 3"))
            .containsExactly("Source");
    }

    @Test
    void deletingDocumentDeletesBacklinks()
    {
        vault.addDocument("Target", BACKLINKS_QUERY);
        var source = vault.addDocument("Source", "Link to [[Target]].");
        context.start();
        vault.removeDocument("Source");
        var changelog = run(delete(source, Document.class));
        softly.assertThat(backlinkChanges(changelog))
            .containsExactly(delete(new Backlink("Target", "Source"), Backlink.class));
        softly.assertThat(context.backlinkRepository().backlinksFor("Target")).isEmpty();
    }

    @Test
    void deletingDocumentWithoutTrackedLinksChangesNothing()
    {
        vault.addDocument("Target", BACKLINKS_QUERY);
        var source = vault.addDocument("Source", "Link to [[Other]].");
        context.start();
        vault.removeDocument("Source");
        var changelog = run(delete(source, Document.class));
        softly.assertThat(backlinkChanges(changelog)).isEmpty();
    }

    @Test
    void addingQueryCollectsExistingBacklinksFromVault()
    {
        var oldTarget = vault.addDocument("Target", "No query yet.");
        vault.addDocument("Source 1", "Link to [[Target]].");
        vault.addDocument("Source 2", "Link to [[Target]].");
        vault.addDocument("Unrelated", "Link to [[Other]].");
        context.start();
        softly.assertThat(context.backlinkRepository().backlinksFor("Target")).isEmpty();
        var newTarget = vault.addDocument("Target", BACKLINKS_QUERY);
        var changelog = run(update(oldTarget, newTarget, Document.class));
        softly.assertThat(changelog.changesFor(BacklinkQueryReference.class).toList())
            .containsExactly(create(
                new BacklinkQueryReference("Target", "Target"), BacklinkQueryReference.class));
        softly.assertThat(backlinkChanges(changelog)).containsExactlyInAnyOrder(
            create(new Backlink("Target", "Source 1"), Backlink.class),
            create(new Backlink("Target", "Source 2"), Backlink.class)
        );
        softly.assertThat(context.backlinkRepository().backlinksFor("Target"))
            .containsExactlyInAnyOrder("Source 1", "Source 2");
    }

    @Test
    void addingSecondQueryForSameDocumentDoesNotRescanVault()
    {
        vault.addDocument("Target", BACKLINKS_QUERY);
        vault.addDocument("Source", "Link to [[Target]].");
        context.start();
        var overview = vault.addDocument("Overview", """
            <!--query:backlinks
            document: Target
            -->
            <!--/query-->
            """
        );
        var changelog = run(create(overview, Document.class));
        softly.assertThat(changelog.changesFor(BacklinkQueryReference.class).toList())
            .containsExactly(create(
                new BacklinkQueryReference("Overview", "Target"), BacklinkQueryReference.class));
        softly.assertThat(backlinkChanges(changelog)).isEmpty();
        softly.assertThat(context.backlinkRepository().backlinksFor("Target"))
            .containsExactly("Source");
    }

    @Test
    void removingLastQueryExpungesBacklinks()
    {
        var oldTarget = vault.addDocument("Target", BACKLINKS_QUERY);
        vault.addDocument("Source 1", "Link to [[Target]].");
        vault.addDocument("Source 2", "Link to [[Target]].");
        context.start();
        var newTarget = vault.addDocument("Target", "Query removed.");
        var changelog = run(update(oldTarget, newTarget, Document.class));
        softly.assertThat(changelog.changesFor(BacklinkQueryReference.class).toList())
            .containsExactly(delete(
                new BacklinkQueryReference("Target", "Target"), BacklinkQueryReference.class));
        softly.assertThat(backlinkChanges(changelog)).containsExactlyInAnyOrder(
            delete(new Backlink("Target", "Source 1"), Backlink.class),
            delete(new Backlink("Target", "Source 2"), Backlink.class)
        );
        softly.assertThat(context.referenceRepository().backlinkDocumentNames()).isEmpty();
        softly.assertThat(context.backlinkRepository().backlinksFor("Target")).isEmpty();
    }

    @Test
    void removingOneOfTwoQueriesKeepsBacklinks()
    {
        vault.addDocument("Target", BACKLINKS_QUERY);
        var overview = vault.addDocument("Overview", """
            <!--query:backlinks
            document: Target
            -->
            <!--/query-->
            """
        );
        vault.addDocument("Source", "Link to [[Target]].");
        context.start();
        vault.removeDocument("Overview");
        var changelog = run(delete(overview, Document.class));
        softly.assertThat(changelog.changesFor(BacklinkQueryReference.class).toList())
            .containsExactly(delete(
                new BacklinkQueryReference("Overview", "Target"), BacklinkQueryReference.class));
        softly.assertThat(backlinkChanges(changelog)).isEmpty();
        softly.assertThat(context.backlinkRepository().backlinksFor("Target"))
            .containsExactly("Source");
    }

    @Test
    void deletingDocumentWithQueryExpungesBacklinks()
    {
        var target = vault.addDocument("Target", BACKLINKS_QUERY);
        vault.addDocument("Source", "Link to [[Target]].");
        context.start();
        vault.removeDocument("Target");
        var changelog = run(delete(target, Document.class));
        softly.assertThat(backlinkChanges(changelog))
            .containsExactly(delete(new Backlink("Target", "Source"), Backlink.class));
        softly.assertThat(context.referenceRepository().backlinkDocumentNames()).isEmpty();
    }

    @Test
    void changingQueryTargetSwitchesBacklinks()
    {
        var oldOverview = vault.addDocument("Overview", """
            <!--query:backlinks
            document: Target 1
            -->
            <!--/query-->
            """
        );
        vault.addDocument("Source 1", "Link to [[Target 1]].");
        vault.addDocument("Source 2", "Link to [[Target 2]].");
        context.start();
        var newOverview = vault.addDocument("Overview", """
            <!--query:backlinks
            document: Target 2
            -->
            <!--/query-->
            """
        );
        var changelog = run(update(oldOverview, newOverview, Document.class));
        softly.assertThat(changelog.changesFor(BacklinkQueryReference.class).toList())
            .containsExactlyInAnyOrder(
                delete(new BacklinkQueryReference("Overview", "Target 1"),
                    BacklinkQueryReference.class
                ),
                create(new BacklinkQueryReference("Overview", "Target 2"),
                    BacklinkQueryReference.class
                )
            );
        softly.assertThat(backlinkChanges(changelog)).containsExactlyInAnyOrder(
            delete(new Backlink("Target 1", "Source 1"), Backlink.class),
            create(new Backlink("Target 2", "Source 2"), Backlink.class)
        );
        softly.assertThat(context.referenceRepository().backlinkDocumentNames())
            .containsExactly("Target 2");
    }

    @Test
    void unchangedQueryProducesNoReferenceChanges()
    {
        var oldTarget = vault.addDocument("Target", BACKLINKS_QUERY);
        context.start();
        var newTarget = vault.addDocument("Target", "Some text.\n\n" + BACKLINKS_QUERY);
        var changelog = run(update(oldTarget, newTarget, Document.class));
        softly.assertThat(changelog.changesFor(BacklinkQueryReference.class).toList()).isEmpty();
        softly.assertThat(context.referenceRepository().hasBacklinkReference("Target")).isTrue();
    }

    @Test
    void otherQueriesAreIgnored()
    {
        vault.addDocument("Target", """
            <!--query:toc-->
            <!--/query-->
            """
        );
        vault.addDocument("Source", "Link to [[Target]].");
        context.start();
        softly.assertThat(context.referenceRepository().backlinkDocumentNames()).isEmpty();
    }

    @Test
    void queryIsNotImpactedByUnrelatedChanges()
    {
        vault.addDocument("Target", BACKLINKS_QUERY);
        context.start();
        softly.assertThat(isImpacted(changelogFor(), "Target")).isFalse();
        softly.assertThat(isImpacted(
                changelogFor(create(new Backlink("Other", "Source"), Backlink.class)), "Target"))
            .isFalse();
        softly.assertThat(isImpacted(
                changelogFor(delete(new Backlink("Target", "Source"), Backlink.class)), "Target"))
            .isTrue();
    }

    private Changelog run(Change<?> change)
    {
        return context.harness().runFor(List.of(change));
    }

    private static List<Change<Backlink>> backlinkChanges(Changelog changelog)
    {
        return changelog.changesFor(Backlink.class).toList();
    }

    private QueryDefinitionStub definition(String documentName)
    {
        var query = context.query();
        return new QueryDefinitionStub(query, vault.findDocument(documentName).orElseThrow());
    }

    private boolean isImpacted(Changelog changelog, String documentName)
    {
        var document = vault.findDocument(documentName).orElseThrow();
        return context.query().isImpactedBy(changelog, new QueryDefinitionStub(
            context.query(), document)
        );
    }

    private String runQuery(String documentName)
    {
        return format(definition(documentName));
    }

    private String runQuery(String documentName, String targetDocumentName)
    {
        return format(definition(documentName).withConfiguration("document", targetDocumentName));
    }

    private String format(QueryDefinitionStub definition)
    {
        return formatterRegistry.format(context.query().run(definition), MARKDOWN);
    }
}
