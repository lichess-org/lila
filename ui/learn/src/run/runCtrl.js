var m = require('mithril');
var lessons = require('../lesson/list');
var makeLesson = require('../lesson');
var xhr = require('../xhr');

module.exports = function(opts) {

  var setScore = function(level, score) {
    xhr.setScore(level.key, score).then(function(data) {
      opts.data = data;
    });
  };

  var lesson = makeLesson(lessons.get(m.route.param("id")), {
    stage: m.route.param('stage') || 1,
    setScore: setScore
  });

  opts.route = 'run';
  opts.lessonId = lesson.blueprint.id;

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
