var gamesToMigrate = db.game5.find({
  $or: [{
      'p0.w': true
    }, {
      'p1.w': true
    }
  ]
}, {
  'p0.w': true
});

gamesToMigrate.forEach(function(g) {
  var color = !! (g.p0 && g.p0.w);
  var update = {
    $unset: color ? {
      'p0.w': true
    } : {
      'p1.w': true
    },
    $set: {
      w: color
    }
  };
  // printjson(g);
  // printjson(update);
  // print(' ');
  db.game5.update({
    _id: g._id
  }, update);
});

print("Done!");
