map = [
  ['_lichess', 'activity.lichess'],
  ['_pirate', 'flags.pirate-flag'],
  ['_rainbow', 'flags.rainbow-flag'],
  ['_transgender', 'flags.transgender-flag'],
  ['_esperanto', 'symbols.esperanto'],
];

db.user4.find({ 'profile.country': { $in: map.map(e => e[0]) } }, { 'profile.country': 1 }).forEach(user => {
  flair = map.find(e => e[0] == user.profile.country)[1];
  db.user4.updateOne({ _id: user._id }, { $unset: { 'profile.country': 1 }, $set: { flair: flair } });
});
