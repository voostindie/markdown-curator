package nl.ulso.hash;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.function.UnaryOperator;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNull;

/// Generates short hashes from string input using SHA-256, formatting it as a hexadecimal string
/// and taking the first couple of characters.
///
/// The goal of this class is **not** to create real hashes! The goal is to create a short, stable
/// signature of a [String] input with a high probability to change if the input changes. Do not use
/// these hashes as keys!
public final class ShortHasher
    implements UnaryOperator<String>
{
    private static final String DIGEST_ALGORITHM = "SHA-256";
    private static final int HASH_LENGTH = 8;

    @Override
    public String apply(String input)
    {
        return shortHashOf(input);
    }

    public static String shortHashOf(String input)
    {
        requireNonNull(input);
        try
        {
            var digest = MessageDigest.getInstance(DIGEST_ALGORITHM).digest(input.getBytes(UTF_8));
            return HexFormat.of().formatHex(digest).substring(0, HASH_LENGTH);
        }
        catch (NoSuchAlgorithmException _)
        {
            throw new IllegalArgumentException(
                "No " + DIGEST_ALGORITHM + "? That's a required algorithm!");
        }
    }
}
