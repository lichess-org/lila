var m = require('mithril');
var lessons = require('../lesson/list');
var makeLesson = require('../lesson');

module.exports = function(lesson, opts) {

  var lesson = makeLesson(lessons.get(m.route.param("id")), {
    stage: m.route.param('stage') || 1
  });

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
