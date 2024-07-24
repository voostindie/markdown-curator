package nl.ulso.markdown_curator.vault;

import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;

import static nl.ulso.markdown_curator.vault.Document.newDocument;

@ExtendWith(SoftAssertionsExtension.class)
class InternalLinkFinderTest
{
    @InjectSoftAssertions
    private SoftAssertions softly;

    @Test
    void singleSimpleLink()
    {
        var links = allLinks("A single [[link]] in a line");
        softly.assertThat(links.size()).isEqualTo(1);
        var first = links.getFirst();
        softly.assertThat(first.targetDocument()).isEqualTo("link");
        softly.assertThat(first.alias()).isNotPresent();
        softly.assertThat(first.targetAnchor()).isNotPresent();
    }

    @Test
    void multiLinksInText()
    {
        var links = allLinks("Here are [[link1]] and [[link2]] in a single line");
        softly.assertThat(links.size()).isEqualTo(2);
        softly.assertThat(links.get(0).targetDocument()).isEqualTo("link1");
        softly.assertThat(links.get(1).targetDocument()).isEqualTo("link2");
    }

    @Test
    void linkWithAnchor()
    {
        var links = allLinks("[[link#anchor]]");
        softly.assertThat(links.size()).isEqualTo(1);
        var first = links.getFirst();
        softly.assertThat(first.targetDocument()).isEqualTo("link");
        softly.assertThat(first.targetAnchor()).isPresent();
        softly.assertThat(first.targetAnchor().orElseThrow()).isEqualTo("anchor");
        softly.assertThat(first.alias()).isNotPresent();
    }

    @Test
    void linkWithAlias()
    {
        var links = allLinks("[[link|alias]]");
        softly.assertThat(links.size()).isEqualTo(1);
        var first = links.getFirst();
        softly.assertThat(first.targetDocument()).isEqualTo("link");
        softly.assertThat(first.targetAnchor()).isNotPresent();
        softly.assertThat(first.alias()).isPresent();
        softly.assertThat(first.alias()).get().isEqualTo("alias");
    }

    @Test
    void linkWithAnchorAndAlias()
    {
        var links = allLinks("[[link#anchor|alias]]");
        softly.assertThat(links.size()).isEqualTo(1);
        var first = links.getFirst();
        softly.assertThat(first.targetDocument()).isEqualTo("link");
        softly.assertThat(first.targetAnchor()).isPresent();
        softly.assertThat(first.targetAnchor().orElseThrow()).isEqualTo("anchor");
        softly.assertThat(first.alias()).isPresent();
        softly.assertThat(first.alias().orElseThrow()).isEqualTo("alias");
    }

    private List<InternalLink> allLinks(String content)
    {
        return newDocument("test", 0, content.lines().toList()).findInternalLinks();
    }
}
