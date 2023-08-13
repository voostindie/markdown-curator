package nl.ulso.hash;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;

public final class Hasher
{
    private static final String DIGEST_ALGORITHM = "SHA-256";
    private static final int HASH_LENGTH = 8;

    private Hasher()
    {
    }

    public static String hash(String input)
    {
        Objects.requireNonNull(input);
        try
        {
            var digest = MessageDigest.getInstance(DIGEST_ALGORITHM);
            return HexFormat.of().formatHex(digest.digest(input.getBytes())).substring(0,
                    HASH_LENGTH);
        }
        catch (NoSuchAlgorithmException e)
        {
            throw new IllegalArgumentException(
                    "No " + DIGEST_ALGORITHM + "? That's a required algorithm!");
        }
    }
}
