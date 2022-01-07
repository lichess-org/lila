const oldId = 'qzRBGPLN';
const newId = 'taPgBglC';
const newStartsAt = ISODate('2022-01-11T19:10:00.000Z');

const tour = db.tournament.findOne({ _id: oldId });
const oldStartsAt = tour.startsAt;
tour._id = newId;
tour.startsAt = newStartsAt;
db.tournament.insert(tour);

db.tournament_player.update({ tid: oldId }, { $set: { tid: newId } }, { multi: 1 });
db.tournament_pairing.update({ tid: oldId }, { $set: { tid: newId } }, { multi: 1 });
