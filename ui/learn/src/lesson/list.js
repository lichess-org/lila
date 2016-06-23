module.exports = (function(lessons) {
  return {
    list: lessons,
    get: function(id) {
      return lessons.filter(function(l) {
        return l.id === id;
      })[0];
    }
  };
})([
  require('./rook/lesson'),
  require('./bishop/lesson')
]);
