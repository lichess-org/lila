const newId = () => {
  const idChars = '0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ';
  const idLength = 8;
  let id = '';
  for (let i = idLength; i > 0; --i) id += idChars[Math.floor(Math.random() * idChars.length)];
  return id;
};

db.relay.find().forEach(relay => {
  const tourId = newId();
  db.relay_tour.insert({
    ...{
      _id: tourId,
      name: relay.name,
      description: relay.description,
      ownerId: relay.ownerId,
      createdAt: relay.createdAt,
      official: relay.official,
      active: false,
      syncedAt: relay.startedAt,
    },
    ...(relay.markup ? { markup: relay.markup } : {}),
    ...(relay.credit ? { credit: relay.credit } : {}),
  });
  db.relay.update(
    { _id: relay._id },
    {
      $set: { tourId },
      $unset: {
        ownerId: true,
        official: true,
        description: true,
        markup: true,
        credit: true,
      },
    },
  );
});
