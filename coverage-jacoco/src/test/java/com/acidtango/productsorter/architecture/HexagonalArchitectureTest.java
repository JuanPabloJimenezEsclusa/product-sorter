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
  private static final String ADAPTER_REST = "com.acidtango.productsorter.adapter.rest..";
  private static final String INFRASTRUCTURE = "com.acidtango.productsorter.infrastructure..";

  private static final String[] COMMON = {
    "java..",
    "com.tngtech.archunit..",
    "org.junit..",
    "org.assertj..",
    "org.mockito.."
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
      "org.springframework..",
      "org.springframework.boot..",
      "org.springdoc..",
      "io.swagger..",
      "jakarta..",
      "com.acidtango.productsorter.api.."))
    .as("Adapter-rest dependencies must be whitelisted")
    .because("adapter-rest translates HTTP to use case calls");

  @ArchTest
  static final ArchRule infrastructureDependencies = classes()
    .that().resideInAPackage(INFRASTRUCTURE)
    .should().onlyDependOnClassesThat().resideInAnyPackage(concat(
      DOMAIN, APPLICATION, INFRASTRUCTURE,
      "org.springframework..",
      "org.springframework.boot..",
      "com.github.benmanes.caffeine..",
      "com.mongodb..",
      "io.micrometer.."))
    .as("Infrastructure dependencies must be whitelisted")
    .because("infrastructure adapters implement domain ports");

  private static String[] concat(final String... rest) {
    final var result = new String[COMMON.length + rest.length];
    System.arraycopy(COMMON, 0, result, 0, COMMON.length);
    System.arraycopy(rest, 0, result, COMMON.length, rest.length);
    return result;
  }
}
