// db.relay.aggregate([{$match:{tourId:'KNfeoWE'}},{$project:{name:1,at:{$ifNull:['$startsAt','$startedAt']}}},{$sort:{at:1}},{$group:{_id:null,at:{$push:'$at'}}},{$project:{start:{$first:'$at'},end:{$last:'$at'}}}])

const fetchDates = (tourId) => db.relay.aggregate([
  { $match: { tourId } },
  { $project: { name: 1, at: { $ifNull: ['$startsAt', '$startedAt'] } } },
  { $sort: { at: 1 } }, { $group: { _id: null, at: { $push: '$at' } } },
  { $project: { start: { $first: '$at' }, end: { $last: '$at' } } }
]).next();

db.relay_tour.find({ dates: { $exists: 0 } }).sort({ $natural: -1 }).forEach(tour => {
  const dates = fetchDates(tour._id);
  if (dates) {
    console.log(tour._id + ' ' + tour.createdAt);
    db.relay_tour.updateOne({ _id: tour._id }, { $set: { dates } });
  }
});
