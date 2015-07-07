var it = 0;
var batchSize = 100;

db.user4.find({
  createdAt: {
    $gt: ISODate("2015-02-03T20:00:09.148Z")
  }
}).forEach(function(u) {

  db.user4.update({
    _id: u._id
  }, {
    $unset: {
      createdAt: true
    }
  });

  db.user4.update({
    _id: u._id
  }, {
    $set: {
      createdAt: u.createdAt
    }
  })

  ++it;
  if (it % batchSize == 0) {
    print(it);
    sleep(100);
  }
});

print("Done!");
