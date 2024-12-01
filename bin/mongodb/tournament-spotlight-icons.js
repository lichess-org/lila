const replacements =
  [
    [7, ''], // atomic
    [6, ''], // antichess
    [2, ''], // chess960
    [10, ''], // crazyhouse
    [8, ''], // horde
    [4, ''], // kingOfTheHill
    [9, ''], // racingKings
    [5, ''], // threeCheck
  ];

replacements.forEach(([variantId, icon]) => {
  print(variantId, icon);
  const res = db.tournament2.updateMany(
    { 'spotlight.iconFont': { $exists: true }, variant: variantId },
    { $set: { 'spotlight.iconFont': icon } }
  );
  print('done: ' + res.modifiedCount);
});
