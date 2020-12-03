// Makes report scores decay with time.
// This script is meant to run once per day.
// It's ok to run it more than that once a day.
// It's ok to run it less than that once a day.
// It's ok to run it several times in a row.
// The script is expected to complete within a couple seconds.
//
// Run it with:
// mongo <IP>:27017/lichess mongodb-report-score-decay.js

const atomMinScore = 5;

db.report2.find(
  {open:true,'atoms.score':{$gt: atomMinScore}}, // selector
  {score:1,'atoms.score':1,'atoms.initScore':1,'atoms.at':1} // projection
).hint('best_open').forEach(report => {

  // what to update in the report
  const set = {
    score: 0
  };

  report.atoms.forEach((atom, index) => {
    if (!atom.initScore) {
      // save original score if not yet present
      atom.initScore = set[`atoms.${index}.initScore`] = atom.score;
    }
    atom.score = set[`atoms.${index}.score`] = newScore(atom);
    set.score += atom.score;
  });

  if (set.score != report.score) {
    db.report2.update({ _id: report._id}, {$set:set});
  }
});

function newScore(atom) {
  const minScore = Math.min(atom.initScore, atomMinScore);
  return Math.max(minScore, atom.initScore - scoreDecay(atom.at));
}

function scoreDecay(date) {
  return daysSince(date);
}
function daysSince(date) {
  return Math.floor((new Date().getTime()  - date.getTime() ) / 86400000);
}
