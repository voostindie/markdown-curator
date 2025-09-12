package nl.ulso.markdown_curator.project;

import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Optional;
import java.util.Set;

import static nl.ulso.markdown_curator.project.ProjectProperty.LAST_MODIFIED;
import static nl.ulso.markdown_curator.project.ProjectProperty.LEAD;
import static nl.ulso.markdown_curator.project.ProjectProperty.PRIORITY;
import static nl.ulso.markdown_curator.project.ProjectProperty.STATUS;
import static nl.ulso.markdown_curator.project.ProjectTestData.PROJECT_PROPERTIES;

@ExtendWith(SoftAssertionsExtension.class)
class ProjectPropertyResolverRegistryTest
{
    private ProjectPropertyResolverRegistry registry;

    @InjectSoftAssertions
    private SoftAssertions softly;

    @BeforeEach
    void setUp()
    {
        this.registry = new ProjectPropertyResolverRegistryImpl(Set.of(
                new FrontMatterProjectPropertyResolver(PROJECT_PROPERTIES.get(STATUS), null),
                new FrontMatterProjectPropertyResolver(PROJECT_PROPERTIES.get(LEAD), null),
                new FrontMatterProjectPropertyResolver(PROJECT_PROPERTIES.get(PRIORITY), null),
                new DummyProjectPropertyResolver(PROJECT_PROPERTIES.get(PRIORITY), 1)
        ));

    }

    @Test
    void newRegistry()
    {
        softly.assertThat(registry.resolversFor(PROJECT_PROPERTIES.get(LAST_MODIFIED)))
                .hasSize(0);
        softly.assertThat(registry.resolversFor(PROJECT_PROPERTIES.get(LEAD))).hasSize(1);
        softly.assertThat(registry.resolversFor(PROJECT_PROPERTIES.get(PRIORITY))).hasSize(2);
    }

    @Test
    void orderedResolvers()
    {
        var list = registry.resolversFor(PROJECT_PROPERTIES.get(PRIORITY));
        softly.assertThat(list).hasSize(2);
        softly.assertThat(list.getFirst()).isInstanceOf(DummyProjectPropertyResolver.class);
        softly.assertThat(list.getLast()).isInstanceOf(FrontMatterProjectPropertyResolver.class);
    }

    private static class DummyProjectPropertyResolver
            implements ProjectPropertyResolver
    {
        private final ProjectProperty property;
        private final int priority;

        public DummyProjectPropertyResolver(ProjectProperty property, int constantPriority)
        {
            this.property = property;
            this.priority = constantPriority;
        }

        @Override
        public ProjectProperty projectProperty()
        {
            return property;
        }

        @Override
        public Optional<?> resolveValue(Project project)
        {
            return Optional.of(priority);
        }

        @Override
        public int order()
        {
            return 1;
        }
    }
}
