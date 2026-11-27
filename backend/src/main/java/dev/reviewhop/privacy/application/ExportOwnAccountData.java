package dev.reviewhop.privacy.application;

import dev.reviewhop.identity.api.CurrentAppUser;
import dev.reviewhop.privacy.application.port.out.PrivacyAudit;
import dev.reviewhop.privacy.application.port.out.PrivacyDataStore;
import java.time.Clock;

public final class ExportOwnAccountData {

    private final CurrentAppUser currentAppUser;
    private final PrivacyDataStore dataStore;
    private final PrivacyAudit audit;
    private final Clock clock;

    public ExportOwnAccountData(
            CurrentAppUser currentAppUser,
            PrivacyDataStore dataStore,
            PrivacyAudit audit,
            Clock clock) {
        this.currentAppUser = currentAppUser;
        this.dataStore = dataStore;
        this.audit = audit;
        this.clock = clock;
    }

    public AccountDataExport execute() {
        var exportedAt = clock.instant();
        var user = currentAppUser.getOrCreate();
        var export = dataStore.export(user.id().value(), exportedAt);
        audit.accountDataExported();
        return export;
    }
}
