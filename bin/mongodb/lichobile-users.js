function gatherRecentUsers() {
  db.user4.aggregate([
    {
      $match: {
        enabled: true,
        seenAt: { $gt: new Date(Date.now() - 1000 * 60 * 60 * 24 * 60) },
      },
    },
    { $project: { lang: 1 } },
    { $out: 'lm_user_recent' },
  ]);
  print('Done!');
  print(db.lm_user_recent.estimatedDocumentCount());
}

function filterLichobileUsers() {
  db.lm_user_recent.aggregate([
    {
      $lookup: {
        from: 'security',
        as: 'sec',
        localField: '_id',
        foreignField: 'user',
        pipeline: [
          {
            $match: {
              api: { $exists: 1 },
            },
          },
          { $limit: 1 },
        ],
      },
    },
    { $match: { sec: { $ne: [] } } },
    {
      $lookup: {
        from: 'oauth2_access_token',
        as: 'tokens',
        localField: '_id',
        foreignField: 'userId',
        pipeline: [{ $match: { scopes: 'web:mobile' } }, { $limit: 1 }],
      },
    },
    { $match: { tokens: [] } },
    { $project: { lang: 1 } },
    { $out: 'lm_user_recent_nomobile' },
  ]);
  print('Done!');
  print(db.lm_user_recent_nomobile.estimatedDocumentCount());
}

gatherRecentUsers();
filterLichobileUsers();
