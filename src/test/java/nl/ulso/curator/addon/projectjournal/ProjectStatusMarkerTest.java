package nl.ulso.curator.addon.projectjournal;

import nl.jqno.equalsverifier.EqualsVerifier;
import nl.ulso.curator.addon.journal.Marker;
import nl.ulso.curator.vault.Document;
import nl.ulso.curator.vault.VaultStub;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Map;

import static nl.ulso.curator.addon.projectjournal.ProjectStatusMarker.isProjectStatusMarker;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SoftAssertionsExtension.class)
class ProjectStatusMarkerTest
{
    private VaultStub vault;
    private ProjectStatusMarker statusMarker;

    @BeforeEach
    void setUp()
    {
        this.vault = new VaultStub();
        var document = vault.addDocumentInPath("Status", """
            ---
            project-statuses: ["New", "In progress", "Done"]
            ---
            """
        );
        this.statusMarker = new ProjectStatusMarker(new Marker(document));
    }

    @Test
    void name()
    {
        assertThat(statusMarker.name()).isEqualTo("Status");
    }

    @Test
    void frontMatterProperty()
    {
        assertThat(statusMarker.frontMatterProperty()).isEqualTo("project-statuses");
    }

    @Test
    void markdownLinks()
    {
        assertThat(statusMarker.markdownLinks()).containsExactlyInAnyOrderEntriesOf(Map.of(
            "[[Status|New]]", "New",
            "[[Status|In progress]]", "In progress",
            "[[Status|Done]]", "Done"
        ));
    }

    @Test
    void isProjectStatusMarkerTrue()
    {
        var marker = new Marker(vault.findDocument("Status").orElseThrow());
        assertThat(isProjectStatusMarker(marker)).isTrue();
    }

    @Test
    void isProjectStatusMarkerFalse()
    {
        var marker = new Marker(vault.addDocument("Test", ""));
        assertThat(isProjectStatusMarker(marker)).isFalse();
    }

    @Test
    void resolveStatusFromValidLink()
    {
        var status = statusMarker.resolveStatusFrom("[[Status|In progress]]");
        assertThat(status).isEqualTo("In progress");
    }

    @Test
    void resolveStatusFromInvalidLink()
    {
        var status = statusMarker.resolveStatusFrom("[[Status|Unknown]]");
        assertThat(status).isNull();
    }

    @Test
    void testToString()
    {
        assertThat(statusMarker.toString()).isEqualTo("Status");
    }

    @Test
    void testEquals()
    {
        EqualsVerifier.forClass(ProjectStatusMarker.class)
            .withPrefabValues(
                Document.class,
                vault.findDocument("Status").orElseThrow(),
                vault.addDocument("Other", "")
            )
            .withPrefabValues(
                Marker.class,
                new Marker(vault.findDocument("Status").orElseThrow()),
                new Marker(vault.addDocument("Other", ""))
            )
            .withRedefinedSuperclass()
            .withIgnoredFields("markdownLinks")
            .verify();
    }
}
