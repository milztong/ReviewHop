package dev.reviewhop.integration.adapter.in.web;

import dev.reviewhop.integration.application.ProviderConnectionNotFoundException;
import dev.reviewhop.integration.application.SyncRequestConflictException;
import dev.reviewhop.integration.application.SyncJobNotFoundException;
import dev.reviewhop.integration.application.port.out.ProviderFailure;
import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "dev.reviewhop.integration")
class ProviderApiExceptionHandler {

    @ExceptionHandler(ProviderConnectionNotFoundException.class)
    ProblemDetail connectionNotFound(ProviderConnectionNotFoundException exception) {
        return problem(HttpStatus.NOT_FOUND, "Provider connection not found", exception.getMessage());
    }

    @ExceptionHandler(SyncJobNotFoundException.class)
    ProblemDetail syncJobNotFound(SyncJobNotFoundException exception) {
        return problem(HttpStatus.NOT_FOUND, "Synchronization job not found", exception.getMessage());
    }

    @ExceptionHandler(SyncRequestConflictException.class)
    ProblemDetail syncConflict(SyncRequestConflictException exception) {
        return problem(HttpStatus.CONFLICT, "Synchronization conflict", exception.getMessage());
    }

    @ExceptionHandler(ProviderFailure.class)
    ProblemDetail providerFailure(ProviderFailure exception) {
        var status = switch (exception.code()) {
            case "PROFILE_NOT_FOUND" -> HttpStatus.NOT_FOUND;
            case "RATE_LIMITED" -> HttpStatus.TOO_MANY_REQUESTS;
            case "TIMEOUT", "NETWORK_ERROR", "PROVIDER_UNAVAILABLE" -> HttpStatus.SERVICE_UNAVAILABLE;
            default -> HttpStatus.BAD_GATEWAY;
        };
        var detail = problem(status, "Provider request failed", exception.getMessage());
        detail.setProperty("code", exception.code());
        detail.setProperty("retryable", exception.retryable());
        if (exception.retryAfter() != null) {
            detail.setProperty("retryAfterSeconds", exception.retryAfter().toSeconds());
        }
        return detail;
    }

    private static ProblemDetail problem(HttpStatus status, String title, String detail) {
        var problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        problem.setType(URI.create("https://ankileet.dev/problems/" + status.value()));
        return problem;
    }
}
