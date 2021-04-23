const users = db.user4;

// Add some users (their passwords are "password")
users.updateOne(
  { _id: 'limezebra' },
  {
    $set: {
      username: 'LimeZebra',
      email: 'limezebra@gmail.com',
      bpass: BinData(0, 'n1G7smu1Z6DC1GszOpLba+POwqXfToPW8KMVJWxzECou5EnPYotZ'),
      perfs: {},
      count: {
        ai: 0,
        draw: 0,
        drawH: 0,
        game: 0,
        loss: 0,
        lossH: 0,
        rated: 0,
        win: 0,
        winH: 0,
      },
      enabled: true,
      createdAt: ISODate('2021-04-22T19:48:34.144Z'),
      seenAt: ISODate('2021-04-22T19:48:34.858Z'),
      time: {
        total: 0,
        tv: 0,
      },
      verbatimEmail: 'limeZebra@gmail.com',
      len: 9,
      lang: 'en-US',
    },
  },
  { upsert: true }
);

users.updateOne(
  { _id: 'humorousantelope' },
  {
    $set: {
      username: 'HumorousAntelope',
      email: 'humorousantelope@gmail.com',
      bpass: BinData(0, 'n1G7smu1Z6DC1GszOpLba+POwqXfToPW8KMVJWxzECou5EnPYotZ'),
      perfs: {},
      count: {
        ai: 0,
        draw: 0,
        drawH: 0,
        game: 0,
        loss: 0,
        lossH: 0,
        rated: 0,
        win: 0,
        winH: 0,
      },
      enabled: true,
      createdAt: ISODate('2021-04-22T19:48:34.144Z'),
      seenAt: ISODate('2021-04-22T19:48:34.858Z'),
      time: {
        total: 0,
        tv: 0,
      },
      verbatimEmail: 'HumorousAntelope@gmail.com',
      len: 9,
      lang: 'en-US',
    },
  },
  { upsert: true }
);

// add some games
const games = db.game5;

games.updateOne(
  { _id: 'D9f2fvLt' },
  {
    $set: {
      is: 'aaaaaa',
      us: ['LimeZebra', 'HumorousAntelope'],
      p0: {
        e: 1760,
        p: true,
        d: 119,
      },
      p1: {
        e: 1728,
        d: -5,
      },
      s: 31,
      t: 43,
      c: BinData(0, 'CgAANI8AKqw='),
      cw: BinData(0, 'ViGoXhaqKJqy6K9rRsLIsJG9dB7Whfk5OIpRuqTI'),
      cb: BinData(0, 'VSyJozR7hL6SNDZNEKQgi/PHAS5pL6SnwZus4A=='),
      ra: true,
      ca: ISODate('2020-07-01T02:25:33.258Z'),
      ua: ISODate('2020-07-01T02:29:47.068Z'),
      so: 12,
      hp: BinData(0, 'PDdrGSJaqs+KmUAq85NNMThkXKNHIUA='),
      w: true,
      wid: 'LimeZebra',
      an: true,
    },
  },
  { upsert: true }
);

games.updateOne(
  { _id: 'TaHSAsYD' },
  {
    $set: {
      is: 'O8dEwDod',
      us: ['LimeZebra', 'HumorousAntelope'],
      p0: {
        e: 2264,
        d: 3,
      },
      p1: {
        e: 2069,
        d: -3,
      },
      s: 35,
      t: 97,
      c: BinData(0, 'CgAA38IA6mQ='),
      cw: BinData(0, 'wGAhen8MW6C28eG7gbi0S7rEzAvMPLTu1KHlhKUh18BT3zxTY9SgQYDTvHlPCf6QIyB8SJ/PQYihGoTkZtLA'),
      cb: BinData(0, 'vHARnEBZLL6hkblax4GSWiwXenwwssK8dk8LyS4sfqG7D1eo43kerh70+MHF8Bm0ah3leztkZ5KI4yrsrCrO8piEWFA='),
      ra: true,
      ca: ISODate('2020-07-01T02:23:07.519Z'),
      ua: ISODate('2020-07-01T02:42:59.641Z'),
      so: 5,
      tid: 'f5WSE8Yc',
      hp: BinData(0, 'PDVvHX7zZXUV51aObVt//n+PYCv+TKzL6zXvcTbiqlrv551I5GeN/S1rL74i/K9a68ahbTaRzHvo9uTN2+dc'),
      w: true,
      wid: 'LimeZebra',
      an: true,
    },
  },
  { upsert: true }
);

games.updateOne(
  { _id: 'rVK1n3ZW' },
  {
    $set: {
      is: 'YQjJlsJm',
      us: ['HumorousAntelope', 'LimeZebra'],
      p0: {
        e: 1728,
        d: 19,
      },
      p1: {
        e: 1634,
        d: -14,
      },
      s: 31,
      t: 75,
      c: BinData(0, 'CgAAaY4AcFY='),
      cw: BinData(0, 'lEpQfow+jiH0ZqKUtise1vRboSseBmxcOHuCdrbcfoG54kZkOWRyS4Y4GZKj8bBNJhEA'),
      cb: BinData(0, 'lFBoqBHuiqOYaqOE5i4e5PibwfhOF80wIGgPgfHuHw/D5HdI1yOMdEXIqOBIs40A'),
      ra: true,
      ca: ISODate('2020-05-01T08:53:16.559Z'),
      ua: ISODate('2020-05-01T09:02:37.125Z'),
      so: 5,
      tid: '7VGVwRoa',
      hp: BinData(0, 'AlG1u0hHOm25JpJokI+8K3RkyR0MmzAXP2rScXHxpee2Ad7au+S6MxnE'),
      w: true,
      wid: 'HumorousAntelope',
      an: true,
    },
  },
  { upsert: true }
);

// add some puzzles
const puzzleColl = db.puzzle2_puzzle;

puzzleColl.updateOne(
  { _id: '02VMp' },
  {
    $set: {
      gameId: 'D9f2fvLt',
      fen: 'rk1q3r/4nQpp/p2pB3/1p6/4P3/5P1P/PPP2P2/2KR3R w - - 0 20',
      themes: ['middlegame', 'defensiveMove', 'hangingPiece', 'long', 'advantage'],
      glicko: {
        r: 1532.7572615553727,
        d: 129.23809133936328,
        v: 0.08996277354273302,
      },
      plays: 17,
      vote: 0.7272727272727273,
      line: 'd1d6 d8d6 h1d1 d6c5 f7g7 e7g6',
      generator: 14,
      cp: 376,
      vd: 3,
      vu: 19,
      users: ['LimeZebra', 'HumorousAntelope'],
    },
  },
  { upsert: true }
);

puzzleColl.updateOne(
  { _id: '01tg7' },
  {
    $set: {
      gameId: 'TaHSAsYD',
      fen: '8/1bnr2pk/4pq1p/p1p1Rp2/P1B2P2/1PP3Q1/3r1BPP/4R1K1 w - - 1 44',
      themes: ['middlegame', 'short', 'fork', 'advantage'],
      glicko: {
        r: 1540.7253919742554,
        d: 75.23754491869116,
        v: 0.09003857959955437,
      },
      plays: 241,
      vote: 0.9266055226325989,
      line: 'f2c5 d2g2 g3g2 b7g2',
      generator: 14,
      cp: 468,
      vd: 8,
      vu: 210,
      users: ['LimeZebra', 'HumorousAntelope'],
    },
  },
  { upsert: true }
);

puzzleColl.updateOne(
  { _id: '02yo7' },
  {
    $set: {
      gameId: 'rVK1n3ZW',
      fen: 'r1b2rk1/ppp2pp1/2np2qp/1BbNp3/4P3/3P1N1P/PPP2PP1/R2QK2R b KQ - 3 10',
      themes: ['opening', 'short', 'fork', 'crushing'],
      glicko: {
        r: 1116.0797837293023,
        d: 104.13245164404374,
        v: 0.08999051112782643,
      },
      plays: 32,
      vote: 0.6666666865348816,
      line: 'c6b4 d5e7 g8h7 e7g6',
      generator: 14,
      cp: 717,
      vd: 1,
      vu: 5,
      users: ['HumorousAntelope', 'LimeZebra'],
    },
  },
  { upsert: true }
);

// Regen puzzle paths

const pathColl = db.puzzle2_path;
const verbose = false;
const maxRatingBuckets = 12;
const maxPathLength = 500;
const maxPuzzlesPerTheme = 2 * 1000 * 1000; // reduce to 500000 to avoid memory restrictions in some envs (!?)

const generation = Date.now();

const tiers = [
  ['top', 20 / 100],
  ['good', 50 / 100],
  ['all', 95 / 100],
];

const mixBoundaries = [
  100,
  800,
  900,
  1000,
  1100,
  1200,
  1270,
  1340,
  1410,
  1480,
  1550,
  1620,
  1690,
  1760,
  1830,
  1900,
  2000,
  2100,
  2200,
  2350,
  2500,
  2650,
  2800,
  9999,
];

const themes = db.puzzle2_puzzle.distinct('themes', {});
// const themes = ['overloading'];

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
  // ['mix'].forEach(theme => {
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

  const bucketBase = {
    groupBy: '$glicko.r',
    output: { puzzle: { $push: { id: '$_id', vote: '$vote' } } },
  };

  const nbPuzzles = puzzleColl.count(selector);

  if (!nbPuzzles) return [];

  const themeMaxPathLength = Math.max(10, Math.min(maxPathLength, Math.round(nbPuzzles / 200)));
  const nbRatingBuckets =
    theme == 'mix'
      ? mixBoundaries.length - 1
      : Math.max(3, Math.min(maxRatingBuckets, Math.round(nbPuzzles / themeMaxPathLength / 20)));

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
      const positionInTier = bucketIndex % nbRatingBuckets;
      const isFirstOfTier = positionInTier == 0;
      const isLastOfTier = positionInTier == nbRatingBuckets - 1;
      const pathLength = Math.max(10, Math.min(maxPathLength, Math.round(bucket.puzzles.length / 30)));
      const ratingMin = isFirstOfTier ? 100 : Math.ceil(bucket._id.min);
      const ratingMax = isLastOfTier
        ? 9999
        : theme == 'mix'
        ? mixBoundaries[positionInTier + 1]
        : Math.floor(bucket._id.max);
      const nbPaths = Math.max(1, Math.floor(bucket.puzzles.length / pathLength));
      const paths = chunkify(bucket.puzzles, nbPaths);
      if (verbose)
        print(
          `  ${theme} ${positionInTier} ${bucket.tier} ${ratingMin}->${ratingMax} puzzles: ${bucket.puzzles.length} pathLength: ${pathLength} paths: ${paths.length}`
        );

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

print('Done!');
