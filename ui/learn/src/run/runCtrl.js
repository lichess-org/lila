var m = require('mithril');
var lessons = require('../lesson/list');
var makeLesson = require('../lesson');

module.exports = function(lesson, opts) {

  var lessonData = lessons.get(m.route.param("id"));
  if (!lessonData) return m.route('/');
  var lesson = makeLesson(lessonData);

  return {
    lesson: lesson
  };
};
