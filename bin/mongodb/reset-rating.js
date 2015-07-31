var user = 'chessbrahs';
var perf = 'bullet';
var rating = 2527;

var set = {};
set ['perfs.' + perf + '.gl.r'] = rating;

var push = {};
push['perfs.' + perf + '.re'] = {
  $each: [NumberInt(rating)],
  $position: 0
};

db.user4.update({
  _id: user
}, {
  $set: set,
  $push: push
});
