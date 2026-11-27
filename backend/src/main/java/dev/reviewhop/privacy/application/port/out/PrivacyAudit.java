package dev.reviewhop.privacy.application.port.out;

import dev.reviewhop.privacy.application.DeletedAccountData;

public interface PrivacyAudit {

    void accountDataExported();

    void accountDataDeleted(DeletedAccountData deleted);
}
