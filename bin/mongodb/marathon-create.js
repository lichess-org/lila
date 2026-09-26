const copyFromId = 'spring25';

const t = db.tournament2.findOne({ _id: copyFromId });

// overrides
t._id = 'autumn26';
t.name = '2026 Autumn Marathon';
t.clock = {
  limit: NumberInt(3 * 60),
  increment: NumberInt(0),
};
t.schedule.speed = 'bullet';
t.startsAt = ISODate('2026-10-24');
t.description =
  "Let's make this the biggest chess tournament in history. " +
  '24h of ' +
  t.clock.limit / 60 +
  '+' +
  t.clock.increment +
  ' ' +
  t.schedule.speed +
  ' chess: ' +
  'top 500 players get a unique trophy!';

// initialize values
t.status = NumberInt(10);
t.createdAt = new Date();
t.nbPlayers = NumberInt(0);
delete t.featured;
delete t.winner;

db.tournament2.insertOne(t);
