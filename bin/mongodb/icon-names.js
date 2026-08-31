const replacements = [
  ['', 'globe'],
  ['', 'tools'],
  ['', 'agent'],
  ['', 'checkmark'],
  ['', 'ink-quill'],
  ['', 'shield'],
  ['', 'radio-tower'],
  ['', 'berserk'],
  ['', 'antichess'],
  ['', 'flag-king-hill'],
  ['', 'die-six'],
  ['', 'three-check-stack'],
  ['', 'flag-racing-kings'],
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
