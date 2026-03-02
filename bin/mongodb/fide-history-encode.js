const tcs = ['standard', 'rapid', 'blitz'];

const encodePlayer = player => {
  tcs.forEach(tc => {
    if (player[tc]) player[tc] = player[tc].map(encodePoint);
  });
};

// [ '2025-12', 1828 ] -> 2025121828
const encodePoint = ([date, elo]) => Number.parseInt(date.replace('-', '') + elo.toString().padStart(4, '0'));

db.fide_player_rating.find().forEach(player => {
  encodePlayer(player);
  db.fide_player_rating.updateOne({ _id: player._id }, { $set: player });
});
