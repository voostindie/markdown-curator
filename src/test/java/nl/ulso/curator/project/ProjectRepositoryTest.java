package nl.ulso.curator.project;

import nl.ulso.curator.Change;
import nl.ulso.curator.vault.Document;
import nl.ulso.curator.vault.VaultStub;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;

import static nl.ulso.curator.Change.create;
import static nl.ulso.curator.Change.delete;
import static nl.ulso.curator.Change.update;
import static nl.ulso.curator.Changelog.changelogFor;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SoftAssertionsExtension.class)
class ProjectRepositoryTest
{
    @InjectSoftAssertions
    private SoftAssertions softly;

    private VaultStub vault;
    private ProjectRepositoryImpl repository;

    @BeforeEach
    void setUp()
    {
        vault = new VaultStub();
        repository = new ProjectRepositoryImpl(vault, new ProjectSettings("Projects"));
        var folder = vault.addFolder("Projects");
        var subfolder = folder.addFolder("Archived");
        vault.addDocument("README", "");
        folder.addDocument("Project 1", "");
        subfolder.addDocument("Archived Project", "");
        repository.fullRefresh();
    }

    @AfterEach
    void tearDown()
    {
        vault = null;
        repository = null;
    }

    @Test
    void emptyRepository()
    {
        var emptyVault = new VaultStub();
        var empyRepository = new ProjectRepositoryImpl(emptyVault, new ProjectSettings("Projects"));
        empyRepository.fullRefresh();
        assertThat(empyRepository.projectsByName()).isEmpty();
    }

    @Test
    void isProjectDocument()
    {
        var document = vault.resolveDocumentInPath("Projects/Project 1");
        var project = repository.projectFor(document);
        softly.assertThat(
                repository.isProjectDocument().test(Change.update(document, Document.class)))
            .isTrue();
        softly.assertThat(project).isPresent();
        softly.assertThat(project).map(Project::document).hasValue(document);
    }

    @Test
    void subFoldersNotIncluded()
    {
        var projects = repository.projectsByName();
        softly.assertThat(projects).hasSize(1);
        softly.assertThat(projects.get("Project 1")).isNotNull();
    }

    @Test
    void isNotProjectDocument()
    {
        var document = vault.resolveDocumentInPath("README");
        softly.assertThat(repository.isProjectDocument().test(update(document, Document.class))).isFalse();
        softly.assertThat(repository.projectFor(document)).isEmpty();
    }

    @Test
    void addProjectDocument()
    {
        var document = vault.addDocumentInPath("Projects/Project 2", "");
        repository.run(changelogFor(create(document, Document.class)));
        var projects = repository.projectsByName();
        softly.assertThat(projects).hasSize(2);
        softly.assertThat(projects.get("Project 2")).isNotNull();
    }

    @Test
    void removeProjectDocument()
    {
        var document = vault.resolveDocumentInPath("Projects/Project 1");
        repository.run(changelogFor(delete(document, Document.class)));
        var projects = repository.projectsByName();
        softly.assertThat(projects).isEmpty();
    }
}
