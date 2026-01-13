const user = 'Amazing_Tactics'.toLowerCase();

db.study.find({ ownerId: user, visibility: 'public' }).forEach(function(study) {
  const line = computeLine(study);
  if (line.find(l => l.ownerId != user)) {
    console.log('--------------------------');
    line.forEach((study, i) => {
      console.log(`${i} ${studyUrl(study)} @${study.ownerId} ${study.likes} ${study.name}`);
    });
  }
});

function computeLine(study) {
  const parts = (study.from || '').split(' ');
  let source;
  if (parts[0] == 'study') source = db.study.findOne({ _id: parts[1] });
  if (source) return [study, ...computeLine(source)];
  return [study];
}

function studyUrl(study) {
  return `https://lichess.org/study/${study._id}`;
}
