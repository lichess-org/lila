var all = [{
  id: 'pieces.rook',
  num: 1,
  title: 'The mighty rook'
}];

module.exports = {
  all: all,
  byId: function(id) {
    return all.find(function(s) {
      return s.id === id;
    });
  }
};
