package nl.ulso.curator.addon.project;

import nl.ulso.curator.change.Change;
import nl.ulso.curator.vault.VaultStub;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Set;
import java.util.stream.Stream;

import static nl.ulso.curator.addon.project.ProjectTestData.ATTRIBUTE_DEFINITIONS;
import static nl.ulso.curator.change.Change.Kind.CREATE;
import static nl.ulso.curator.change.Change.Kind.DELETE;
import static nl.ulso.curator.change.Change.Kind.UPDATE;
import static nl.ulso.curator.change.Change.create;
import static nl.ulso.curator.change.Change.update;
import static nl.ulso.curator.change.Changelog.changelogFor;
import static org.assertj.core.api.Assertions.assertThat;

class FrontMatterAttributeProducerTest
{
    private VaultStub vault;
    private FrontMatterAttributeProducer producer;

    @BeforeEach
    void setUp()
    {
        vault = ProjectTestData.createTestVault();
        producer = new FrontMatterAttributeProducer(ATTRIBUTE_DEFINITIONS, vault);
    }

    @Test
    void consumedPayloadTypes()
    {
        assertThat(producer.consumedPayloadTypes())
            .containsAll(Set.of(Project.class));
    }

    @Test
    void producedPayloadTypes()
    {
        assertThat(producer.producedPayloadTypes())
            .containsAll(Set.of(AttributeValue.class));
    }

    @ParameterizedTest
    @MethodSource("projectUpdates")
    void projectUpdateAddsAttributes(
        String oldContent, String newContent, Change.Kind testKind, String expectedProperty
    )
    {
        var path = "Projects/project";
        var oldProject = new Project(vault.addDocumentInPath(path, oldContent));
        var newProject = new Project(vault.addDocumentInPath(path, newContent));
        var init = create(newProject, Project.class);
        var changelog =
            producer.apply(changelogFor(init, update(oldProject, newProject, Project.class)));
        var changes = changelog.changesFor(AttributeValue.class)
            .filter(change -> change.kind() == testKind)
            .map(change -> change.value().definition().frontMatterProperty())
            .toList();
        assertThat(changes).contains(expectedProperty);
    }

    static Stream<Arguments> projectUpdates()
    {
        return Stream.of(
            Arguments.of(
                "",
                """
                    ---
                    status: done
                    ---
                    """,
                CREATE,
                "status"
            ),
            Arguments.of(
                """
                    ---
                    status: done
                    ---
                    """,
                "",
                DELETE,
                "status"
            ),
            Arguments.of(
                """
                    ---
                    last_modified: 2026-01-31
                    ---
                    """,
                """
                    ---
                    last_modified: 2026-02-06
                    ---
                    """,
                UPDATE,
                "last_modified"
            )
        );
    }
}
