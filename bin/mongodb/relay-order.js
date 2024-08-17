console.log('Relay order migration');
console.log(db.relay_tour.estimatedDocumentCount(), 'tours');
const sortWith = r => r['startedAt'] || r['startsAt'] || r['createdAt'];
db.relay_tour
  .find({}, { _id: 1, name: 1 })
  .sort({ $natural: -1 })
  .forEach(tour => {
    const relays = db.relay.find({ tourId: tour._id }, { _id: 1, order: 1 }).toArray();
    if (relays.every(r => !!r.order)) return;
    relays.sort((a, b) => {
      for (k in ['startedAt', 'startsAt', 'createdAt']) {
        const aVal = sortWith(a);
        const bVal = sortWith(b);
        if (aVal && bVal && aVal != 'afterPrevious' && bVal != 'afterPrevious') return aVal - bVal;
      }
    });
    const res = db.relay.bulkWrite(
      relays.map((relay, index) => ({
        updateOne: {
          filter: { _id: relay._id },
          update: { $set: { order: index + 1 } },
        },
      })),
    );
    console.log(tour._id, tour.name, res.modifiedCount, 'rounds');
  });
