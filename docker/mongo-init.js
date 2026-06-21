db = db.getSiblingDB("productsorter");

db.products.insertMany([
  { _id: "1", name: "V-NECK BASIC SHIRT", salesUnits: 100, stock: [{ size: "S", quantity: 4 }, { size: "M", quantity: 9 }, { size: "L", quantity: 0 }] },
  { _id: "2", name: "CONTRASTING FABRIC T-SHIRT", salesUnits: 50, stock: [{ size: "S", quantity: 35 }, { size: "M", quantity: 9 }, { size: "L", quantity: 9 }] },
  { _id: "3", name: "RAISED PRINT T-SHIRT", salesUnits: 80, stock: [{ size: "S", quantity: 20 }, { size: "M", quantity: 2 }, { size: "L", quantity: 20 }] },
  { _id: "4", name: "PLEATED T-SHIRT", salesUnits: 3, stock: [{ size: "S", quantity: 25 }, { size: "M", quantity: 30 }, { size: "L", quantity: 10 }] },
  { _id: "5", name: "CONTRASTING LACE T-SHIRT", salesUnits: 650, stock: [{ size: "S", quantity: 0 }, { size: "M", quantity: 1 }, { size: "L", quantity: 0 }] },
  { _id: "6", name: "SLOGAN T-SHIRT", salesUnits: 20, stock: [{ size: "S", quantity: 9 }, { size: "M", quantity: 2 }, { size: "L", quantity: 5 }] }
]);

print("Seeded 6 products");
