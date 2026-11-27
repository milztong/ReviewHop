package dev.reviewhop.privacy.adapter.in.web;

import dev.reviewhop.privacy.application.AccountDataExport;
import dev.reviewhop.privacy.application.DeleteOwnAccountData;
import dev.reviewhop.privacy.application.ExportOwnAccountData;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users/me")
class PrivacyController {

    private final ExportOwnAccountData exportOwnAccountData;
    private final DeleteOwnAccountData deleteOwnAccountData;

    PrivacyController(
            ExportOwnAccountData exportOwnAccountData,
            DeleteOwnAccountData deleteOwnAccountData) {
        this.exportOwnAccountData = exportOwnAccountData;
        this.deleteOwnAccountData = deleteOwnAccountData;
    }

    @Operation(summary = "Export the current user's ReviewHop data as JSON")
    @GetMapping(value = "/export", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<AccountDataExport> exportCurrentUserData() {
        var contentDisposition = ContentDisposition.attachment()
                .filename("reviewhop-data-export.json")
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
                .body(exportOwnAccountData.execute());
    }

    @Operation(summary = "Permanently delete the current user's ReviewHop data")
    @DeleteMapping
    ResponseEntity<Void> deleteCurrentUserData(
            @RequestHeader("X-Account-Deletion-Confirmation") String confirmation) {
        if (!"DELETE".equals(confirmation)) {
            throw new IllegalArgumentException("Account deletion confirmation is invalid");
        }
        deleteOwnAccountData.execute();
        return ResponseEntity.noContent().build();
    }
}
