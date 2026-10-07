function toLink(doc, txt) {
  print('https://lichess.org/@/' + doc._id + ' ' + txt);
}

db.game5
  .aggregate([
    { $match: { ca: { $gt: new Date(Date.now() - 1000 * 3600 * 1) }, so: 12, wid: { $exists: 1 } } },
    { $sortByCount: '$wid' },
    {
      $lookup: {
        from: 'user4',
        localField: '_id',
        foreignField: '_id',
        as: 'user',
        pipeline: [{ $project: { email: 1, createdAt: 1 } }],
      },
    },
    { $unwind: '$user' },
    { $match: { 'user.createdAt': { $gt: new Date(Date.now() - 1000 * 3600 * 24 * 7) } } },
  ])
  .map(doc => toLink(doc, doc.user.email + ' ' + doc.count + ' games'));

// db.game5
//   .aggregate([
//     { $match: { ca: { $gt: new Date(Date.now() - 1000 * 3600 * 1) }, so: 12, pl: { $exists: 1 } } },
//     { $unwind: '$pl' },
//     { $sortByCount: '$pl' },
//     { $limit: 20 },
//     {
//       $lookup: {
//         from: 'user4',
//         localField: '_id',
//         foreignField: '_id',
//         as: 'user',
//         pipeline: [{ $project: { email: 1, createdAt: 1 } }],
//       },
//     },
//     { $unwind: '$user' },
//   ])
//   .map(doc => toLink(doc, doc.user.email + ' ' + doc.count + ' games'));
