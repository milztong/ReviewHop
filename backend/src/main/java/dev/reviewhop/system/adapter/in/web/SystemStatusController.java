package dev.reviewhop.system.adapter.in.web;

import dev.reviewhop.system.application.GetSystemStatus;
import dev.reviewhop.system.domain.DatabaseStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/system")
class SystemStatusController {

    private final GetSystemStatus getSystemStatus;

    SystemStatusController(GetSystemStatus getSystemStatus) {
        this.getSystemStatus = getSystemStatus;
    }

    @Operation(
            summary = "Get the application status",
            description = "Verifies that the API can read the Flyway-managed system marker.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Application and database are ready"),
            @ApiResponse(responseCode = "503", description = "The database is unavailable")
    })
    @GetMapping("/status")
    ResponseEntity<SystemStatusResponse> getStatus() {
        var status = getSystemStatus.execute();
        var response = SystemStatusResponse.from(status);

        return status.database() == DatabaseStatus.READY
                ? ResponseEntity.ok(response)
                : ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }
}
