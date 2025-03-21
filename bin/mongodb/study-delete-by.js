const owner = 'donnyzz';

const ids = db.study.distinct('_id', { ownerId: owner });

console.log(`Deleting ${ids.length} studies...`);

sleep(3000);
db.study_chapter_flat.deleteMany({ studyId: { $in: ids } });
db.study.deleteMany({ _id: { $in: ids } });
