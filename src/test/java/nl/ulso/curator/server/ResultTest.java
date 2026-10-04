package nl.ulso.curator.server;

import org.junit.jupiter.api.Test;

import static nl.ulso.curator.query.OutputFormat.JSON;
import static nl.ulso.curator.query.OutputFormat.MARKDOWN;
import static nl.ulso.curator.server.Result.clientError;
import static nl.ulso.curator.server.Result.ok;
import static nl.ulso.curator.server.Status.CLIENT_ERROR;
import static nl.ulso.curator.server.Status.OK;
import static org.assertj.core.api.Assertions.assertThat;

class ResultTest
{
    @Test
    void okResultContainsStatusOkAndOutput()
    {
        var result = ok(JSON, "{\"status\":\"success\"}");
        assertThat(result.status()).isEqualTo(OK);
        assertThat(result.outputFormat()).isEqualTo(JSON);
        assertThat(result.output()).contains("{\"status\":\"success\"}");
    }

    @Test
    void clientErrorResultContainsClientErrorStatusAndMarkdownOutput()
    {
        var result = clientError("Invalid request parameter");
        assertThat(result.status()).isEqualTo(CLIENT_ERROR);
        assertThat(result.outputFormat()).isEqualTo(MARKDOWN);
        assertThat(result.output()).contains("Invalid request parameter");
    }

    @Test
    void notFoundResultContainsNotFoundStatusAndMarkdownOutput()
    {
        var result = Result.notFound("Resource not found");
        assertThat(result.status()).isEqualTo(Status.NOT_FOUND);
        assertThat(result.outputFormat()).isEqualTo(MARKDOWN);
        assertThat(result.output()).contains("Resource not found");
    }

    @Test
    void serverErrorResultContainsServerErrorStatusAndMarkdownOutput()
    {
        var result = Result.serverError("Internal server error occurred");
        assertThat(result.status()).isEqualTo(Status.SERVER_ERROR);
        assertThat(result.outputFormat()).isEqualTo(MARKDOWN);
        assertThat(result.output()).contains("Internal server error occurred");
    }
}
