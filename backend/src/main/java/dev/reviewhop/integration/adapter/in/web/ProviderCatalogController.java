package dev.reviewhop.integration.adapter.in.web;

import dev.reviewhop.integration.application.ListProviderCatalog;
import dev.reviewhop.integration.application.ProviderDescriptorView;
import io.swagger.v3.oas.annotations.Operation;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/providers")
class ProviderCatalogController {

    private final ListProviderCatalog listProviderCatalog;

    ProviderCatalogController(ListProviderCatalog listProviderCatalog) {
        this.listProviderCatalog = listProviderCatalog;
    }

    @Operation(summary = "List available providers and their integration capabilities")
    @GetMapping
    List<ProviderDescriptorView> list() {
        return listProviderCatalog.execute();
    }
}
