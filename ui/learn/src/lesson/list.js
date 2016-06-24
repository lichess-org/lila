var util = require('../util');

var lessons = [
  require('./rook'),
  require('./bishop'),
  require('./queen')
].map(util.toLesson);

module.exports = {
  list: lessons,
  get: function(id) {
    return lessons.filter(function(l) {
      return l.id == id;
    })[0];
  }
};
