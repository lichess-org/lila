var m = require('mithril');
var lessons = require('../lesson/list');
var makeLesson = require('../lesson');

module.exports = function(lesson, opts) {

  try {
    var lesson = makeLesson(lessons.get(m.route.param("id")), {
      stage: m.route.param('stage') || 1
    });
  } catch (e) {
    console.log('No such lesson!');
    return m.route('/');
  }

  var getNext = function() {
    return lessons.get(lesson.blueprint.id + 1);
  };

  return {
    lesson: function() {
      return lesson;
    },
    getNext: getNext
  };
};
