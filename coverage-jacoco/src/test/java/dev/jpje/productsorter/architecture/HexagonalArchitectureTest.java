package dev.jpje.productsorter.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(packages = "dev.jpje.productsorter")
class HexagonalArchitectureTest {

  private static final String DOMAIN = "dev.jpje.productsorter.domain..";
  private static final String APPLICATION = "dev.jpje.productsorter.application..";
  private static final String API_SPEC = "dev.jpje.productsorter.api..";
  private static final String ADAPTER_REST = "dev.jpje.productsorter.adapter.rest..";
  private static final String ADAPTER_PERSISTENCE = "dev.jpje.productsorter.adapter.persistence..";
  private static final String ADAPTER_OBSERVABILITY = "dev.jpje.productsorter.adapter.observability..";
  private static final String[] COMMON = { "java.(io|lang|time|util).." };

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
      "..application..", "..adapter.rest..", "..adapter.persistence..")
    .as("Domain must not depend on application or adapters")
    .because("domain is the innermost layer");

  @ArchTest
  static final ArchRule applicationMustNotDependOnAdapters = noClasses()
    .that().resideInAPackage(APPLICATION)
    .should().dependOnClassesThat().resideInAnyPackage(
      "..adapter.rest..", "..adapter.persistence..")
    .allowEmptyShould(true)
    .as("Application must not depend on adapters")
    .because("application implements use cases independently of delivery mechanisms");

  @ArchTest
  static final ArchRule persistenceAdapterMustNotDependOnAdapterRest = noClasses()
    .that().resideInAPackage(ADAPTER_PERSISTENCE)
    .should().dependOnClassesThat().resideInAnyPackage("..adapter.rest..")
    .as("Persistence adapter must not depend on adapter-rest")
    .because("outbound adapters are independent of inbound adapters");

  @ArchTest
  static final ArchRule persistenceAdapterMustNotDependOnApplication = noClasses()
    .that().resideInAPackage(ADAPTER_PERSISTENCE)
    .should().dependOnClassesThat().resideInAnyPackage("..application..")
    .as("Persistence adapter must not depend on application")
    .because("outbound adapters implement domain ports, not application ports");

  @ArchTest
  static final ArchRule restAdapterMustNotDependOnPersistenceAdapter = noClasses()
    .that().resideInAPackage(ADAPTER_REST)
    .should().dependOnClassesThat().resideInAnyPackage("..adapter.persistence..")
    .as("REST adapter must not depend on persistence adapter")
    .because("inbound adapters don't need outbound adapter details");

  @ArchTest
  static final ArchRule persistenceEntitiesMustNotLeaveAdapter = noClasses()
    .that().resideInAPackage(ADAPTER_REST)
    .should().dependOnClassesThat().resideInAnyPackage(
      "..adapter.persistence.mongo.entity..")
    .as("REST adapter must not depend on persistence entities")
    .because("persistence entities are internal to the persistence adapter");

  @ArchTest
  static final ArchRule bootstrapMustNotDependOnDomainModel = noClasses()
    .that().resideInAPackage("dev.jpje.productsorter.config..")
    .should().dependOnClassesThat().resideInAnyPackage(
      "..domain.model..", "..domain.vo..")
    .allowEmptyShould(true)
    .as("Bootstrap config must not depend on domain model or value objects")
    .because("bootstrap is the composition root, not a business layer");

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
  static final ArchRule restAdapterDependencies = classes()
    .that().resideInAPackage(ADAPTER_REST)
    .should().onlyDependOnClassesThat().resideInAnyPackage(concat(
      DOMAIN, APPLICATION, API_SPEC, ADAPTER_REST,
      "org.springframework.(beans|context|core.convert|http|security|stereotype|web)..",
      "com.fasterxml.jackson.(databind|datatype)..",
      "jakarta.servlet..",
      "org.springdoc..",
      "io.swagger..",
      "io.micrometer..",
      "io.github.resilience4j..",
      "org.slf4j.."))
    .as("REST adapter dependencies must be whitelisted")
    .because("REST adapter translates HTTP to use case calls via Spring MVC and OAuth2");

  @ArchTest
  static final ArchRule apiSpecDependencies = classes()
    .that().resideInAPackage(API_SPEC)
    .should().onlyDependOnClassesThat().resideInAnyPackage(concat(
      API_SPEC,
      "org.springframework.(http|web|format|lang|validation)..",
      "jakarta.(annotation|validation)..",
      "io.swagger..",
      "com.fasterxml.jackson..",
      "org.openapitools.."))
    .as("API Spec dependencies must be whitelisted")
    .because("api-spec is a generated OpenAPI contract with framework annotations only");

  @ArchTest
  static final ArchRule observabilityAdapterDependencies = classes()
    .that().resideInAPackage(ADAPTER_OBSERVABILITY)
    .should().onlyDependOnClassesThat().resideInAnyPackage(concat(
      DOMAIN, APPLICATION, ADAPTER_OBSERVABILITY,
      "org.springframework.(beans|boot|context|core|stereotype|web)..",
      "jakarta.servlet..",
      "org.slf4j..",
      "io.micrometer..",
      "io.opentelemetry..",
      "jdk.jfr.consumer.."))
    .as("Observability adapter dependencies must be whitelisted")
    .because("observability adapter configures Micrometer, OpenTelemetry, and MDC logging");

  @ArchTest
  static final ArchRule persistenceAdapterDependencies = classes()
    .that().resideInAPackage(ADAPTER_PERSISTENCE)
    .should().onlyDependOnClassesThat().resideInAnyPackage(concat(
      DOMAIN, ADAPTER_PERSISTENCE,
      "org.springframework.(boot|cache|context|dao|data|stereotype|beans)..",
      "com.github.benmanes.caffeine..",
      "com.mongodb..",
      "org.bson..",
      "io.lettuce..",
      "io.micrometer..",
      "io.github.resilience4j..",
      "org.slf4j.."))
    .as("Persistence adapter dependencies must be whitelisted")
    .because("persistence adapter implements MongoDB repository with caching");

  private static String[] concat(final String... rest) {
    final var result = new String[COMMON.length + rest.length];
    System.arraycopy(COMMON, 0, result, 0, COMMON.length);
    System.arraycopy(rest, 0, result, COMMON.length, rest.length);
    return result;
  }
}
