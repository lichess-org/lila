// https://github.com/lichess-org/lila/blob/master/modules/relay/src/main/RelayInputSanity.scala#L31-L38

db.study_chapter_flat
  .find({ 'relay.fideIds': { $exists: 1 }, tags: 'Site:Tornelo INT' }, { tags: 1 })
  .forEach(function (c) {
    const tags = c.tags.filter(t => !t.includes('Country:'));
    db.study_chapter_flat.updateOne({ _id: c._id }, { $set: { tags } });
  });
