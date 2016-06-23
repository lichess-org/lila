var m = require('mithril');
var stageCtrl = require('./stageCtrl');
var stageView = require('./stageView');

module.exports = function(opts) {
  return {
    controller: function() {
      return stageCtrl(opts);
    },
    view: stageView
  };
};
