package com.smartbatch360.desktop.api;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.smartbatch360.desktop.config.AppConfig;
import com.smartbatch360.desktop.server.EmbeddedServer;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

/**
 * Thin async JSON REST client built on {@link HttpClient}. Every call returns a
 * {@link CompletableFuture} - callers must never block the JavaFX Application
 * Thread on the result (docs/03_ARCHITECTURE.md, docs/05_CRUD_SPECIFICATION.md).
 * Non-2xx responses are translated into a user-friendly {@link ApiException}.
 *
 * Connection failures are retried briefly, but only while the embedded
 * backend is still booting - see {@link #shouldWaitForBackend}. The backend
 * starts in parallel with the UI and takes tens of seconds (Spring + Flyway +
 * Hibernate), so without this the first screen shown after launch reliably
 * rendered "Could not reach the SmartBatch360 server" until the user hit
 * Retry by hand. A genuinely unreachable server still fails fast.
 */
public class ApiClient {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String baseUrl;

    public ApiClient() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        this.baseUrl = AppConfig.get().apiBaseUrl();
    }

    public <T> CompletableFuture<List<T>> getList(String path, Class<T> elementType) {
        return send(newRequest(path).GET().build())
                .thenApply(body -> readList(body, elementType));
    }

    public <T> CompletableFuture<T> get(String path, Class<T> type) {
        return send(newRequest(path).GET().build())
                .thenApply(body -> read(body, type));
    }

    public <T> CompletableFuture<T> post(String path, Object requestBody, Class<T> responseType) {
        return send(newRequest(path)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofByteArray(writeBytes(requestBody)))
                .build())
                .thenApply(body -> read(body, responseType));
    }

    /** For action-style endpoints (e.g. batch controls) that take no request body. */
    public <T> CompletableFuture<T> postAction(String path, Class<T> responseType) {
        return send(newRequest(path).POST(HttpRequest.BodyPublishers.noBody()).build())
                .thenApply(body -> read(body, responseType));
    }

    public <T> CompletableFuture<T> put(String path, Object requestBody, Class<T> responseType) {
        return send(newRequest(path)
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofByteArray(writeBytes(requestBody)))
                .build())
                .thenApply(body -> read(body, responseType));
    }

    public CompletableFuture<Void> delete(String path) {
        return send(newRequest(path).DELETE().build()).thenApply(body -> null);
    }

    private HttpRequest.Builder newRequest(String path) {
        return HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .header("Accept", "application/json")
                .timeout(Duration.ofSeconds(15));
    }

    /** How long to keep re-trying while the embedded backend is still starting up. */
    private static final int STARTUP_RETRY_LIMIT = 40;
    private static final Duration STARTUP_RETRY_DELAY = Duration.ofMillis(750);

    private CompletableFuture<String> send(HttpRequest request) {
        return send(request, 0);
    }

    private CompletableFuture<String> send(HttpRequest request, int attempt) {
        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(this::bodyOrThrow)
                .exceptionallyCompose(throwable -> {
                    Throwable cause = throwable instanceof CompletionException ? throwable.getCause() : throwable;
                    if (shouldWaitForBackend(cause, attempt)) {
                        Executor later = CompletableFuture.delayedExecutor(
                                STARTUP_RETRY_DELAY.toMillis(), TimeUnit.MILLISECONDS);
                        return CompletableFuture.supplyAsync(() -> null, later)
                                .thenCompose(ignored -> send(request, attempt + 1));
                    }
                    return CompletableFuture.failedFuture(
                            cause instanceof ApiException ? cause : ApiException.network(cause));
                });
    }

    private String bodyOrThrow(HttpResponse<String> response) {
        int status = response.statusCode();
        if (status >= 200 && status < 300) {
            return response.body();
        }
        throw toApiException(status, response.body());
    }

    /**
     * True only for a transport-level failure that is explainable by the
     * backend not being up yet. An {@link ApiException} (the server answered,
     * just not with 2xx) is never retried, and neither is anything at all once
     * the embedded server reports itself running - at that point a connection
     * failure is real and the user should hear about it immediately.
     * (isStartupSettled() rather than isRunning(): the latter is synchronized
     * against the startup lock and would block here for the whole boot.)
     *
     * Requires a saved database config too: with none, the backend is never
     * going to start on its own and the right answer is the error state that
     * points at Settings, not a 30-second wait.
     */
    private boolean shouldWaitForBackend(Throwable cause, int attempt) {
        return !(cause instanceof ApiException)
                && attempt < STARTUP_RETRY_LIMIT
                && !EmbeddedServer.isStartupSettled()
                && EmbeddedServer.savedConfig().isPresent();
    }

    private ApiException toApiException(int status, String body) {
        try {
            ApiErrorDto error = objectMapper.readValue(body, ApiErrorDto.class);
            String message = error.message() != null ? error.message() : defaultMessageFor(status);
            return new ApiException(status, message, error.fieldErrors());
        } catch (IOException e) {
            return new ApiException(status, defaultMessageFor(status), List.of());
        }
    }

    private String defaultMessageFor(int status) {
        return switch (status) {
            case 400 -> "The submitted data is invalid.";
            case 404 -> "The requested record could not be found.";
            case 409 -> "This action conflicts with existing data.";
            default -> "Something went wrong while contacting the server. Please try again.";
        };
    }

    private <T> T read(String body, Class<T> type) {
        try {
            return objectMapper.readValue(body, type);
        } catch (IOException e) {
            throw new ApiException(0, "The server returned an unexpected response.", List.of());
        }
    }

    private <T> List<T> readList(String body, Class<T> elementType) {
        try {
            JavaType listType = objectMapper.getTypeFactory().constructCollectionType(List.class, elementType);
            return objectMapper.readValue(body, listType);
        } catch (IOException e) {
            throw new ApiException(0, "The server returned an unexpected response.", List.of());
        }
    }

    private byte[] writeBytes(Object value) {
        try {
            return objectMapper.writeValueAsBytes(value);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to serialize request body", e);
        }
    }
}
