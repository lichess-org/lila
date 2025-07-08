const nbPrivate = db.relay_tour.updateMany(
  { tier: -1 },
  { $set: { tier: NumberInt(5), visibility: 'private' } },
  { multi: 1 },
).modifiedCount;

console.log(`Updated ${nbPrivate} private relays to tier 5 and visibility private.`);

const nbPublic = db.relay_tour.updateMany(
  { visibility: { $exists: false } },
  { $set: { visibility: 'public' } },
  { multi: 1 },
).modifiedCount;

console.log(`Updated ${nbPublic} public relays to visibility public.`);
