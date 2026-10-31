package com.smartbatch360.desktop.api;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Against a real HTTP server, so what is tested is what the client does with
 * a real reply - an empty 204 in particular, which is where it went wrong.
 */
class ApiClientTest {

    private HttpServer server;
    private ApiClient client;

    @BeforeEach
    void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/no-content", exchange -> {
            exchange.getRequestBody().readAllBytes();
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
        });
        server.createContext("/json", exchange -> {
            exchange.getRequestBody().readAllBytes();
            byte[] body = "{\"name\":\"Kharadi\"}".getBytes();
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        client = new ApiClient("http://localhost:" + server.getAddress().getPort());
    }

    @AfterEach
    void stop() {
        server.stop(0);
    }

    /**
     * The logo upload answers 204 with no body. Read as JSON that failed, so a
     * logo that had been saved was reported to the user as not uploaded.
     */
    @Test
    void anEmptyReplyToACallExpectingNothingIsASuccess() throws Exception {
        Void result = client.put("/no-content", Map.of("contentType", "image/png"), Void.class)
                .get(5, TimeUnit.SECONDS);

        assertThat(result).isNull();
    }

    @Test
    void aReplyThatIsExpectedIsStillRead() throws Exception {
        @SuppressWarnings("unchecked")
        Map<String, Object> result = client.put("/json", Map.of(), Map.class).get(5, TimeUnit.SECONDS);

        assertThat(result).containsEntry("name", "Kharadi");
    }

    /** An empty reply where data was expected is still an error, not a silent null. */
    @Test
    void anEmptyReplyWhereDataWasExpectedIsStillAnError() {
        assertThatThrownBy(() -> client.put("/no-content", Map.of(), Map.class).get(5, TimeUnit.SECONDS))
                .hasCauseInstanceOf(ApiException.class)
                .hasMessageContaining("unexpected response");
    }
}
