package dev.reviewhop.integration.application;

import dev.reviewhop.integration.domain.Provider;
import dev.reviewhop.integration.domain.ProviderCapability;
import java.util.Set;

public record ProviderDescriptorView(
        Provider provider,
        Set<ProviderCapability> capabilities) {

    public ProviderDescriptorView {
        capabilities = Set.copyOf(capabilities);
    }
}
