const colls = ['tournament2'];

colls.forEach(function (coll) {
  indexes = db[coll].getIndexes();
  if (indexes && indexes.length > 0) {
    print('//Indexes for ' + coll + ':');
  }
  indexes.forEach(function (index) {
    var options = {};
    if (index.unique) {
      options.unique = index.unique;
    }
    if (index.sparse) {
      options.sparse = index.sparse;
    }
    if (index.partialFilterExpression) {
      options.partialFilterExpression = index.partialFilterExpression;
    }
    // options.background = true;
    options = JSON.stringify(options);
    var key = JSON.stringify(index.key);
    if (key !== '{"_id":1}') {
      print('db.' + coll + '.createIndex(' + key + ', ' + options + ');');
    }
  });
});
