package nl.ulso.curator.addon.journal;

import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import static nl.ulso.curator.addon.journal.Outline.newOutline;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SoftAssertionsExtension.class)
class OutlineTest
{
    @ParameterizedTest
    @MethodSource("provideTrees")
    void buildAndCheckTree(String treeLines, String expected)
    {
        var treeInfo = newOutline(treeLines.trim().lines().toList()).toLineValues();
        var computedString = treeInfo.toString();
        var expectedString = "[" + expected.trim().lines().collect(Collectors.joining(", ")) + "]";
        assertThat(computedString).isEqualTo(expectedString);
    }

    public static Stream<Arguments> provideTrees()
    {
        return Stream.of(
                Arguments.of("", ""),
                Arguments.of("""
                        - Node
                        """, """
                        (1, 2) at depth 1
                        """),
                Arguments.of("""
                        - Node
                            - Node
                            - Node
                                - Node
                        - Node
                            - Node
                        """, """
                        (1, 8) at depth 1
                        (2, 3) at depth 2
                        (4, 7) at depth 2
                        (5, 6) at depth 3
                        (9, 12) at depth 1
                        (10, 11) at depth 2
                        """),
                Arguments.of("""
                        ## Section
                        
                        Paragraph
                        
                        Another paragraph
                        """, """
                        (1, 2) at depth 1
                        (3, 4) at depth 1
                        (5, 6) at depth 1
                        (7, 8) at depth 1
                        (9, 10) at depth 1
                        """),
                Arguments.of("""
                        - Node
                        \t- Node
                        \t    - Node
                        \t\t- Node
                        """, """
                        (1, 8) at depth 1
                        (2, 7) at depth 2
                        (3, 4) at depth 3
                        (5, 6) at depth 3
                        """)
        );
    }
}
