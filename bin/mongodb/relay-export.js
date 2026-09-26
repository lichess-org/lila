const from = new Date('2024-01-01T00:00:00Z');
const to = new Date('2026-01-01T00:00:00Z');

const sec = connect('mongodb://localhost:27117/lichess');

db.relay_tour_export.drop();
db.relay_round_export.drop();

sec.relay_tour
  .find({
    'dates.start': { $gte: from, $lt: to },
    'dates.end': { $gte: from, $lt: to },
  })
  .forEach(t => {
    console.log(`Exporting tour ${t._id} (${t.name})`);
    db.relay_tour_export.insertOne(t);
    sec.relay.find({ tourId: t._id }).forEach(r => {
      db.relay_round_export.insertOne(r);
    });
  });
