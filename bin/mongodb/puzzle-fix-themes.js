const eqSet = (xs, ys) => xs.size === ys.size && [...xs].every(x => ys.has(x));

let nb = 0;

function roundFix(round) {
  const p = round.puzzle;
  const moves = p.line.split(' ').length / 2;
  const t = new Set(round.t);
  if (moves < 5) t.delete('+mateIn5');
  if (moves !== 4) t.delete('+mateIn4');
  if (moves !== 3) t.delete('+mateIn3');
  if (moves !== 2) t.delete('+mateIn2');
  if (moves !== 1) t.delete('+mateIn1');
  t.delete('+oneMove');
  t.delete('+short');
  t.delete('+long');
  t.delete('+veryLong');
  if (moves === 1) t.add('+oneMove');
  else if (moves === 2) t.add('+short');
  else if (moves === 3) t.add('+long');
  else t.add('+veryLong');
  if (!eqSet(new Set(round.t), t)) {
    nb++;
    const kept = new Set(round.t).intersection(t);
    console.log(
      nb,
      round._id,
      moves,
      Array.from(new Set(round.t).difference(kept)).join(' '),
      '=>',
      Array.from(t.difference(kept)).join(' '),
    );
    db.puzzle2_round.updateOne({ _id: round._id }, { $set: { t: Array.from(t) } });
    db.puzzle2_puzzle.updateOne({ _id: p._id }, { $set: { dirty: true } });
  }
}

db.puzzle2_round
  .aggregate([
    { $match: { _id: /^lichess:/ } },
    {
      $lookup: {
        from: 'puzzle2_puzzle',
        as: 'puzzle',
        localField: 'p',
        foreignField: '_id',
      },
    },
    {
      $unwind: '$puzzle',
    },
  ])
  .forEach(roundFix);
