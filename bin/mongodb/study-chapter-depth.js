rs.slaveOk();
var depth,
  maxDepth = 900;

function depthOf(branch) {
  return 2 + Math.max(...(branch.n || []).map(depthOf), 0);
}

function urlOf(chap) {
  var study = db.study.findOne({ _id: chap.studyId });
  return `https://lichess.org/study/${study._id}/${chap._id} by ${study.ownerId}`;
}

db.study_chapter.find().forEach(chap => {
  try {
    depth = depthOf(chap.root);
    if (depth > maxDepth) print(`${urlOf(chap)} ${depth}`);
  } catch (e) {
    print(`${urlOf(chap)} ${e}`);
  }
});
