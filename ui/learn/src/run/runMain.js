var m = require('mithril');
var runCtrl = require('./runCtrl');
var runView = require('./runView');

module.exports = function(opts) {
  return {
    controller: function() {
      return runCtrl(opts);
    },
    view: runView
  };
};
