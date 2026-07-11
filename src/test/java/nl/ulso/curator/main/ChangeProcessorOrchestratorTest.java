package nl.ulso.curator.main;

import nl.ulso.curator.change.*;
import nl.ulso.curator.statistics.Statistics;
import nl.ulso.curator.vault.Document;
import nl.ulso.curator.vault.Folder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.Logger;
import org.slf4j.event.Level;

import java.io.PrintWriter;
import java.util.*;

import static nl.ulso.curator.change.Changelog.emptyChangelog;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ChangeProcessorOrchestratorTest
{
    @Test
    void orderProducersBeforeConsumers()
    {
        var model1 = new ChangeProcessorStub(1).consuming(Integer.class);
        var model2 = new ChangeProcessorStub(2).consuming(Document.class).producing(Integer.class);
        var model3 = new ChangeProcessorStub(3).consuming(Integer.class);
        var model4 = new ChangeProcessorStub(4).consuming(Folder.class).producing(Integer.class);
        var models = createModelSet(model1, model2, model3, model4);
        var orchestrator = new DefaultChangeProcessorOrchestrator(models, new NullStatistics());
        assertThat(orchestrator.changeProcessors()).containsExactly(model2, model4, model1, model3);
    }

    @Test
    void orderProducersBeforeRequired()
    {
        var model1 = new ChangeProcessorStub(1).consuming(Document.class).requiring(Integer.class);
        var model2 = new ChangeProcessorStub(2).consuming(Document.class).producing(Integer.class);
        var models = createModelSet(model1, model2);
        var orchestrator = new DefaultChangeProcessorOrchestrator(models, new NullStatistics());
        assertThat(orchestrator.changeProcessors()).containsExactly(model2, model1);
    }

    @Test
    void acceptResetConsumers()
    {
        var model1 = new ChangeProcessorStub(1).consuming(Reset.class);
        var model2 = new ChangeProcessorStub(2).consuming(Document.class);
        var model3 = new ChangeProcessorStub(3).consuming(Folder.class);
        var models = createModelSet(model1, model2, model3);
        var orchestrator = new DefaultChangeProcessorOrchestrator(models, new NullStatistics());
        assertThat(orchestrator.changeProcessors()).containsExactly(model1, model2, model3);
    }

    @ParameterizedTest
    @ValueSource(classes = {Reset.class, Document.class, Folder.class})
    void rejectResetProducer(Class<?> payloadType)
    {
        var model = new ChangeProcessorStub(1).consuming(Integer.class).producing(payloadType);
        var models = createModelSet(model);
        var statistics = new NullStatistics();
        assertThatThrownBy(() -> new DefaultChangeProcessorOrchestrator(models, statistics))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("reserved payload type");

    }

    @Test
    void rejectConsumerProducerCycle()
    {
        var model1 = new ChangeProcessorStub(1).consuming(Integer.class).producing(String.class);
        var model2 = new ChangeProcessorStub(2).producing(Integer.class).consuming(String.class);
        var models = createModelSet(model1, model2);
        var statistics = new NullStatistics();
        assertThatThrownBy(() -> new DefaultChangeProcessorOrchestrator(models, statistics))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Dependency cycle");
    }

    @Test
    void rejectProcessorThatConsumersNothing()
    {
        var model = new ChangeProcessorStub(1);
        var models = createModelSet(model);
        var statistics = new NullStatistics();
        assertThatThrownBy(() -> new DefaultChangeProcessorOrchestrator(models, statistics))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("must consume at least one payload type");
    }

    @Test
    void rejectUnsatisifiedConsumer()
    {
        var model = new ChangeProcessorStub(1).consuming(Integer.class);
        var models = createModelSet(model);
        var statistics = new NullStatistics();
        assertThatThrownBy(() -> new DefaultChangeProcessorOrchestrator(models, statistics))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("unsatisfied consumer");
    }

    /// Creates a set of models that are in a guaranteed order to ensure non-flaky tests. Data
    /// models are ordered on their... order.
    private Set<ChangeProcessor> createModelSet(ChangeProcessorStub... models)
    {
        return new TreeSet<>(Arrays.asList(models));
    }

    static final class ChangeProcessorStub
        implements ChangeProcessor, Comparable<ChangeProcessorStub>
    {
        private final int order;
        private final Set<Class<?>> processedPayloadTypes = new HashSet<>();
        private final Set<Class<?>> consumedPayloadTypes = new HashSet<>();
        private final Set<Class<?>> requiredPayloadTypes = new HashSet<>();

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

        ChangeProcessorStub requiring(Class<?> payloadType)
        {
            requiredPayloadTypes.add(payloadType);
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
        public Changelog apply(Changelog changelog)
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

        @Override
        public Set<Class<?>> requiredPayloadTypes()
        {
            return requiredPayloadTypes;
        }

        @Override
        public String name()
        {
            return "Stub-" + order;
        }
    }

    private static final class NullStatistics
        implements Statistics
    {
        @Override
        public void logTo(Logger logger, Level level)
        {
            // Do nothing.
        }

        @Override
        public void logTo(PrintWriter writer)
        {
            // Do nothing.
        }
    }
}
