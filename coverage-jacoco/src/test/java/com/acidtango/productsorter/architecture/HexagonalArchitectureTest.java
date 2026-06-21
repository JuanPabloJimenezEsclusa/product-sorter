package com.acidtango.productsorter.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(packages = "com.acidtango.productsorter")
class HexagonalArchitectureTest {

  private static final String DOMAIN = "com.acidtango.productsorter.domain..";
  private static final String APPLICATION = "com.acidtango.productsorter.application..";
  private static final String API_SPEC = "com.acidtango.productsorter.api..";
  private static final String ADAPTER_REST = "com.acidtango.productsorter.adapter.rest..";
  private static final String INFRASTRUCTURE = "com.acidtango.productsorter.infrastructure.(persistence|cache|config)..";
  private static final String OBSERVABILITY = "com.acidtango.productsorter.infrastructure.observability..";

  private static final String[] COMMON = {
    "java..",
    "com.tngtech.archunit..",
    "org.junit..",
    "org.assertj..",
    "org.mockito..",
    "org.springframework.boot..",
    "org.springframework.test.."
  };

  @ArchTest
  static final ArchRule domainMustNotDependOnSpring = noClasses()
    .that().resideInAPackage(DOMAIN)
    .should().dependOnClassesThat().resideInAnyPackage("org.springframework..")
    .as("Domain must not depend on Spring")
    .because("domain is pure Java with zero framework imports");

  @ArchTest
  static final ArchRule domainMustNotDependOnOuterLayers = noClasses()
    .that().resideInAPackage(DOMAIN)
    .should().dependOnClassesThat().resideInAnyPackage(
      "..application..", "..adapter.rest..", "..infrastructure..")
    .as("Domain must not depend on application, adapter, or infrastructure")
    .because("domain is the innermost layer");

  @ArchTest
  static final ArchRule applicationMustNotDependOnInfrastructureOrAdapter = noClasses()
    .that().resideInAPackage(APPLICATION)
    .should().dependOnClassesThat().resideInAnyPackage(
      "..adapter.rest..", "..infrastructure..")
    .allowEmptyShould(true)
    .as("Application must not depend on adapter or infrastructure")
    .because("application implements use cases independently of delivery mechanisms");

  @ArchTest
  static final ArchRule infrastructureMustNotDependOnAdapterRest = noClasses()
    .that().resideInAPackage(INFRASTRUCTURE)
    .should().dependOnClassesThat().resideInAnyPackage("..adapter.rest..")
    .as("Infrastructure must not depend on adapter-rest")
    .because("outbound adapters are independent of inbound adapters");

  @ArchTest
  static final ArchRule infrastructureMustNotDependOnApplication = noClasses()
    .that().resideInAPackage(INFRASTRUCTURE)
    .should().dependOnClassesThat().resideInAnyPackage("..application..")
    .as("Infrastructure must not depend on application")
    .because("outbound adapter implementations only depend on domain ports");

  @ArchTest
  static final ArchRule adapterRestMustNotDependOnInfrastructure = noClasses()
    .that().resideInAPackage(ADAPTER_REST)
    .should().dependOnClassesThat().resideInAnyPackage("..infrastructure..")
    .as("Adapter-rest must not depend on infrastructure")
    .because("inbound adapters don't need outbound adapter details");

  @ArchTest
  static final ArchRule domainDependencies = classes()
    .that().resideInAPackage(DOMAIN)
    .should().onlyDependOnClassesThat().resideInAnyPackage(concat(DOMAIN))
    .as("Domain module must only depend on itself")
    .because("domain is pure Java");

  @ArchTest
  static final ArchRule applicationDependencies = classes()
    .that().resideInAPackage(APPLICATION)
    .should().onlyDependOnClassesThat().resideInAnyPackage(concat(DOMAIN, APPLICATION))
    .as("Application module dependencies")
    .because("application depends on domain types and standard libraries");

  @ArchTest
  static final ArchRule adapterRestDependencies = classes()
    .that().resideInAPackage(ADAPTER_REST)
    .should().onlyDependOnClassesThat().resideInAnyPackage(concat(
      DOMAIN, APPLICATION, ADAPTER_REST,
      "org.springframework.context..",
      "org.springframework.http..",
      "org.springframework.security..",
      "org.springframework.web..",
      "org.springdoc..",
      "io.swagger..",
      "jakarta..",
      "org.slf4j..",
      "com.acidtango.productsorter.api.."))
    .as("Adapter-rest dependencies must be whitelisted")
    .because("adapter-rest translates HTTP to use case calls via Spring MVC and OAuth2");

  @ArchTest
  static final ArchRule apiSpecDependencies = classes()
    .that().resideInAPackage(API_SPEC)
    .should().onlyDependOnClassesThat().resideInAnyPackage(concat(
      API_SPEC,
      "org.springframework.http..",
      "org.springframework.web..",
      "org.springframework.format..",
      "org.springframework.lang..",
      "org.springframework.validation..",
      "io.swagger..",
      "jakarta..",
      "com.fasterxml.jackson..",
      "org.openapitools.."))
    .as("API Spec dependencies must be whitelisted")
    .because("api-spec is a generated OpenAPI contract with framework annotations only");

  @ArchTest
  static final ArchRule observabilityDependencies = classes()
    .that().resideInAPackage(OBSERVABILITY)
    .should().onlyDependOnClassesThat().resideInAnyPackage(concat(
      DOMAIN, OBSERVABILITY,
      "org.springframework..",
      "org.springframework.boot..",
      "io.micrometer..",
      "jakarta..",
      "org.slf4j.."))
    .as("Observability dependencies must be whitelisted")
    .because("observability configures Micrometer, OpenTelemetry, and MDC logging");

  @ArchTest
  static final ArchRule infrastructureDependencies = classes()
    .that().resideInAPackage(INFRASTRUCTURE)
    .should().onlyDependOnClassesThat().resideInAnyPackage(concat(
      DOMAIN, INFRASTRUCTURE,
      "org.springframework.cache..",
      "org.springframework.context..",
      "org.springframework.data..",
      "org.springframework.stereotype..",
      "com.github.benmanes.caffeine..",
      "com.mongodb..",
      "io.micrometer.."))
    .as("Infrastructure dependencies must be whitelisted")
    .because("infrastructure adapters implement persistence, caching, and observability");

  private static String[] concat(final String... rest) {
    final var result = new String[COMMON.length + rest.length];
    System.arraycopy(COMMON, 0, result, 0, COMMON.length);
    System.arraycopy(rest, 0, result, COMMON.length, rest.length);
    return result;
  }
}
