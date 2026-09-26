const tcs = ['standard', 'rapid', 'blitz'];

// [ '2025-12', 1828 ] -> 2025121828
const encodePointExpr = pointExpr => ({
  $add: [
    {
      $multiply: [
        {
          $toInt: {
            $replaceAll: {
              input: { $arrayElemAt: [pointExpr, 0] },
              find: '-',
              replacement: '',
            },
          },
        },
        10000,
      ],
    },
    { $arrayElemAt: [pointExpr, 1] },
  ],
});

const setStage = tcs.reduce((acc, tc) => {
  acc[tc] = {
    $cond: [
      { $isArray: `$${tc}` },
      {
        $map: {
          input: `$${tc}`,
          as: 'point',
          in: {
            $cond: [{ $isArray: '$$point' }, encodePointExpr('$$point'), '$$point'],
          },
        },
      },
      `$${tc}`,
    ],
  };
  return acc;
}, {});

db.fide_player_rating.updateMany({ $or: tcs.map(tc => ({ [tc]: { $elemMatch: { $type: 'array' } } })) }, [
  { $set: setStage },
]);
