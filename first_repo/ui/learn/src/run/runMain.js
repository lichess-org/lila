var runCtrl = require('./runCtrl');
var runView = require('./runView');

module.exports = function (opts, trans) {
  return {
    controller: function () {
      return runCtrl(opts, trans);
    },
    view: runView,
  };
};
