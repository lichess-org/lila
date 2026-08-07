/*
Exports draw offers to a CSV file.

Output format:
GameID,Draws,Variant

Draws are plies separated by white space. Plus for white, minus for black.
The variant is omitted for standard chess.

Output sample:

STsO5Bvd,+47,
0M3VDciI,+58 +80 +102 -119,
0B5JCSyq,-7 -28,crazy
tEby487R,-34,atomic
*/

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
