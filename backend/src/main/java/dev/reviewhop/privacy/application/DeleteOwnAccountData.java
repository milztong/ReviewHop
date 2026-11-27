package dev.reviewhop.privacy.application;

import dev.reviewhop.identity.api.CurrentAppUser;
import dev.reviewhop.privacy.application.port.out.PrivacyAudit;
import dev.reviewhop.privacy.application.port.out.PrivacyDataStore;

public final class DeleteOwnAccountData {

    private final CurrentAppUser currentAppUser;
    private final PrivacyDataStore dataStore;
    private final PrivacyAudit audit;

    public DeleteOwnAccountData(
            CurrentAppUser currentAppUser,
            PrivacyDataStore dataStore,
            PrivacyAudit audit) {
        this.currentAppUser = currentAppUser;
        this.dataStore = dataStore;
        this.audit = audit;
    }

    public void execute() {
        currentAppUser.findExisting().ifPresent(user -> {
            var deleted = dataStore.delete(user.id().value());
            if (deleted.appUsers() == 1) {
                audit.accountDataDeleted(deleted);
            }
        });
    }
}
