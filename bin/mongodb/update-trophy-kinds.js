const newIcons = {
  wayOfBerserk: '',
  marathonWinner: '',
  marathonTopTen: '',
  marathonTopFifty: '',
  marathonTopHundred: '',
  developer: '',
  moderator: '',
  verified: '',
};
Object.keys(newIcons).forEach(id => db.trophyKind.update({ _id: id }, { $set: { icon: newIcons[id] } }));
