package nl.ulso.markdown_curator.project;

import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Optional;
import java.util.Set;

@ExtendWith(SoftAssertionsExtension.class)
class AttributeValueResolverRegistryTest
{
    private AttributeValueResolverRegistry registry;

    @InjectSoftAssertions
    private SoftAssertions softly;

    @BeforeEach
    void setUp()
    {
        this.registry = new DefaultAttributeValueResolverRegistry(Set.of(
                new FrontMatterAttributeValueResolver<>(Attribute.LEAD, "last-modified", null),
                new FrontMatterAttributeValueResolver<>(Attribute.PRIORITY, "last-modified", null),
                new FrontMatterAttributeValueResolver<>(Attribute.STATUS, "status", null),
                new DummyAttributeValueResolver(Attribute.PRIORITY, 1)
        ));
    }

    @Test
    void newRegistry()
    {
        softly.assertThat(registry.resolversFor(Attribute.LAST_MODIFIED)).hasSize(0);
        softly.assertThat(registry.resolversFor(Attribute.LEAD)).hasSize(1);
        softly.assertThat(registry.resolversFor(Attribute.PRIORITY)).hasSize(2);
    }

    @Test
    void orderedResolvers()
    {
        var list = registry.resolversFor(Attribute.PRIORITY);
        softly.assertThat(list).hasSize(2);
        softly.assertThat(list.getFirst()).isInstanceOf(DummyAttributeValueResolver.class);
        softly.assertThat(list.getLast()).isInstanceOf(FrontMatterAttributeValueResolver.class);
    }

    private static class DummyAttributeValueResolver
            implements AttributeValueResolver<Integer>
    {
        private final Attribute<Integer> attribute;
        private final int priority;

        public DummyAttributeValueResolver(Attribute<Integer> attribute, int constantPriority)
        {
            this.attribute = attribute;
            this.priority = constantPriority;
        }

        @Override
        public Attribute<Integer> attribute()
        {
            return attribute;
        }

        @Override
        public Optional<Integer> resolveValue(Project project)
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
