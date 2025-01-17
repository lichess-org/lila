count = 0;
db.analysis_requester.find().forEach(function (doc) {
  const entries = Object.entries(doc);
  const reduced = entries.filter(([key]) => !key.includes('-') || key.startsWith('2025-'));
  if (entries.length !== reduced.length) {
    const newDoc = Object.fromEntries(reduced);
    db.analysis_requester.replaceOne({ _id: doc._id }, newDoc);
  }
  if (count % 1000 === 0) print(count);
  count++;
});

print(count, 'documents processed');

/*
  size: 2710719734,
  count: 6218537,
  storageSize: 627077120,
  totalIndexSize: 178106368,
  totalSize: 805183488,
  indexSizes: { 'total_-1': 51482624, _id_: 126623744 },
  avgObjSize: 435,
  ns: 'lichess.analysis_requester',
  nindexes: 2,
  scaleFactor: 1
*/
