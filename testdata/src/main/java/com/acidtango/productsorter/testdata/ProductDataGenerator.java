package com.acidtango.productsorter.testdata;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.random.RandomGenerator;

import com.fasterxml.jackson.databind.ObjectMapper;

public class ProductDataGenerator {

  private static final Logger log = Logger.getLogger(ProductDataGenerator.class.getName());

  private final RealisticProductNames names;
  private final SalesDistribution sales;
  private final StockAllocator stock;
  private final ObjectMapper mapper;

  public ProductDataGenerator() {
    final var rng = RandomGenerator.of("L64X128MixRandom");
    this.names = new RealisticProductNames(rng);
    this.sales = new SalesDistribution(rng);
    this.stock = new StockAllocator(rng);
    this.mapper = new ObjectMapper();
  }

  public void generate(final int count, final File output) throws IOException {
    try (final var writer = new PrintWriter(output, StandardCharsets.UTF_8)) {
      for (int i = 1; i <= count; i++) {
        final var product = mapper.createObjectNode();
        product.put("_id", UUID.randomUUID().toString());
        product.put("name", names.sample());
        product.put("salesUnits", sales.sample());

        final var stockEntries = mapper.createArrayNode();
        final var alloc = stock.allocate();
        alloc.forEach((size, qty) -> {
          final var entry = mapper.createObjectNode();
          entry.put("size", size);
          entry.put("quantity", qty);
          stockEntries.add(entry);
        });
        product.set("stock", stockEntries);

        writer.println(mapper.writeValueAsString(product));
      }
    }
  }

  static void main(final String[] args) throws IOException {
    if (args.length < 2) {
      log.severe("Usage: ProductDataGenerator <count> <output.json>");
      System.exit(1);
    }

    final var count = Integer.parseInt(args[0]);
    final var output = Path.of(args[1]).normalize().toFile();

    if (output.getParentFile() != null && !output.getParentFile().exists()
        && !output.getParentFile().mkdirs()) {
      throw new IOException("Failed to create output directory: " + output.getParentFile().getAbsolutePath());
    }
    final var generator = new ProductDataGenerator();
    generator.generate(count, output);
    log.log(Level.INFO, "Generated {0} products to {1}", new Object[] { count, output.getCanonicalPath() });
  }
}
