package dev.reviewhop.integration.adapter.in.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

record ConnectCodewarsRequest(
        @NotBlank @Size(max = 255) String username) {
}
