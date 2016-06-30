var m = require('mithril');
var stages = require('../stage/list');
var makeStage = require('../stage');
var xhr = require('../xhr');

module.exports = function(opts) {

  var setScore = function(stage, score) {
    xhr.setScore(stage.key, score).then(function(data) {
      opts.data = data;
    });
  };

  var stage = makeStage(stages.get(m.route.param("id")), {
    level: m.route.param('level') || 1,
    setScore: setScore
  });

  opts.route = 'run';
  opts.stageId = stage.blueprint.id;

  var getNext = function() {
    return stages.get(stage.blueprint.id + 1);
  };

  return {
    stage: function() {
      return stage;
    },
    getNext: getNext
  };
};
