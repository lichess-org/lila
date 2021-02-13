/* [INSERT HEAVY SWEARING HERE]
 *
 * there are duplicated keys in mongodb.
 * "updatedAt":{"$date":"2019-12-13T02:01:26.389Z"},"updatedAt":{"$date":"2019-12-13T02:00:30.324Z"}
 *
 * only visible with mongoexport, but cause bugs when used with reactivemongo
 */
db.study.find({ updatedAt: { $gt: new Date(Date.now() - 1000 * 3600 * 12) } }).forEach(s1 => {
  let id = s1._id;
  let u1 = s1.updatedAt;
  db.study.update({ _id: id }, { $unset: { updatedAt: 1 } });
  let s2 = db.study.findOne({ _id: id });
  let u2 = s2.updatedAt;
  let u3;
  if (u2) {
    let newer = u1 > u2 ? 'first' : 'second';
    print(`Found duplicated updatedAt for ${id}, ${newer} one is newer`);
    db.study.update({ _id: id }, { $unset: { updatedAt: 1 } });
    u3 = u1 > u2 ? u1 : u2;
  } else {
    u3 = u1;
  }
  db.study.update({ _id: id }, { $set: { updatedAt: u3 } });
});
