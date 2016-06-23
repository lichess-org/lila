var m = require('mithril');
var stages = require('../stages');

module.exports = function(opts) {
  return {
    controller: function() {
      var stage = stages.byId(m.route.param("id"));
      return {
        stage: stage
      };
    },
    view: function(ctrl) {
      var stage = ctrl.stage;
      return m('div.learn.stage', 'Learn stage ', stage.title);
    }
  };
}
