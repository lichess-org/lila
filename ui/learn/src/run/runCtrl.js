var m = require('mithril');
var lessons = require('../lesson/list');
var makeLesson = require('../lesson');

module.exports = function(lesson, opts) {

  var onStageComplete = function() {
    if (lesson.next()) true;
    else alert('lesson complete');
    m.redraw.strategy('all');
    m.redraw();
  };

  try {
    var lesson = makeLesson(lessons.get(m.route.param("id")), {
      onStageComplete: onStageComplete
    });
  } catch (e) {
    console.log('No such lesson!');
    return m.route('/');
  }

  return {
    lesson: function() {
      return lesson;
    }
  };
};
