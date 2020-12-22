db.trophyKind.drop();

db.trophyKind.insert({
  _id: "developer",
  name: "Lishogi developer",
  icon: "\ue000",
  url: "https://github.com/ornicar/lila/graphs/contributors",
  klass: "icon3d",
  order: NumberInt(100),
  withCustomImage: false,
});
db.trophyKind.insert({
  _id: "moderator",
  name: "Lishogi moderator",
  icon: "\ue002",
  url: "//lichess.org/report",
  klass: "icon3d",
  order: NumberInt(101),
  withCustomImage: false,
});
db.trophyKind.insert({
  _id: "verified",
  name: "Verified account",
  icon: "E",
  klass: "icon3d",
  order: NumberInt(102),
  withCustomImage: false,
});
