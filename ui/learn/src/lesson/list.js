var lessons = [
  require('./rook/lesson'),
  require('./bishop/lesson')
];

module.exports = {
  list: lessons,
  get: function(id) {
    return lessons.filter(function(l) {
      return l.id === id;
    })[0];
  }
};
