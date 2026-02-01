package nl.ulso.curator.vault;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DocumentFinderTest
{
    private VaultStub vault;

    @Test
    void documentInRoot()
    {
        var finder = new DocumentFinder("README");
        finder.visit(vault);
        assertThat(finder.document()).isPresent();
    }

    @Test
    void documentInFolder()
    {
        var finder = new DocumentFinder("Project 1");
        finder.visit(vault);
        assertThat(finder.document()).isPresent();
    }

    @Test
    void noDocument()
    {
        var finder = new DocumentFinder("Project Zero");
        finder.visit(vault);
        assertThat(finder.document()).isEmpty();
    }

    @BeforeEach
    void setUp()
    {
        vault = new VaultStub();
        vault.addDocument("README", "Dummy content");
        vault.addFolder("Projects").addDocument("Project 1", "Dummy project");
    }
}
