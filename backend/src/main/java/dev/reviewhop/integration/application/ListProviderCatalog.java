package dev.reviewhop.integration.application;

import dev.reviewhop.integration.application.port.out.ProviderConnector;
import java.util.List;

public final class ListProviderCatalog {

    private final List<ProviderConnector> connectors;

    public ListProviderCatalog(List<ProviderConnector> connectors) {
        this.connectors = List.copyOf(connectors);
    }

    public List<ProviderDescriptorView> execute() {
        return connectors.stream()
                .map(connector -> new ProviderDescriptorView(
                        connector.provider(), connector.capabilities()))
                .toList();
    }
}
