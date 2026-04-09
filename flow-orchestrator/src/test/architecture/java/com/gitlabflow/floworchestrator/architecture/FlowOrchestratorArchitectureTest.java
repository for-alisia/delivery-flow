package com.gitlabflow.floworchestrator.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RestController;

@AnalyzeClasses(
        packages = "com.gitlabflow.floworchestrator",
        importOptions = {ImportOption.DoNotIncludeTests.class, ImportOption.DoNotIncludeJars.class})
class FlowOrchestratorArchitectureTest {

    // --- Naming and placement rules ---

    @ArchTest
    static final ArchRule controllers_should_reside_in_rest_package_and_use_restcontroller_annotation = classes()
            .that()
            .haveSimpleNameEndingWith("Controller")
            .should()
            .resideInAPackage("..orchestration..rest..")
            .andShould()
            .beAnnotatedWith(RestController.class);

    @ArchTest
    static final ArchRule ports_should_be_interfaces_in_orchestration_packages = classes()
            .that()
            .haveSimpleNameEndingWith("Port")
            .should()
            .beInterfaces()
            .andShould()
            .resideInAPackage("..orchestration..");

    @ArchTest
    static final ArchRule services_should_live_in_orchestration_and_not_depend_on_provider_dtos = classes()
            .that()
            .haveSimpleNameEndingWith("Service")
            .should()
            .resideInAPackage("..orchestration..")
            .andShould()
            .beAnnotatedWith(Service.class)
            .andShould()
            .onlyDependOnClassesThat()
            .resideOutsideOfPackage("..integration..dto..");

    @ArchTest
    static final ArchRule configuration_properties_should_reside_in_config_package = classes()
            .that()
            .areAnnotatedWith(ConfigurationProperties.class)
            .should()
            .resideInAPackage("..config..");

    // --- Dependency boundary rules ---

    @ArchTest
    static final ArchRule adapters_should_not_depend_on_rest_models = noClasses()
            .that()
            .haveSimpleNameEndingWith("Adapter")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("..orchestration..rest..", "..orchestration..rest.dto..");

    @ArchTest
    static final ArchRule adapters_should_live_in_integration_and_be_components = classes()
            .that()
            .haveSimpleNameEndingWith("Adapter")
            .should()
            .resideInAPackage("..integration..")
            .andShould()
            .beAnnotatedWith(Component.class);

    @ArchTest
    static final ArchRule integration_should_not_depend_on_rest_layer = noClasses()
            .that()
            .resideInAPackage("..integration..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("..orchestration..rest..", "..orchestration..rest.dto..");

    // --- Injection rules ---

    @ArchTest
    static final ArchRule no_field_injection = noFields()
            .should()
            .beAnnotatedWith(Autowired.class)
            .as("Field injection is not allowed — use constructor injection via @RequiredArgsConstructor");

    // --- Cycle detection ---

    @ArchTest
    static final ArchRule no_cycles_between_capability_packages = slices().matching(
                    "com.gitlabflow.floworchestrator.orchestration.(*)..")
            .should()
            .beFreeOfCycles();

    @ArchTest
    static final ArchRule no_cycles_between_top_level_packages =
            slices().matching("com.gitlabflow.floworchestrator.(*)..").should().beFreeOfCycles();
}
