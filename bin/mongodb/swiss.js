db.swiss.dropIndexes();
db.swiss_player.dropIndexes();
db.swiss_pairing.dropIndexes();

db.swiss.ensureIndex({ teamId: 1, startsAt: -1 });

db.swiss_player.ensureIndex({ s: 1, u: 1 });
db.swiss_player.ensureIndex({ s: 1, n: 1 }, { unique: true });
db.swiss_player.ensureIndex({ s: 1, c: -1 });

db.swiss_pairing.ensureIndex({ s: 1, r: 1 });
db.swiss_pairing.ensureIndex({ s: 1, p: 1, r: 1 });
db.swiss_pairing.ensureIndex({ t: 1 }, { partialFilterExpression: { t: true } });
