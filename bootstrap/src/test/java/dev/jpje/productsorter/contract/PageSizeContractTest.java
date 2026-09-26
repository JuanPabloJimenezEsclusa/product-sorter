package dev.jpje.productsorter.contract;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import dev.jpje.productsorter.application.port.PageSize;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

/**
 * Pins the runtime page-size bounds to the bounds declared in the OpenAPI contract, so the
 * implementation and the published contract cannot drift apart.
 */
class PageSizeContractTest {

  private static final String SPEC = "openapi/product-sorter-v1.yaml";

  @Test
  void pageSizeBoundsMustMatchTheOpenApiContract() {
    final var paths = specPaths();

    assertSizeContract(paths, "/products", "get");
    assertSizeContract(paths, "/products/sort", "post");
  }

  @SuppressWarnings("unchecked")
  private static void assertSizeContract(final Map<String, Object> paths,
                                         final String path, final String method) {
    final var operation = (Map<String, Object>) ((Map<String, Object>) paths.get(path)).get(method);
    final var parameters = (List<Map<String, Object>>) operation.get("parameters");
    final var schema = (Map<String, Object>) parameters.stream()
      .filter(parameter -> "size".equals(parameter.get("name")))
      .findFirst()
      .orElseThrow()
      .get("schema");

    assertThat(((Number) schema.get("minimum")).intValue())
      .as("OpenAPI minimum for %s %s", method.toUpperCase(), path).isEqualTo(PageSize.MIN);
    assertThat(((Number) schema.get("maximum")).intValue())
      .as("OpenAPI maximum for %s %s", method.toUpperCase(), path).isEqualTo(PageSize.MAX);
    assertThat(((Number) schema.get("default")).intValue())
      .as("OpenAPI default for %s %s", method.toUpperCase(), path).isEqualTo(PageSize.DEFAULT);
  }

  @SuppressWarnings("unchecked")
  private static Map<String, Object> specPaths() {
    try (InputStream in = PageSizeContractTest.class.getClassLoader().getResourceAsStream(SPEC)) {
      assertThat(in).as("OpenAPI spec is on the classpath at %s", SPEC).isNotNull();
      final Map<String, Object> spec = new Yaml().load(in);
      return (Map<String, Object>) spec.get("paths");
    } catch (final Exception e) {
      throw new IllegalStateException("Failed to read " + SPEC, e);
    }
  }
}
