db.relay_tour
  .find({
    description: { $exists: true },
    'info.dates': { $exists: false },
    'info.format': { $exists: false },
    'info.tc': { $exists: false },
    'info.players': { $exists: false },
  })
  .forEach(function (tour) {
    if (!tour.description.includes('|')) {
      return;
    }
    const split = tour.description.split('|').map(x => x.trim());
    const info = {};
    const dates = split.shift();
    if (dates && /\d/.test(dates)) info.dates = dates;
    const format = split.shift();
    if (format) info.format = format;
    const tc = split.shift();
    if (tc) info.tc = tc.replace(/time control/i, '').trim();
    const players = split.shift();
    if (players) info.players = players;
    db.relay_tour.updateOne({ _id: tour._id }, { $set: { info: info } });
  });
