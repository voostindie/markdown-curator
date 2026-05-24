package nl.ulso.curator.addon.omnifocus;

import org.junit.jupiter.api.Test;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultOmniFocusDatabaseTest
{
    @Test
    void existingFileIsAccessible()
    {
        var database = new DefaultOmniFocusDatabase(new File("README.md"));
        assertThat(database.isAccessible()).isTrue();
    }

    @Test
    void existingFileHasLastModified()
    {
        var readme = new File("README.md");
        var expected = readme.lastModified();
        var database = new DefaultOmniFocusDatabase(readme);
        assertThat(database.lastModified()).isEqualTo(expected);
    }

    @Test
    void existingFileHasPath()
    {
        var readme = new File("README.md");
        var database = new DefaultOmniFocusDatabase(readme);
        assertThat(database.path()).endsWith("README.md");
    }

    @Test
    void nonExistentFileIsInaccessible()
    {
        var database = new DefaultOmniFocusDatabase(new File("NOTHING"));
        assertThat(database.isAccessible()).isFalse();
    }

    @Test
    void nonExistentFileHasZeroLastModified()
    {
        var database = new DefaultOmniFocusDatabase(new File("NOTHING"));
        assertThat(database.lastModified()).isZero();
    }

    @Test
    void nonExistentFileHasPath()
    {
        var database = new DefaultOmniFocusDatabase(new File("NOTHING"));
        assertThat(database.path()).endsWith("NOTHING");
    }
}
