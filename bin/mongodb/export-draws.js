const date = { $gt: new Date('2022-01-01'), $lt: new Date('2026-01-01') };

const variants = [
  'std',
  'std',
  'chess960',
  'fen',
  'koth',
  '3check',
  'anti',
  'atomic',
  'horde',
  'racing',
  'crazy',
];

db.game5
  .aggregate([
    { $match: { ca: date, do: { $exists: 1 }, v: { $ne: 3 } } },
    { $project: { _id: 1, do: 1, v: 1 } },
  ])
  .forEach(game => {
    const draws = game.do.map(d => (d > 0 ? '+' + d : d.toString())).join(' ');
    const variant = game.v ? variants[game.v] : '';
    print(game._id + ',' + draws + ',' + variant);
  });
