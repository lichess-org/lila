var makeStage = require('./stage');

module.exports = function(lesson) {

  var stageId = 0;
  var stage = makeStage(lesson.stages[stageId]);

  return {
    stage: stage
  }
};
