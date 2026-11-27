package dev.reviewhop.privacy.application.port.out;

import dev.reviewhop.privacy.application.AccountDataExport;
import dev.reviewhop.privacy.application.DeletedAccountData;
import java.time.Instant;
import java.util.UUID;

public interface PrivacyDataStore {

    AccountDataExport export(UUID userId, Instant exportedAt);

    DeletedAccountData delete(UUID userId);
}
