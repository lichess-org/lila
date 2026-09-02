const replacements = [
  ['', 'globe'],
  ['', 'tools'],
  ['', 'agent'],
  ['', 'checkmark'],
  ['', 'inkQuill'],
  ['', 'shield'],
  ['', 'radioTower'],
  ['', 'berserk'],
  ['', 'antichess'],
  ['', 'flagKingHill'],
  ['', 'dieSix'],
  ['', 'threeCheckStack'],
  ['', 'flagRacingKings'],
  ['', 'crazyhouse'],
  ['', 'atom'],
  ['', 'keypad'],
  ['', 'globe'],
];

replacements.forEach(([char, svg]) => {
  print(char, svg);
  db.trophyKind.updateMany({ icon: char }, { $set: { icon: svg } });
  db.tournament2.updateMany(
    { 'spotlight.iconFont': char },
    {
      $unset: { 'spotlight.iconFont': true },
      $set: { 'spotlight.icon': svg },
    },
  );
});
