var limit = 50 * 1000;
var coll = db.relation;
var query = {
  _id: 'thibault/legendary22bcloud'
};
var expected = false;

function timer(name, f) {
  print('Start ' + name);
  var start = new Date().getTime();
  if (f() !== expected) print('FAILS');
  else {
    for (var i = 0; i < limit; i++) f();
    print(name + ': ' + (new Date().getTime() - start));
  }
}

timer('count', function() {
  return coll.count(query) === 1;
});
timer('find', function() {
  return coll.find(query).limit(1).length() === 1;
});
timer('findOne', function() {
  return coll.findOne(query) !== null;
});
