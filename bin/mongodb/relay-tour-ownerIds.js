db.relay_tour.find().forEach(function (tour) {
  db.relay_tour.updateOne({ _id: tour._id }, { $set: { ownerIds: [tour.ownerId] } });
});
