package com.smartbatch360.desktop.api;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.smartbatch360.desktop.config.AppConfig;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Thin async JSON REST client built on {@link HttpClient}. Every call returns a
 * {@link CompletableFuture} - callers must never block the JavaFX Application
 * Thread on the result (docs/03_ARCHITECTURE.md, docs/05_CRUD_SPECIFICATION.md).
 * Non-2xx responses are translated into a user-friendly {@link ApiException}.
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

    private CompletableFuture<String> send(HttpRequest request) {
        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .handle((response, throwable) -> {
                    if (throwable != null) {
                        throw ApiException.network(throwable);
                    }
                    int status = response.statusCode();
                    if (status >= 200 && status < 300) {
                        return response.body();
                    }
                    throw toApiException(status, response.body());
                });
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
