package nl.ulso.markdown_curator;

import nl.ulso.markdown_curator.vault.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.*;

import static nl.ulso.markdown_curator.Changelog.emptyChangelog;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ChangeProcessorOrchestratorTest
{
    @Test
    void orderProducersBeforeConsumers()
    {
        var model1 = new ChangeProcessorStub(1).consuming(Integer.class);
        var model2 = new ChangeProcessorStub(2).producing(Integer.class);
        var model3 = new ChangeProcessorStub(3).consuming(Integer.class);
        var model4 = new ChangeProcessorStub(4).producing(Integer.class);
        var models = createModelSet(model1, model2, model3, model4);
        var orchestrator = new ChangeProcessorOrchestratorImpl(models);
        assertThat(orchestrator.changeProcessors()).containsExactly(model2, model4, model1, model3);
    }

    @Test
    void acceptVaultConsumers()
    {
        var model1 = new ChangeProcessorStub(1).consuming(Vault.class);
        var model2 = new ChangeProcessorStub(2).consuming(Document.class);
        var model3 = new ChangeProcessorStub(3).consuming(Folder.class);
        var models = createModelSet(model1, model2, model3);
        var orchestrator = new ChangeProcessorOrchestratorImpl(models);
        assertThat(orchestrator.changeProcessors()).containsExactly(model1, model2, model3);
    }

    @ParameterizedTest
    @ValueSource(classes = {Vault.class, Document.class, Folder.class})
    void rejectVaultProducer(Class<?> payloadType)
    {
        var model = new ChangeProcessorStub(1).producing(payloadType);
        var models = createModelSet(model);
        assertThatThrownBy(() -> new ChangeProcessorOrchestratorImpl(models))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("reserved payload type");

    }

    @Test
    void rejectConsumerProducerCycle()
    {
        var model1 = new ChangeProcessorStub(1).consuming(Integer.class).producing(String.class);
        var model2 = new ChangeProcessorStub(2).producing(Integer.class).consuming(String.class);
        var models = createModelSet(model1, model2);
        assertThatThrownBy(() -> new ChangeProcessorOrchestratorImpl(models))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Dependency cycle");
    }

    @Test
    void rejectUnsatisifiedConsumer()
    {
        var model = new ChangeProcessorStub(1).consuming(Integer.class);
        var models = createModelSet(model);
        assertThatThrownBy(() -> new ChangeProcessorOrchestratorImpl(models))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("unsatisfied consumer");
    }

    /// Creates a set of models that are in a guaranteed order, to ensure non-flaky tests. Data
    /// models are ordered on their... order.
    private Set<ChangeProcessor> createModelSet(ChangeProcessorStub... models)
    {
        return new TreeSet<>(Arrays.asList(models));
    }

    private static final class ChangeProcessorStub
        implements ChangeProcessor, Comparable<ChangeProcessorStub>
    {
        private final int order;
        private final Set<Class<?>> processedPayloadTypes = new HashSet<>();
        private final Set<Class<?>> consumedPayloadTypes = new HashSet<>();

        ChangeProcessorStub(int order)
        {
            this.order = order;
        }

        ChangeProcessorStub consuming(Class<?> payloadType)
        {
            consumedPayloadTypes.add(payloadType);
            return this;
        }

        ChangeProcessorStub producing(Class<?> payloadType)
        {
            processedPayloadTypes.add(payloadType);
            return this;
        }

        @Override
        public int compareTo(ChangeProcessorStub o)
        {
            return Integer.compare(order, o.order);
        }

        @Override
        public String toString()
        {
            return "ChangeProcessorStub{" + order + '}';
        }

        @Override
        public Changelog run(Changelog changelog)
        {
            return emptyChangelog();
        }

        @Override
        public Set<Class<?>> consumedPayloadTypes()
        {
            return consumedPayloadTypes;
        }

        @Override
        public Set<Class<?>> producedPayloadTypes()
        {
            return processedPayloadTypes;
        }
    }
}
