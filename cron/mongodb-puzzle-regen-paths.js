/* Generates and saves a new generation of puzzle paths.
 * Drops the previous generation.
 *
 * mongosh <IP>:<PORT>/<DB> mongodb-puzzle-regen-paths.js
 *
 * Must run on the puzzle database.
 * Should run every 60 minutes.
 * Should complete within 3 minutes.
 * OK to run many times in a row.
 * OK to skip runs.
 * NOT OK to run concurrently.
 *
 * might require this mongodb config: (https://jira.mongodb.org/browse/SERVER-44174)
 * setParameter:
 *   internalQueryMaxPushBytes: 314572800
 */

const verbose = false;
const puzzleColl = db.puzzle2_puzzle;
const pathCollName = 'puzzle2_path';
const pathColl = db[pathCollName];
const pathNextColl = db.puzzle2_path_next;
const maxRatingBuckets = 20;
const maxPathLength = 200;
const maxPuzzlesPerTheme = 3800000; // avoids memory restrictions in some envs, like:
// MongoServerError: document constructed by $facet is 104948160 bytes, which exceeds the limit of 104857600 bytes
const maxOpenings = 1000; // using the most represented
const maxPathsPerGroup = 30;

const sep = '|';

const generation = Date.now();

pathNextColl.drop({});

const tiers = [
  ['top', 20 / 100],
  ['good', 50 / 100],
  ['all', 95 / 100],
];

const mixBoundaries = [
  100, 650, 800, 900, 1000, 1100, 1200, 1270, 1340, 1410, 1480, 1550, 1620, 1690, 1760, 1830, 1900, 2000,
  2100, 2200, 2350, 2500, 2650, 2800, 9999,
];

const themes = puzzleColl.distinct('themes', {}).filter(t => t && t != 'checkFirst');
const openings = db.puzzle2_puzzle
  .aggregate([
    { $unwind: '$opening' },
    { $sortByCount: '$opening' },
    { $limit: maxOpenings },
    { $group: { _id: null, openings: { $push: '$_id' } } },
  ])
  .next().openings;

function chunkify(a, n) {
  let len = a.length,
    out = [],
    i = 0,
    size;
  if (len % n === 0) {
    size = Math.floor(len / n);
    while (i < len) {
      out.push(a.slice(i, (i += size)));
    }
  } else
    while (i < len) {
      size = Math.ceil((len - i) / n--);
      out.push(a.slice(i, (i += size)));
    }
  return out;
}
const padRating = r => (r < 1000 ? '0' : '') + r;

let anyBuggy = false;

[...themes, ...openings, 'mix'].forEach(theme => {
  // [...openings].forEach(theme => {
  // ['mix'].forEach(theme => {
  const isOpening = openings.includes(theme);
  const subtleSelector = {
    $or: [{ tooSubtle: { $ne: true } }, { 'glicko.r': { $gte: 2200 } }, { 'glicko.d': { $gte: 120 } }],
  };
  const themeSelector = isOpening
    ? { opening: theme }
    : {
        themes:
          theme == 'mix'
            ? { $ne: 'equality' }
            : theme == 'equality'
              ? 'equality'
              : {
                  $eq: theme,
                  $ne: 'equality',
                },
      };
  const selector = {
    ...{ issue: { $exists: false } },
    ...subtleSelector,
    ...themeSelector,
  };

  const bucketBase = {
    groupBy: '$glicko.r',
    output: { puzzle: { $push: { id: '$_id', vote: '$vote' } } },
  };

  const nbPuzzles = puzzleColl.countDocuments(selector);

  if (!nbPuzzles) return [];

  const themeMaxPathLength = Math.max(10, Math.min(maxPathLength, Math.round(nbPuzzles / 150)));
  const nbRatingBuckets =
    theme == 'mix'
      ? mixBoundaries.length - 1
      : Math.max(3, Math.min(maxRatingBuckets, Math.round(nbPuzzles / themeMaxPathLength / 15)));

  const bucketStages =
    theme == 'mix'
      ? [
          {
            $bucket: {
              ...bucketBase,
              boundaries: mixBoundaries,
            },
          },
          { $addFields: { _id: { min: '$_id' } } },
        ]
      : [
          {
            $bucketAuto: {
              ...bucketBase,
              buckets: nbRatingBuckets,
            },
          },
        ];

  const pipeline = [
    {
      $match: selector,
    },
    ...(theme == 'mix' ? [{ $sample: { size: maxPuzzlesPerTheme } }] : []),
    ...bucketStages,
    {
      $unwind: '$puzzle',
    },
    {
      $sort: {
        'puzzle.vote': -1,
      },
    },
    {
      $group: {
        _id: '$_id',
        total: {
          $sum: 1,
        },
        puzzles: {
          $push: '$puzzle.id',
        },
      },
    },
    {
      $facet: tiers.reduce(
        (facets, [name, ratio]) => ({
          ...facets,
          ...{
            [name]: [
              {
                $project: {
                  total: 1,
                  puzzles: {
                    $slice: [
                      '$puzzles',
                      {
                        $round: {
                          $multiply: ['$total', ratio],
                        },
                      },
                    ],
                  },
                },
              },
              {
                $unwind: '$puzzles',
              },
              {
                $sample: {
                  // shuffle
                  size: 10 * 1000 * 1000,
                },
              },
              {
                $group: {
                  _id: '$_id',
                  puzzles: {
                    $addToSet: '$puzzles',
                  },
                },
              },
              {
                $sort: {
                  '_id.min': 1,
                },
              },
              {
                $addFields: {
                  tier: name,
                },
              },
            ],
          },
        }),
        {},
      ),
    },
    {
      $project: {
        bucket: {
          $concatArrays: tiers.map(t => '$' + t[0]),
        },
      },
    },
    {
      $unwind: '$bucket',
    },
    {
      $replaceRoot: {
        newRoot: '$bucket',
      },
    },
  ];

  if (verbose)
    print(
      `theme: ${theme}, puzzles: ${nbPuzzles}, path length: ${themeMaxPathLength}, rating buckets: ${nbRatingBuckets}`,
    );

  let prevTier = '',
    indexInTier = 0,
    buggy = false;

  puzzleColl
    .aggregate(pipeline, {
      allowDiskUse: true,
      comment: 'regen-paths',
    })
    .forEach(bucket => {
      if (prevTier == bucket.tier) indexInTier++;
      else {
        indexInTier = 0;
        prevTier = bucket.tier;
      }
      const isFirstOfTier = indexInTier == 0;
      const isLastOfTier = indexInTier == nbRatingBuckets - 1;
      const pathLength = Math.max(10, Math.min(maxPathLength, Math.round(bucket.puzzles.length / 30)));
      const ratingMin = isFirstOfTier ? 100 : Math.ceil(bucket._id.min);
      const ratingMax = isLastOfTier
        ? 9999
        : theme == 'mix'
          ? mixBoundaries[indexInTier + 1]
          : Math.floor(bucket._id.max);
      const nbPaths = Math.max(1, Math.floor(bucket.puzzles.length / pathLength));
      const allPaths = chunkify(bucket.puzzles, nbPaths);
      const paths = allPaths.slice(0, maxPathsPerGroup);
      buggy = buggy || (ratingMin == 100 && ratingMax == 9999) || ratingMin > ratingMax;
      anyBuggy = anyBuggy || buggy;
      if (verbose || buggy)
        print(
          `  ${theme} ${indexInTier} ${bucket.tier} ${ratingMin}->${ratingMax} puzzles: ${bucket.puzzles.length} pathLength: ${pathLength} paths: ${allPaths.length}->${paths.length}`,
        );

      pathNextColl.insertMany(
        paths.map((ids, j) => ({
          _id: `${theme}${sep}${bucket.tier}${sep}${padRating(ratingMin)}-${padRating(
            ratingMax,
          )}${sep}${generation}${sep}${j}`,
          min: `${theme}${sep}${bucket.tier}${sep}${padRating(ratingMin)}`,
          max: `${theme}${sep}${bucket.tier}${sep}${padRating(ratingMax)}`,
          ids,
          tier: bucket.tier,
          theme: theme,
          gen: generation,
        })),
        {
          ordered: false,
        },
      );
    });

  if (!buggy) {
    pathNextColl.aggregate([{ $merge: pathCollName }]); // much faster!
    pathColl.deleteMany({
      /* theme: theme */ _id: new RegExp('^' + theme + '\\|'),
      gen: { $ne: generation },
    });
  }
  pathNextColl.drop({});
});

if (!anyBuggy) {
  const res = pathColl.deleteMany({ gen: { $ne: generation } });
  if (verbose) print(`Deleted ${res.deletedCount} other gen paths`);
}
