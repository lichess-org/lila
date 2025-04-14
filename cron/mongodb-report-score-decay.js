// Makes report scores decay with time.
// This script is meant to run once per day.
// It's ok to run it more than once a day.
// It's ok to run it less than once a day.
// It's ok to run it several times in a row.
// The script is expected to complete within a couple seconds.
// Run it with:
// mongosh <IP>:27017/lichess mongodb-report-score-decay.js

const newScore = atom => Math.max(0, atom.initScore - scoreDecay(atom.at));
const scoreDecay = date => Math.max(0, daysSince(date) - 3);
const now = new Date().getTime();
const daysSince = date => Math.floor((now - date.getTime()) / 86400000);

db.report2
  .find({ open: true }, { score: 1, 'atoms.score': 1, 'atoms.initScore': 1, 'atoms.at': 1 })
  .hint('best_open')
  .forEach(report => {
    // what to update in the report
    const set = {
      score: NumberInt(0),
    };

    report.atoms.forEach((atom, index) => {
      if (!atom.initScore || atom.initScore != NumberInt(atom.initScore)) {
        // save original score if not yet present
        atom.initScore = set[`atoms.${index}.initScore`] = NumberInt(atom.score);
      }
      atom.score = set[`atoms.${index}.score`] = NumberInt(newScore(atom));
      set.score += atom.score;
    });

    if (set.score <= 0) {
      db.report2.deleteOne({ _id: report._id });
    } else if (set.score != report.score) {
      db.report2.updateOne({ _id: report._id }, { $set: set });
    }
  });
