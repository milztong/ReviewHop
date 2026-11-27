package dev.reviewhop;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class ArchitectureTest {

    private static final String BASE_PACKAGE = "dev.reviewhop";

    @Test
    void moduleStructureIsValid() {
        ApplicationModules.of(ReviewHopApplication.class).verify();
    }

    @Test
    void domainDoesNotDependOnFrameworks() {
        var classes = new ClassFileImporter().importPackages(BASE_PACKAGE);

        noClasses()
                .that().resideInAPackage("..domain..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "org.springframework..",
                        "jakarta.persistence..",
                        "software.amazon.awssdk..",
                        "io.swagger.v3..",
                        "io.github.openspacedrepetition..")
                .check(classes);

        noClasses()
                .that().resideOutsideOfPackage("..learning.adapter.out.scheduling..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "io.github.openspacedrepetition..")
                .check(classes);
    }
}
