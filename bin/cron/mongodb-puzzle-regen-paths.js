/* Generates and saves a new generation of puzzle paths.
 * Drops the previous generation.
 *
 * mongo <IP>:<PORT>/<DB> mongodb-puzzle-regen-paths.js
 *
 * Must run on the puzzle database.
 * Should run every 60 minutes.
 * Should complete within 3 minutes.
 * OK to run many times in a row.
 * OK to skip runs.
 * NOT OK to run concurrently.
 */

const puzzleColl = db.puzzle2_puzzle;
const pathColl = db.puzzle2_path;

const generation = Date.now()

const tiers = [
  ['top', 33 / 100],
  ['good', 66 / 100],
  ['all', 95 / 100]
];

const themes = db.puzzle2_puzzle.distinct('themes',{});

// explodeArray([1..12], 3)
// [[1, 4, 7, 10], [2, 5, 8, 11], [3, 5, 9, 12]]
function explodeArray(arr, nb) {
  const res = [];
  for (i = 0; i < nb; i++) res[i] = [];
  for (i in arr) res[i % nb].push(arr[i]);
  return res;
}
const padRating = r => (r < 1000 ? '0' : '') + r;

function makeTier(theme, tierName, thresholdRatio) {

  const themeName = theme || 'any';

  const selector = {
    ...(theme ? {themes: theme} : {})
  };

  const nbThemePuzzles = puzzleColl.count(selector);

  const nbPuzzles = Math.round(nbThemePuzzles * thresholdRatio);

  if (!nbPuzzles) return [];

  const pathLength = Math.max(20, Math.min(300, Math.round(nbPuzzles / 100)));

  const nbRatingBuckets = Math.max(3, Math.min(8, Math.round(nbPuzzles / pathLength / 15)));

  const docs = [];
  let bucketNumber = 0;

  db.puzzle2_puzzle.aggregate([{
    $match: selector
  }, {
    $sort: {
      vote: -1
    }
  }, {
    $limit: nbPuzzles
  }, {
    $bucketAuto: {
      groupBy: '$glicko.r',
      buckets: nbRatingBuckets,
      output: {
        puzzles: {
          $addToSet: '$_id'
        }
      }
    }
  }], {
    allowDiskUse: true,
    comment: 'make-paths'
  }).forEach(bucket => {
    const ratingMin = Math.ceil(bucket._id.min);
    const ratingMax = ++bucketNumber == nbRatingBuckets ? 9999 : Math.floor(bucket._id.max);
    const nbPaths = Math.floor(bucket.puzzles.length / pathLength);
    const puzzles = bucket.puzzles.slice(0, nbPaths * pathLength);
    const paths = explodeArray(puzzles, nbPaths);
    // print(`  ${ratingMin}->${ratingMax} paths: ${paths.length}`);
    paths.forEach((ids, i) => {
      docs.push({
        _id: `${themeName}_${tierName}_${ratingMin}-${ratingMax}_${generation}_${i}`,
        min: `${themeName}_${tierName}_${padRating(ratingMin)}`,
        max: `${themeName}_${tierName}_${padRating(ratingMax)}`,
        ids,
        tier: tierName,
        theme: themeName,
        size: ids.length,
        gen: generation
      });
    });
  });

  print(`theme: ${themeName}, tier: ${tierName}, threshold: ${thresholdRatio}, puzzles: ${nbPuzzles}, path length: ${pathLength}, rating buckets: ${nbRatingBuckets}, paths: ${docs.length}`);

  return docs;
}

const docs = [];

themes.concat([null]).forEach(theme =>
  tiers.forEach(([name, threshold]) => makeTier(theme, name, threshold).forEach(p => docs.push(p)))
);

pathColl.insert(docs, {ordered: false});

pathColl.remove({gen:{$ne:generation}});
