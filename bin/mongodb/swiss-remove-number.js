db.swiss_player.dropIndex('s_1_u_1');
db.swiss_player.dropIndex('s_1_n_1');

db.swiss.find().forEach(swiss => {
  let index = {};
  db.swiss_player.find({ s: swiss._id }).forEach(p => {
    index[p.n] = p.u;
  });

  db.swiss_pairing.find({ s: swiss._id }).forEach(p => {
    db.swiss_pairing.update(
      { _id: p._id },
      {
        $set: {
          p: [index[p.p[0]], index[p.p[1]]],
          t: p.t == p.p[0] ? NumberInt(0) : p.t == p.p[1] ? NumberInt(1) : p.t,
        },
      },
    );
  });

  db.swiss_player.update({ s: swiss._id }, { $unset: { n: 1 } }, { multi: 1 });
});

db.swiss.ensureIndex(
  { nbPlayers: -1 },
  { partialFilterExpression: { featurable: true, 'settings.i': { $lte: 600 } } },
);
