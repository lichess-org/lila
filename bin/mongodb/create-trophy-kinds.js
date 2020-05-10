db.trophyKind.drop();
db.trophyKind.insert({
  _id: "marathonWinner",
  name: "Marathon Winner",
  icon: "\\",
  klass: "fire-trophy",
  order: NumberInt(51),
  withCustomImage: false
});
db.trophyKind.insert({
  _id: "marathonTopTen",
  name: "Marathon Top 10",
  icon: "\\",
  klass: "fire-trophy",
  order: NumberInt(52),
  withCustomImage: false
});
db.trophyKind.insert({
  _id: "marathonTopFifty",
  name: "Marathon Top 50",
  icon: "\\",
  klass: "fire-trophy",
  order: NumberInt(53),
  withCustomImage: false
});
db.trophyKind.insert({
  _id: "marathonTopHundred",
  name: "Marathon Top 100",
  icon: "\\",
  klass: "fire-trophy",
  order: NumberInt(54),
  withCustomImage: false
});
db.trophyKind.insert({
  _id: "developer",
  name: "Lidraughts developer",
  icon: "\ue000",
  url: "https://github.com/roepstoep/lidraughts",
  klass: "icon3d",
  order: NumberInt(100),
  withCustomImage: false
});
db.trophyKind.insert({
  _id: "moderator",
  name: "Lidraughts moderator",
  icon: "\ue002",
  url: "//lidraughts.org/report",
  klass: "icon3d",
  order: NumberInt(101),
  withCustomImage: false
});
db.trophyKind.insert({
  _id: "verified",
  name: "Verified account",
  icon: "E",
  klass: "icon3d",
  order: NumberInt(102),
  withCustomImage: false
});
