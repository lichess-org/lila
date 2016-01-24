var limit = 50 * 1000;
var coll = db.relation;

function timer(name, f) {
  print('Start ' + name);
  var start = new Date().getTime();
  if (f() !== true) print('FAILS');
  else {
    for (var i = 0; i < limit; i++) f();
    print(name + ': ' + (new Date().getTime() - start));
  }
}

timer('count', function() {
  return coll.count({
    _id: 'thibault/legendarybcloud'
  }) === 1;
});
timer('find', function() {
  return coll.find({
    _id: 'thibault/legendarybcloud'
  }).limit(1).length() === 1;
});
timer('findOne', function() {
  return coll.findOne({
    _id: 'thibault/legendarybcloud'
  }) !== null;
});
