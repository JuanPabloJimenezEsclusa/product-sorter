package com.acidtango.productsorter.testdata;

import java.io.File;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.random.RandomGenerator;

import com.fasterxml.jackson.databind.ObjectMapper;

public class ProductDataGenerator {

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

  public void generate(final int count, final File output) throws Exception {
    try (final var writer = new PrintWriter(output, StandardCharsets.UTF_8)) {
      for (int i = 1; i <= count; i++) {
        final var product = mapper.createObjectNode();
        product.put("id", String.valueOf(i));
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

  static void main(final String[] args) throws Exception {
    if (args.length < 2) {
      System.err.println("Usage: ProductDataGenerator <count> <output.json> [--seed <n>]");
      System.exit(1);
    }

    final var count = Integer.parseInt(args[0]);
    final var output = new File(args[1]);
    final var seed = args.length >= 4 && "--seed".equals(args[2])
      ? Long.parseLong(args[3]) : 0L;

    output.getParentFile().mkdirs();
    final var generator = new ProductDataGenerator();
    generator.generate(count, output);
    System.out.println("Generated " + count + " products to " + output.getAbsolutePath());
  }
}
