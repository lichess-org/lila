var id = 'winter17';
var copyFromId = 'autumn17';

var t = db.tournament2.findOne({ _id: copyFromId });

// overrides
t._id = id;
t.name = '2017 Winter Marathon';
t.clock = {
  limit: NumberInt(5 * 60),
  increment: NumberInt(3),
};
t.schedule.speed = 'blitz';
t.startsAt = ISODate('2017-12-28');
t.spotlight.description =
  "Let's make this the biggest chess tournament in history. " +
  '24h of ' +
  t.clock.limit / 60 +
  '+' +
  t.clock.increment +
  ' ' +
  t.schedule.speed +
  ' chess: ' +
  'top 100 players get a unique trophy!';

// initialize values
t.status = NumberInt(10);
t.createdAt = new Date();
t.nbPlayers = NumberInt(0);
delete t.featured;
delete t.winner;

db.tournament2.insert(t);
