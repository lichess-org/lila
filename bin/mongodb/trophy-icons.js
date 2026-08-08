const replacements = [
  ['marathonWinner', ''],
  ['marathonTopTen', ''],
  ['marathonTopFifty', ''],
  ['marathonTopHundred', ''],
  ['marathonSurvivor', ''],
  ['developer', ''],
  ['moderator', ''],
  ['verified', ''],
  ['marathonTopFivehundred', ''],
  ['contentTeam', ''],
  ['secAdvisor', ''],
  ['broadcastTeam', ''],
];

replacements.forEach(([kind, icon]) => {
  print(kind, icon);
  db.trophyKind.updateOne({ _id: kind }, { $set: { icon } });
});
