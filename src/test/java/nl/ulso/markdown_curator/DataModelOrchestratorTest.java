package nl.ulso.markdown_curator;

import nl.ulso.markdown_curator.vault.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.*;

import static nl.ulso.markdown_curator.Changelog.emptyChangelog;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DataModelOrchestratorTest
{
    @Test
    void orderDependentModels()
    {
        var model1 = new DataModelStub(1);
        var model2 = new DataModelStub(2);
        var model3 = new DataModelStub(3);
        model1.addDependency(model2);
        model2.addDependency(model3);
        model2.addDependency(model3);
        var orchestrator = new DataModelOrchestratorImpl(Set.of(model1, model2, model3));
        assertThat(orchestrator.dataModels()).containsExactly(model3, model2, model1);
    }

    @Test
    void orderProducersBeforeConsumers()
    {
        var model1 = new DataModelStub(1).consuming(Integer.class);
        var model2 = new DataModelStub(2).producing(Integer.class);
        var model3 = new DataModelStub(3).consuming(Integer.class);
        var model4 = new DataModelStub(4).producing(Integer.class);
        var models = createModelSet(model1, model2, model3, model4);
        var orchestrator = new DataModelOrchestratorImpl(models);
        assertThat(orchestrator.dataModels()).containsExactly(model2, model4, model1, model3);
    }

    @Test
    void acceptVaultObjectConsumers()
    {
        var model1 = new DataModelStub(1).consuming(Vault.class);
        var model2 = new DataModelStub(2).consuming(Document.class);
        var model3 = new DataModelStub(3).consuming(Folder.class);
        var models = createModelSet(model1, model2, model3);
        var orchestrator = new DataModelOrchestratorImpl(models);
        assertThat(orchestrator.dataModels()).containsExactly(model1, model2, model3);
    }

    @ParameterizedTest
    @ValueSource(classes = {Vault.class, Document.class, Folder.class})
    void rejectVaultObjectProducer(Class<?> objectType)
    {
        var model = new DataModelStub(1).producing(objectType);
        var models = createModelSet(model);
        assertThatThrownBy(() -> new DataModelOrchestratorImpl(models))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("reserved object type");

    }

    @Test
    void rejectDependencyCycle()
    {
        var model1 = new DataModelStub(1);
        var model2 = new DataModelStub(2);
        var model3 = new DataModelStub(3);
        model1.addDependency(model2);
        model2.addDependency(model3);
        model3.addDependency(model1);
        var models = createModelSet(model1, model2, model3);
        assertThatThrownBy(() -> new DataModelOrchestratorImpl(models))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Dependency cycle");
    }

    @Test
    void rejectConsumerProducerCycle()
    {
        var model1 = new DataModelStub(1).consuming(Integer.class).producing(String.class);
        var model2 = new DataModelStub(2).producing(Integer.class).consuming(String.class);
        var models = createModelSet(model1, model2);
        assertThatThrownBy(() -> new DataModelOrchestratorImpl(models))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Dependency cycle");
    }

    @Test
    void rejectUnsatisifiedConsumer()
    {
        var model = new DataModelStub(1).consuming(Integer.class);
        var models = createModelSet(model);
        assertThatThrownBy(() -> new DataModelOrchestratorImpl(models))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("unsatisfied consumer");
    }

    /// Creates a set of models that are in a guaranteed order, to ensure non-flaky tests. Data
    /// models are ordered on their... order.
    private Set<DataModel> createModelSet(DataModelStub... models)
    {
        return new TreeSet<>(Arrays.asList(models));
    }

    private static final class DataModelStub
        implements DataModel, Comparable<DataModelStub>
    {
        private final int order;
        private final Set<DataModel> dependentModels = new HashSet<>();
        private final Set<Class<?>> processedObjectTypes = new HashSet<>();
        private final Set<Class<?>> consumedObjectTypes = new HashSet<>();

        DataModelStub(int order)
        {
            this.order = order;
        }

        DataModelStub consuming(Class<?> objectType)
        {
            consumedObjectTypes.add(objectType);
            return this;
        }

        DataModelStub producing(Class<?> objectType)
        {
            processedObjectTypes.add(objectType);
            return this;
        }

        void addDependency(DataModel dependentModel)
        {
            dependentModels.add(dependentModel);
        }

        @Override
        public int compareTo(DataModelStub o)
        {
            return Integer.compare(order, o.order);
        }

        @Override
        public String toString()
        {
            return "DataModelStub{" + order + '}';
        }

        @Override
        public Changelog process(Changelog changelog)
        {
            return emptyChangelog();
        }

        @Override
        public Set<?> dependentModels()
        {
            return dependentModels;
        }

        @Override
        public Set<Class<?>> consumedObjectTypes()
        {
            return consumedObjectTypes;
        }

        @Override
        public Set<Class<?>> producedObjectTypes()
        {
            return processedObjectTypes;
        }
    }
}
