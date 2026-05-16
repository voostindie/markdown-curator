package nl.ulso.curator.addon.projectjournal;

import nl.ulso.curator.addon.journal.Marker;
import nl.ulso.curator.vault.VaultStub;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Map;

import static nl.ulso.curator.addon.projectjournal.ProjectLeadMarker.isProjectLeadMarker;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SoftAssertionsExtension.class)
class ProjectLeadMarkerTest
{
    private VaultStub vault;
    private ProjectLeadMarker leadMarker;

    @BeforeEach
    void setUp()
    {
        this.vault = new VaultStub();
        var document = vault.addDocumentInPath("Stakeholder", """
            ---
            project-leads: ["Lead"]
            ---
            """
        );
        this.leadMarker = new ProjectLeadMarker(new Marker(document));
        vault.addDocumentInPath("Vincent", """
            That's me!
            """
        );
        vault.addDocumentInPath("Steve", """
            That's someone else!
            """
        );
    }

    @Test
    void name()
    {
        assertThat(leadMarker.name()).isEqualTo("Stakeholder");
    }

    @Test
    void frontMatterProperty()
    {
        assertThat(leadMarker.frontMatterProperty()).isEqualTo("project-leads");
    }

    @Test
    void markdownLinks()
    {
        assertThat(leadMarker.markdownLinks()).containsExactlyInAnyOrderEntriesOf(Map.of(
            "[[Stakeholder|Lead]]", "Lead"
        ));
    }

    @Test
    void isProjectLeadMarkerTrue()
    {
        var marker = new Marker(vault.findDocument("Stakeholder").orElseThrow());
        assertThat(isProjectLeadMarker(marker)).isTrue();
    }

    @Test
    void isProjectStatusMarkerFalse()
    {
        var marker = new Marker(vault.addDocument("Test", ""));
        assertThat(isProjectLeadMarker(marker)).isFalse();
    }

    @Test
    void resolveLeadFromValidLine()
    {
        var lead = leadMarker.resolveLeadFrom(
            "The [[Stakeholder|Lead]] is [[Vincent]].",
            vault
        );
        assertThat(lead).hasValue(vault.findDocument("Vincent").orElseThrow());
    }

    @Test
    void resolveLeadNoLinks()
    {
        var lead = leadMarker.resolveLeadFrom("The Lead is Vincent.", vault);
        assertThat(lead).isEmpty();
    }

    @Test
    void resolveLeadNoValidLink()
    {
        var lead = leadMarker.resolveLeadFrom(
            "The [[Stakeholder|Lead]] is [[Someone else]].",
            vault
        );
        assertThat(lead).isEmpty();

    }

    @Test
    void resolveLeadMultipleLinks()
    {
        var lead = leadMarker.resolveLeadFrom(
            "The [[Stakeholder|Lead]]s are [[Steve]] and [[Vincent]].",
            vault
        );
        assertThat(lead).hasValue(vault.findDocument("Steve").orElseThrow());
    }
}
