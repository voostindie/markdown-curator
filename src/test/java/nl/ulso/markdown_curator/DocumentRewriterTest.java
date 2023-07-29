package nl.ulso.markdown_curator;

import nl.ulso.markdown_curator.vault.VaultStub;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Collections;
import java.util.stream.Stream;

import static nl.ulso.markdown_curator.DocumentRewriter.rewriteDocument;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SoftAssertionsExtension.class)
class DocumentRewriterTest
{
    @ParameterizedTest
    @MethodSource("provideDocuments")
    void writeDocuments(String expected)
    {
        var vault = new VaultStub();
        var document = vault.addDocument("test", expected);
        var content = rewriteDocument(document, Collections.emptyList());
        assertThat(content).isEqualTo(expected);
    }

    public static Stream<Arguments> provideDocuments()
    {
        return Stream.of(
                Arguments.of("""
                        One liner
                        """),
                Arguments.of("""
                        
                        
                        Lots of blank lines
                        
                        
                        """),
                Arguments.of("""
                        ---
                        front: matter
                        ---
                        # Title
                        
                        ## Section 1
                        ### Subsection 1.1
                        
                        Lorem ipsum
                        
                        ### Subsection 1.2
                        With text close together to
                        ### ...Subsection 1.3
                        
                        
                        
                        ...and then nothing for a while...
                        
                        
                        
                        ## Section 2
                        
                        Code sample:
                        
                        ```java
                        public static void main(String[] arguments) {
                            System.out.println("Hello, world!");
                        }
                        ```
                        
                        Seems to work!
                        """)
        );
    }
}
