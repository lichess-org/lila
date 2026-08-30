const replacements = [
  ['', 'globe'],
  ['', 'tools'],
  ['', 'agent'],
  ['', 'checkmark'],
  ['', 'ink-quill'],
  ['', 'shield'],
  ['', 'radio-tower'],
  ['', 'berserk'],
];

replacements.forEach(([char, svg]) => {
  print(char, svg);
  db.trophyKind.updateMany({ icon: char }, { $set: { icon: svg } });
});
