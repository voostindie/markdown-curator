package nl.ulso.curator.server;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static nl.ulso.curator.server.Status.CLIENT_ERROR;
import static nl.ulso.curator.server.Status.NOT_FOUND;
import static nl.ulso.curator.server.Status.OK;
import static nl.ulso.curator.server.Status.SERVER_ERROR;
import static org.assertj.core.api.Assertions.assertThat;

class StatusTest
{
    @ParameterizedTest
    @CsvSource({
        "OK, 200",
        "CLIENT_ERROR, 400",
        "NOT_FOUND, 404",
        "SERVER_ERROR, 500"
    })
    void httpStatusCodesMatchExpectedValues(Status status, int expectedCode)
    {
        assertThat(status.httpStatusCode()).isEqualTo(expectedCode);
    }

    @Test
    void valueOfReturnsCorrectStatus()
    {
        assertThat(Status.valueOf("OK")).isEqualTo(OK);
        assertThat(Status.values()).containsExactly(
            OK,
            CLIENT_ERROR,
            NOT_FOUND,
            SERVER_ERROR
        );
    }
}
