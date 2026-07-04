package nl.ulso.curator.addon.project;

import nl.ulso.curator.vault.VaultStub;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;

import static nl.ulso.curator.change.Change.create;
import static nl.ulso.curator.change.Change.delete;
import static nl.ulso.curator.change.Changelog.changelogFor;
import static nl.ulso.curator.main.VaultTestSupport.initializeVault;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SoftAssertionsExtension.class)
class DefaultProjectRepositoryTest
{
    @InjectSoftAssertions
    private SoftAssertions softly;

    private VaultStub vault;
    private DefaultProjectRepository repository;

    @BeforeEach
    void setUp()
    {
        vault = new VaultStub();
        var producer = new ProjectProducer(new ProjectSettings("Projects"));
        repository = new DefaultProjectRepository();
        vault.addDocument("README", "");
        vault.addDocumentInPath("Projects/Project 1", "");
        vault.addDocumentInPath("Projects/Archived/Archived Project", "");
        var changelog =
            producer.apply(initializeVault(vault).changelogFor(producer.consumedPayloadTypes()));
        repository.apply(changelog.changelogFor(repository.consumedPayloadTypes()));
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
        var emptyRepository = new DefaultProjectRepository();
        assertThat(emptyRepository.projectsByName()).isEmpty();
    }

    @Test
    void isProjectDocument()
    {
        var document = vault.resolveDocumentInPath("Projects/Project 1");
        var project = repository.projectFor(document);
        assertThat(project).map(Project::document).hasValue(document);
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
        softly.assertThat(repository.projectFor(document)).isEmpty();
    }

    @Test
    void addProject()
    {
        var document = vault.addDocumentInPath("Projects/Project 2", "");
        repository.apply(changelogFor(create(new Project(document), Project.class)));
        var projects = repository.projectsByName();
        softly.assertThat(projects).hasSize(2);
        softly.assertThat(projects.get("Project 2")).isNotNull();
    }

    @Test
    void removeProject()
    {
        var document = vault.resolveDocumentInPath("Projects/Project 1");
        repository.apply(changelogFor(delete(new Project(document), Project.class)));
        var projects = repository.projectsByName();
        softly.assertThat(projects).isEmpty();
    }
}
