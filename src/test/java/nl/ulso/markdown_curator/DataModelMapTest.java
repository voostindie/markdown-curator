package nl.ulso.markdown_curator;

import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(SoftAssertionsExtension.class)
class DataModelMapTest
{

    @InjectSoftAssertions
    private SoftAssertions softly;

    @Test
    void emptyMap()
    {
        var map = new DataModelMap(new HashMap<>());
        assertThat(map.models()).isEmpty();
    }

    @Test
    void validModel()
    {
        var map = new DataModelMap(Map.of(DummyDataModel.class, new DummyDataModel()));
        softly.assertThat(map.models()).hasSize(1);
        softly.assertThat(map.get(DummyDataModel.class)).isNotNull();
    }

    @Test
    void invalidModel()
    {
        var map = new DataModelMap(new HashMap<>());
        assertThatThrownBy(() -> map.get(DummyDataModel.class))
                .isInstanceOf(IllegalStateException.class);
    }

    private static final class DummyDataModel
            implements DataModel
    {
        @Override
        public void refreshOnVaultChange()
        {
        }
    }
}
