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
 *
 * might require this mongodb config: (https://jira.mongodb.org/browse/SERVER-44174)
 * setParameter:
 *   internalQueryMaxPushBytes: 314572800
 */

const puzzleColl = db.puzzle2_puzzle;
const pathColl = db.puzzle2_path;
const verbose = false;
const maxRatingBuckets = 12;
const mixRatingBuckets = 20;
const maxPathLength = 500;
const maxPuzzlesPerTheme = 2 * 1000 * 1000; // reduce to 500000 to avoid memory restrictions in some envs (!?)

const generation = Date.now();

const tiers = [
  ['top', 20 / 100],
  ['good', 50 / 100],
  ['all', 95 / 100],
];

const themes = db.puzzle2_puzzle.distinct('themes', {});

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

themes.concat(['mix']).forEach(theme => {
  const selector = {
    themes:
      theme == 'mix'
        ? {
            $ne: 'equality',
          }
        : theme == 'equality'
        ? 'equality'
        : {
            $eq: theme,
            $ne: 'equality',
          },
  };

  const nbPuzzles = puzzleColl.count(selector);

  if (!nbPuzzles) return [];

  const themeMaxPathLength = Math.max(10, Math.min(maxPathLength, Math.round(nbPuzzles / 200)));
  const nbRatingBuckets =
    theme == 'mix'
      ? mixRatingBuckets
      : Math.max(3, Math.min(maxRatingBuckets, Math.round(nbPuzzles / themeMaxPathLength / 20)));

  if (verbose)
    print(
      `theme: ${theme}, puzzles: ${nbPuzzles}, path length: ${themeMaxPathLength}, rating buckets: ${nbRatingBuckets}`
    );

  let bucketIndex = 0;

  db.puzzle2_puzzle
    .aggregate(
      [
        {
          $match: selector,
        },
        {
          $limit: maxPuzzlesPerTheme,
        },
        {
          $bucketAuto: {
            buckets: nbRatingBuckets,
            groupBy: '$glicko.r',
            output: {
              puzzle: {
                $push: {
                  id: '$_id',
                  vote: '$vote',
                },
              },
            },
          },
        },
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
                      size: 9999999,
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
            {}
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
      ],
      {
        allowDiskUse: true,
        comment: 'regen-paths',
      }
    )
    .forEach(bucket => {
      const isFirstOfTier = bucketIndex % nbRatingBuckets == 0;
      const isLastOfTier = bucketIndex % nbRatingBuckets == nbRatingBuckets - 1;
      const pathLength = Math.max(10, Math.min(maxPathLength, Math.round(bucket.puzzles.length / 30)));
      const ratingMin = isFirstOfTier ? 100 : Math.ceil(bucket._id.min);
      const ratingMax = isLastOfTier ? 9999 : Math.floor(bucket._id.max);
      const nbPaths = Math.max(1, Math.floor(bucket.puzzles.length / pathLength));
      const paths = chunkify(bucket.puzzles, nbPaths);
      // print(`  ${theme} ${bucket.tier} ${ratingMin}->${ratingMax} puzzles: ${bucket.puzzles.length} pathLength: ${pathLength} paths: ${paths.length}`);

      pathColl.insert(
        paths.map((ids, j) => ({
          _id: `${theme}_${bucket.tier}_${padRating(ratingMin)}-${padRating(ratingMax)}_${generation}_${j}`,
          min: `${theme}_${bucket.tier}_${padRating(ratingMin)}`,
          max: `${theme}_${bucket.tier}_${padRating(ratingMax)}`,
          ids,
          tier: bucket.tier,
          theme: theme,
          gen: generation,
        })),
        {
          ordered: false,
        }
      );
      bucketIndex++;
    });
});

pathColl.remove({
  gen: {
    $ne: generation,
  },
});
