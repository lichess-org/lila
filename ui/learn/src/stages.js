var all = [{
  id: 'pieces.rook',
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
