/* Creates a time series of mod report queue stats
 *
 * mongosh <IP>:<PORT>/<DB> mongodb-queue-stats.js
 *
 * Must run on the main lila database.
 * Must run once a day at a fixed time.
 * Should complete within 1 minute.
 * OK to run many times in a row.
 * NOT OK to skip runs.
 * OK to run concurrently.
 */

const otherRooms = [
  { room: 'appeal', nb: db.appeal.countDocuments({ status: 'unread' }), score: NumberInt(40) },
  {
    room: 'streamer',
    nb: db.streamer.countDocuments({ 'approval.requested': true, 'approval.ignored': false }),
    score: NumberInt(40),
  },
];

db.report2.aggregate([
  { $match: { open: true, score: { $gte: 20 } } },
  {
    $group: {
      _id: ['$room', { $min: [80, { $multiply: [20, { $floor: { $divide: ['$score', 20] } }] }] }],
      nb: { $sum: 1 },
    },
  },
  { $project: { _id: 0, room: { $first: '$_id' }, score: { $last: '$_id' }, nb: 1 } },
  { $group: { _id: { $dateToString: { date: new Date(), format: '%Y-%m-%d' } }, data: { $push: '$$ROOT' } } },
  {
    $project: {
      data: {
        $concatArrays: ['$data', otherRooms],
      },
    },
  },
  { $merge: { into: 'mod_queue_stat', whenMatched: 'keepExisting' } },
]);
