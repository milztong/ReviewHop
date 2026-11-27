package dev.reviewhop.identity.api;

import java.time.ZoneId;

public record AppUserProfile(AppUserId id, ZoneId timezone, double desiredRetention) {
}
