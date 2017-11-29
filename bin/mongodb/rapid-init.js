
function copyPerf(p) {
  if (!p || !p.nb) return;
  return {
    gl: {
      r: p.gl.r,
      v: p.gl.v,
      d: Math.min(350, p.gl.d + 80)
    },
    nb: NumberInt(0),
    re: []
  };
}

// var query = {_id:{$in:['thibault', 'neio']}};
var query = {
  'perfs.classical.nb':{$gte:1},
  'perfs.rapid': { $exists: false }
};

db.user4.find(query).limit(1000).forEach(u => {
  var rapid = copyPerf(u.perfs.classical);
  if (rapid) db.user4.update({_id:u._id}, {$set: {'perfs.rapid':rapid}});
});
