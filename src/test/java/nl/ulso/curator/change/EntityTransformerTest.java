package nl.ulso.curator.change;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;
import static nl.ulso.curator.change.Change.create;
import static nl.ulso.curator.change.Change.delete;
import static nl.ulso.curator.change.Change.update;
import static nl.ulso.curator.change.Changelog.changelogFor;
import static org.assertj.core.api.Assertions.assertThat;

class EntityTransformerTest
{
    @Test
    void sourceClass()
    {
        var producer = new EntityProducerStub();
        assertThat(producer.sourceClass()).isEqualTo(Integer.class);
    }

    @Test
    void consumedPayloadTypes()
    {
        var producer = new EntityProducerStub();
        assertThat(producer.consumedPayloadTypes()).containsExactly(Integer.class);
    }

    @Test
    void targetClass()
    {
        var producer = new EntityProducerStub();
        assertThat(producer.targetClass()).isEqualTo(String.class);
    }

    @Test
    void producedPayloadTypes()
    {
        var producer = new EntityProducerStub();
        assertThat(producer.producedPayloadTypes()).containsExactly(String.class);
    }

    @ParameterizedTest
    @MethodSource("testCases")
    void processChangelog(List<Change<?>> input, List<String> expected)
    {
        var producer = new EntityProducerStub();
        var changelog = producer.apply(changelogFor(input));
        var strings = changelog.changes()
            .map(c -> c.kind().toString().charAt(0) + ":" + c.value())
            .toList();
        assertThat(strings).containsExactlyElementsOf(expected);
    }

    private static Stream<Arguments> testCases()
    {
        return Stream.of(
            Arguments.of(List.of(create(42, Integer.class)), List.of("C:42")),
            Arguments.of(List.of(create(-42, Integer.class)), emptyList()),
            Arguments.of(List.of(update(42, 43, Integer.class)), List.of("U:43")),
            Arguments.of(List.of(update(-42, 42, Integer.class)), List.of("C:42")),
            Arguments.of(List.of(update(-42, -43, Integer.class)), emptyList()),
            Arguments.of(List.of(update(42, -42, Integer.class)), List.of("D:42")),
            Arguments.of(List.of(delete(42, Integer.class)), List.of("D:42")),
            Arguments.of(List.of(delete(-42, Integer.class)), emptyList())
        );
    }

    private static class EntityProducerStub
        extends EntityTransformer<Integer, String>
    {

        @Override
        protected Class<Integer> sourceClass()
        {
            return Integer.class;
        }

        @Override
        protected Class<String> targetClass()
        {
            return String.class;
        }

        @Override
        protected Optional<String> transform(Integer integer)
        {
            if (integer < 0)
            {
                return Optional.empty();
            }
            return Optional.of(integer.toString());
        }
    }
}
