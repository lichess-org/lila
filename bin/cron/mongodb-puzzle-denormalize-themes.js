/* Denormalize puzzle themes based on round theme votes.
 * Only looks for puzzles with the `dirty` flag, and removes it.
 *
 * mongo <IP>:<PORT>/<DB> mongodb-puzzle-denormalize-themes.js
 * 
 * Must run on the puzzle database.
 * Should run every 5 minutes.
 * Should complete within 10 seconds.
 * OK to run many times in a row.
 * OK to skip runs.
 * OK to run concurrently.
 */

const playColl = db.puzzle2_puzzle;
const roundColl = db.puzzle2_round;

const staticThemes = new Set([
  "oneMove", "short", "long", "veryLong",
  "mateIn1", "mateIn2", "mateIn3", "mateIn4", "mateIn5",
  "enPassant"
]);

playColl.find({ dirty: true }, { themes: true }).forEach(p => {

  const oldThemes = p.themes || [];
  const themeMap = {};

  roundColl.aggregate([
    {$match:{p:p._id,t:{$exists:true}}},
    {$project:{_id:0,t:1,w:1}},
    {$unwind:'$t'},
    {$group:{_id:'$t',v:{$sum:'$w'}}}
  ]).forEach(x => {
    const signum = x._id[0] == '+' ? 1 : -1;
    const theme = x._id.substring(1);
    themeMap[theme] = x.v * signum + (themeMap[theme] || 0);
  });

  const newThemes = new Set();
  Object.keys(themeMap).forEach(theme => {
    if (themeMap[theme] > 0) newThemes.add(theme);
  });

  const update = {$unset:{dirty:true}};
  if (
    oldThemes.length !== newThemes.size ||
    oldThemes.find(t => !newThemes.has(t))
  ) {
    const arr = Array.from(newThemes);
    // print(`Update ${p._id} themes: ${oldThemes.join(', ')} -> ${arr.join(', ')}`);
    update['$set'] = {themes:arr};
  }
  playColl.update({_id:p._id},update);
});
