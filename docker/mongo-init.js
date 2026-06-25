db = db.getSiblingDB("productsorter");

db.products.insertMany([
  { _id: "550e8400-e29b-41d4-a716-446655440000", name: "V-NECH BASIC SHIRT", salesUnits: 100, stock: [{ size: "S", quantity: 4 }, { size: "M", quantity: 9 }, { size: "L", quantity: 0 }], stockRatio: 2 / 3 },
  { _id: "6fa459ea-ee8a-3ca5-8b4a-22e3b2a5b4c6", name: "CONTRASTING FABRIC T-SHIRT", salesUnits: 50, stock: [{ size: "S", quantity: 35 }, { size: "M", quantity: 9 }, { size: "L", quantity: 9 }], stockRatio: 1 },
  { _id: "7c9e6679-7425-40de-944b-e07fc1f90ae7", name: "RAISED PRINT T-SHIRT", salesUnits: 80, stock: [{ size: "S", quantity: 20 }, { size: "M", quantity: 2 }, { size: "L", quantity: 20 }], stockRatio: 1 },
  { _id: "8a4b5c6d-7e8f-9a0b-1c2d-3e4f5a6b7c8d", name: "PLEATED T-SHIRT", salesUnits: 3, stock: [{ size: "S", quantity: 25 }, { size: "M", quantity: 30 }, { size: "L", quantity: 10 }], stockRatio: 1 },
  { _id: "9b0c1d2e-3f4a-5b6c-7d8e-9f0a1b2c3d4e", name: "CONTRASTING LACE T-SHIRT", salesUnits: 650, stock: [{ size: "S", quantity: 0 }, { size: "M", quantity: 1 }, { size: "L", quantity: 0 }], stockRatio: 1 / 3 },
  { _id: "a1b2c3d4-e5f6-7890-abcd-ef1234567890", name: "SLOGAN T-SHIRT", salesUnits: 20, stock: [{ size: "S", quantity: 9 }, { size: "M", quantity: 2 }, { size: "L", quantity: 5 }], stockRatio: 1 }
]);

db.products.createIndex({ salesUnits: -1, _id: -1 });

print("Seeded 6 products, created index {salesUnits: -1, _id: -1}");
