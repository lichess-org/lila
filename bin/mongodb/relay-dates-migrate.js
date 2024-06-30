// db.relay.aggregate([{$match:{tourId:'KNfeoWE'}},{$project:{name:1,at:{$ifNull:['$startsAt','$startedAt']}}},{$sort:{at:1}},{$group:{_id:null,at:{$push:'$at'}}},{$project:{start:{$first:'$at'},end:{$last:'$at'}}}])

const fetchDates = tourId =>
  db.relay
    .aggregate([
      { $match: { tourId } },
      { $project: { name: 1, at: { $ifNull: ['$startsAt', '$startedAt'] } } },
      { $sort: { at: 1 } },
      { $group: { _id: null, at: { $push: '$at' } } },
      { $project: { start: { $first: '$at' }, end: { $last: '$at' } } },
    ])
    .next();

const cmp = (a, b) => (a ? a.getTime() : 0) == (b ? b.getTime() : 0);

db.relay_tour
  .find()
  .sort({ $natural: -1 })
  .limit(200)
  .forEach(tour => {
    const dates = fetchDates(tour._id);
    if (dates) {
      if (!cmp(dates?.start, tour.dates?.start)) {
        console.log(tour._id + ' ' + tour.dates?.start + ' -> ' + dates.start);
        db.relay_tour.updateOne({ _id: tour._id }, { $set: { 'dates.start': dates.start } });
      }
      if (!cmp(dates?.end, tour.dates?.end)) {
        console.log(tour._id + ' ' + tour.dates?.end + ' -> ' + dates.end);
        db.relay_tour.updateOne({ _id: tour._id }, { $set: { 'dates.end': dates.end } });
      }
    }
  });
