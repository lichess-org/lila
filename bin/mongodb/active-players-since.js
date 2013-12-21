var result = db.game5.aggregate({
  $match: {
    ca: {
      $gt: ISODate("2013-02-14T14:06:03Z")
    },
    s: {
      $gte: 30
    },
    "us.0": {
      $exists: true
    }
  }
}, {
  $unwind: "$us"
}, {
  $match: {
    us: {
      $ne: ""
    }
  }
}, {
  $group: {
    _id: "$us",
    number: {
      $sum: 1
    }
  }
}, {
  $sort: {
    number: -1
  }
}, {
  $limit: 10
});

printjson(result);
