package dev.reviewhop.integration.application.port.out;

import dev.reviewhop.integration.domain.CompletedChallengePage;
import dev.reviewhop.integration.domain.Provider;
import dev.reviewhop.integration.domain.ProviderCapability;
import dev.reviewhop.integration.domain.ProviderProfile;
import java.util.Set;

public interface ProviderConnector {

    Provider provider();

    Set<ProviderCapability> capabilities();

    ProviderProfile resolvePublicProfile(String externalUserId);

    CompletedChallengePage fetchCompletedChallenges(String externalUserId, int pageNumber);
}
