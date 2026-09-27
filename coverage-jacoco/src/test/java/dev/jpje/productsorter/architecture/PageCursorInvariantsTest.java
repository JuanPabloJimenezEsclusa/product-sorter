package dev.jpje.productsorter.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMembers;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import dev.jpje.productsorter.domain.port.ProductPage;

@AnalyzeClasses(packages = "dev.jpje.productsorter")
class PageCursorInvariantsTest {

  private static final String ADAPTER_PERSISTENCE = "dev.jpje.productsorter.adapter.persistence..";
  private static final String PRODUCT_PAGE_RESULT =
    "dev.jpje.productsorter.application.port.ProductPageResult";

  @ArchTest
  static final ArchRule adapterPageIsDomainOwned = classes()
    .that().haveSimpleName("ProductPage")
    .should().resideInAPackage("..domain.port")
    .as("The adapter-facing page is domain-owned")
    .because("the outbound port vocabulary lives in the domain");

  @ArchTest
  static final ArchRule clientTokenIsApplicationOwned = classes()
    .that().haveSimpleName("ProductPageResult")
    .should().resideInAPackage("..application.port")
    .as("The client-facing result is application-owned")
    .because("the continuation token belongs to the application, not the adapter");

  @ArchTest
  static final ArchRule adapterPageCarriesNoCursorMember = noMembers()
    .that().areDeclaredIn(ProductPage.class)
    .should().haveNameMatching(".*[Cc]ursor.*")
    .as("The adapter page declares no cursor member")
    .because("a client-facing token at the adapter boundary is the ownership defect being removed");

  @ArchTest
  static final ArchRule persistenceAdapterDoesNotDependOnClientResult = noClasses()
    .that().resideInAPackage(ADAPTER_PERSISTENCE)
    .should().dependOnClassesThat().haveFullyQualifiedName(PRODUCT_PAGE_RESULT)
    .as("The persistence adapter must not depend on the application-facing result")
    .because("the adapter's page output is not the source of the client-facing continuation token");
}
