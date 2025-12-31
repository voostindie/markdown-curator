package nl.ulso.markdown_curator;

import nl.ulso.markdown_curator.vault.event.VaultChangedEvent;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static java.util.Collections.emptySet;
import static nl.ulso.markdown_curator.Changelog.emptyChangelog;
import static nl.ulso.markdown_curator.Curator.orderDataModels;
import static org.assertj.core.api.Assertions.assertThat;

class CuratorTest
{
    @Test
    void orderDataModelsCorrectly()
    {
        var model1 = new DataModelStub();
        var model2 = new DataModelStub(model1);
        var model3 = new DataModelStub(model1, model2);
        var list = orderDataModels(Set.of(model1, model2, model3));
        assertThat(list).containsExactly(model1, model2, model3);
    }

    private static final class DataModelStub
        implements DataModel
    {
        private final Set<DataModel> dependentModels;

        DataModelStub()
        {
            this.dependentModels = emptySet();
        }

        DataModelStub(DataModel... dependentModels)
        {
            this.dependentModels = Set.of(dependentModels);
        }

        @Override
        public Changelog vaultChanged(VaultChangedEvent event, Changelog changelog)
        {
            return emptyChangelog();
        }

        @Override
        public Set<DataModel> dependentModels()
        {
            return dependentModels;
        }
    }
}
