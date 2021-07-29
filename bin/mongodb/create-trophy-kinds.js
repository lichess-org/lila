db.trophyKind.drop();
db.trophyKind.insert({
  _id: 'zugMiracle',
  name: 'Zug miracle',
  url: '//lichess.org/faq#trophies',
  order: NumberInt(1),
  withCustomImage: false,
});
db.trophyKind.insert({
  _id: 'wayOfBerserk',
  name: 'The way of Berserk',
  icon: '',
  url: '//lichess.org/faq#trophies',
  klass: 'fire-trophy',
  order: NumberInt(2),
  withCustomImage: false,
});
db.trophyKind.insert({
  _id: 'marathonWinner',
  name: 'Marathon Winner',
  icon: '',
  klass: 'fire-trophy',
  order: NumberInt(3),
  withCustomImage: false,
});
db.trophyKind.insert({
  _id: 'marathonTopTen',
  name: 'Marathon Top 10',
  icon: '',
  klass: 'fire-trophy',
  order: NumberInt(4),
  withCustomImage: false,
});
db.trophyKind.insert({
  _id: 'marathonTopFifty',
  name: 'Marathon Top 50',
  icon: '',
  klass: 'fire-trophy',
  order: NumberInt(5),
  withCustomImage: false,
});
db.trophyKind.insert({
  _id: 'marathonTopHundred',
  name: 'Marathon Top 100',
  icon: '',
  klass: 'fire-trophy',
  order: NumberInt(6),
  withCustomImage: false,
});
db.trophyKind.insert({
  _id: 'marathonTopFivehundred',
  name: 'Marathon Top 500',
  icon: '',
  klass: 'fire-trophy',
  order: NumberInt(7),
  withCustomImage: false,
});
db.trophyKind.insert({
  _id: 'marathonSurvivor',
  name: 'Marathon #1 survivor',
  icon: '',
  url: '//lichess.org/blog/VXF45yYAAPQgLH4d/chess-marathon-1',
  klass: 'fire-trophy',
  order: NumberInt(8),
  withCustomImage: false,
});
db.trophyKind.insert({
  _id: 'developer',
  name: 'Lichess developer',
  icon: '',
  url: 'https://github.com/ornicar/lila/graphs/contributors',
  klass: 'icon3d',
  order: NumberInt(100),
  withCustomImage: false,
});
db.trophyKind.insert({
  _id: 'moderator',
  name: 'Lichess moderator',
  icon: '',
  url: '//lichess.org/report',
  klass: 'icon3d',
  order: NumberInt(101),
  withCustomImage: false,
});
db.trophyKind.insert({
  _id: 'verified',
  name: 'Verified account',
  icon: '',
  klass: 'icon3d',
  order: NumberInt(102),
  withCustomImage: false,
});
db.trophyKind.insert({
  _id: 'zhwc17',
  name: 'Crazyhouse champion 2017',
  url: '//lichess.org/blog/WMnMzSEAAMgA3oAW/crazyhouse-world-championship-the-candidates',
  order: NumberInt(1),
  withCustomImage: true,
});
db.trophyKind.insert({
  _id: 'zhwc18',
  name: 'Crazyhouse champion 2018',
  url: '//lichess.org/forum/team-crazyhouse-world-championship/opperwezen-the-2nd-cwc',
  order: NumberInt(1),
  withCustomImage: true,
});
db.trophyKind.insert({
  _id: 'atomicwc16',
  name: 'Atomic World Champion 2016',
  url: '//lichess.org/forum/team-atomic-wc/championship-final',
  order: NumberInt(1),
  withCustomImage: true,
});
db.trophyKind.insert({
  _id: 'atomicwc17',
  name: 'Atomic World Champion 2017',
  url: '//lichess.org/forum/team-atomic-wc/awc-2017-its-final-time',
  order: NumberInt(1),
  withCustomImage: true,
});
db.trophyKind.insert({
  _id: 'atomicwc18',
  name: 'Atomic World Champion 2018',
  url: '//lichess.org/forum/team-atomic-wc/announcement-awc-2018',
  order: NumberInt(1),
  withCustomImage: true,
});
db.trophyKind.insert({
  _id: 'acwc18',
  name: 'Antichess World Champion 2018',
  url: '//lichess.org/forum/team-antichess-wc/congratulations-to-our-new-antichess-world-champion',
  order: NumberInt(1),
  withCustomImage: true,
});
