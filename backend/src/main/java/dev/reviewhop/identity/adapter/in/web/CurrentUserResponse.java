package dev.reviewhop.identity.adapter.in.web;

import java.util.UUID;

record CurrentUserResponse(UUID id, String timezone) {
}
