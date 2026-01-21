const pipeline = [{
  "$match": {
    "a": true,
    "m": {
      "$elemMatch": {
        "w": { "$lt": 33300 },
        "i": { "$lt": -1 }
      }
    },
    "p": 3,
    "mr": {
      "$gte": 1605, "$lte": 1627
    }
  }
}, {
  "$limit": 3333
}, {
  "$group": {
    "_id": "$p",
    "loss": {
      "$sum": {
        "$cond": [
          { "$eq": ["$r", 3] },
          1,
          0
        ]
      }
    },
    "nb": { "$sum": 1 }
  }
}];

const q = {
  "a": true,
  "p": 3,
  "mr": {
    "$gte": 1605, "$lte": 1627
  }
};

// db.insight.aggregate(pipeline);
