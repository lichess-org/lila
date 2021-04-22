let users = db.user4;

// Add some users (their passwords are "password")
users.updateOne({ "_id" : "limezebra"},
{
  "$set":{
     "username":"LimeZebra",
     "email":"limezebra@gmail.com",
     "bpass":BinData(0,"n1G7smu1Z6DC1GszOpLba+POwqXfToPW8KMVJWxzECou5EnPYotZ"),
     "perfs":{

     },
     "count":{
        "ai":0,
        "draw":0,
        "drawH":0,
        "game":0,
        "loss":0,
        "lossH":0,
        "rated":0,
        "win":0,
        "winH":0
     },
     "enabled":true,
     "createdAt":ISODate("2021-04-22T19:48:34.144Z"),
     "seenAt":ISODate("2021-04-22T19:48:34.858Z"),
     "time":{
        "total":0,
        "tv":0
     },
     "verbatimEmail":"limeZebra@gmail.com",
     "len":9,
     "lang":"en-US"
  }
}
, {upsert: true});

users.updateOne({ "_id" : "humorousantelope"},
{
  "$set":{
     "username":"HumorousAntelope",
     "email":"humorousantelope@gmail.com",
     "bpass":BinData(0,"n1G7smu1Z6DC1GszOpLba+POwqXfToPW8KMVJWxzECou5EnPYotZ"),
     "perfs":{

     },
     "count":{
        "ai":0,
        "draw":0,
        "drawH":0,
        "game":0,
        "loss":0,
        "lossH":0,
        "rated":0,
        "win":0,
        "winH":0
     },
     "enabled":true,
     "createdAt":ISODate("2021-04-22T19:48:34.144Z"),
     "seenAt":ISODate("2021-04-22T19:48:34.858Z"),
     "time":{
        "total":0,
        "tv":0
     },
     "verbatimEmail":"HumorousAntelope@gmail.com",
     "len":9,
     "lang":"en-US"
  }
}
, {upsert: true});

print('Done!');
