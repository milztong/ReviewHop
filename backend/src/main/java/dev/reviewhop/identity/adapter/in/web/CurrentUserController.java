package dev.reviewhop.identity.adapter.in.web;

import dev.reviewhop.identity.api.CurrentAppUser;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
class CurrentUserController {

    private final CurrentAppUser currentAppUser;

    CurrentUserController(CurrentAppUser currentAppUser) {
        this.currentAppUser = currentAppUser;
    }

    @Operation(summary = "Get or provision the current ReviewHop user")
    @GetMapping("/me")
    CurrentUserResponse getCurrentUser() {
        var profile = currentAppUser.getOrCreate();
        return new CurrentUserResponse(profile.id().value(), profile.timezone().getId());
    }
}
