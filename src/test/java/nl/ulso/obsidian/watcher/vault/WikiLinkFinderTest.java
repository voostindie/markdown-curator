package nl.ulso.obsidian.watcher.vault;

import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static nl.ulso.obsidian.watcher.vault.WikiLinkFinder.allLinks;

@ExtendWith(SoftAssertionsExtension.class)
class WikiLinkFinderTest
{
    @InjectSoftAssertions
    private SoftAssertions softly;

    @Test
    public void singleSimpleLink()
    {
        var matches = allLinks("A single [[link]] in a line");
        softly.assertThat(matches.size()).isEqualTo(1);
        var first = matches.get(0);
        softly.assertThat(first.group(1)).isEqualTo("link");
        softly.assertThat(first.group(2)).isNull();
        softly.assertThat(first.group(3)).isNull();
    }

    @Test
    public void multiLinksInText()
    {
        var matches = allLinks("Here are [[link1]] and [[link2]] in a single line");
        softly.assertThat(matches.size()).isEqualTo(2);
        softly.assertThat(matches.get(0).group(1)).isEqualTo("link1");
        softly.assertThat(matches.get(1).group(1)).isEqualTo("link2");
    }

    @Test
    public void linkWithAnchor()
    {
        var matches = allLinks("[[link#anchor]]");
        softly.assertThat(matches.size()).isEqualTo(1);
        var first = matches.get(0);
        softly.assertThat(first.group(1)).isEqualTo("link");
        softly.assertThat(first.group(2)).isEqualTo("anchor");
        softly.assertThat(first.group(3)).isNull();
    }

    @Test
    public void linkWithAlias()
    {
        var matches = allLinks("[[link|alias]]");
        softly.assertThat(matches.size()).isEqualTo(1);
        var first = matches.get(0);
        softly.assertThat(first.group(1)).isEqualTo("link");
        softly.assertThat(first.group(2)).isNull();
        softly.assertThat(first.group(3)).isEqualTo("alias");
    }

    @Test
    public void linkWithAnchorAndAlias()
    {
        var matches = allLinks("[[link#anchor|alias]]");
        System.out.println(matches.get(0).group(1));
        softly.assertThat(matches.size()).isEqualTo(1);
        var first = matches.get(0);
        softly.assertThat(first.group(1)).isEqualTo("link");
        softly.assertThat(first.group(2)).isEqualTo("anchor");
        softly.assertThat(first.group(3)).isEqualTo("alias");
    }
}
