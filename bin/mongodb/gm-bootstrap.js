var userIds = ['GeorgMeier'].map(u => u.toLowerCase());
var perfType = 'bullet';
var perf = {
  "gl": {
    "r": 2500,
    "d": 150,
    "v": 0.06
  },
  "nb": NumberInt(0),
  "re": [ ]
};

userIds.forEach(id => {
  var user = db.user4.findOne({_id:id});
  if (user && (!user.perfs[perfType] || !user.perfs[perfType].nb))
    db.user4.update({_id:id},{$set:{['perfs.' + perfType]: perf}});
  db.tournament_player.update(
    {uid:id,r:1500,m:1500},
    {$set:{r:NumberInt(perf.gl.r),m:NumberInt(perf.gl.r)}},
    {multi:1});
});
